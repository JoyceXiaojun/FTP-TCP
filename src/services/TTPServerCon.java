package services;

import datatypes.Datagram;
import datatypes.PayLoad;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ShuqinYe on 20/3/16.
 */
public class TTPServerCon {
    /**
     * The first datagram sequence number that has not received an ACK.
     */
    private int base;

    /**
     * The datagram sequence number of the next packet to be sent.
     * This value starts from 1.
     */
    private int nextSeqNum;

    /**
     * Timer to track segments.
     */
    private Timer timer;

    /**
     * The datagram that's passed from the main server.
     */
    private Datagram datagram;
    private PayLoad payLoad;
    private byte[] actualData;

    /**
     * The TTP Service this connection will use to send and receive data.
     */
    private TTPService ttpService;
    private short maxLoadSize;

    /**
     * The actual byte array to send to the client.
     */
    private byte[] totalDataToSend;
    private int fileSize;
    private byte[] MD5 = new byte[16];
    private int maxSeq;

    public TTPServerCon(int base, short nextSeqNum, TTPService ttpService, Datagram datagram) {
        this.base = base;
        this.nextSeqNum = nextSeqNum;
        this.ttpService = ttpService;
        this.datagram = datagram;
        try {
            maxLoadSize = GetSizeService.getMaxLoadSize();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    /**
     * Generate a datagram to be sent to the client.
     * @return the datagram to be sent.
     */
    public synchronized void sendFile(byte[] totalDataToSend, byte[] MD5) throws IOException, InterruptedException {

        this.totalDataToSend = totalDataToSend;
        fileSize = totalDataToSend.length;
        this.MD5 = MD5;
        this.maxSeq = fileSize % maxLoadSize == 0? fileSize / maxLoadSize : fileSize / maxLoadSize + 1;

        while (true) {
            if (ttpService == null) break;
            if (nextSeqNum <= maxSeq && nextSeqNum < base + ttpService.getWindowSize()) {

                ttpService.sendDatagram(makeDatagram(nextSeqNum));

                if (base == nextSeqNum) {
                    if (ttpService == null || base == maxSeq) {
                        stopTimer();
                        break;
                    }
                    startTimer();
                    System.out.println("TTPServerCon: timer started when base == nextseqnum.");
                }
                nextSeqNum++;

            } else {
                if (ttpService == null) {
                    stopTimer();
                    break;
                }
                if (nextSeqNum == maxSeq + 1) {
                    // All the segments have been transmitted, just wait for the timer to take effect.
                    continue;
                }


                System.out.println("TTPServerCon: Window size is full and please try again later.");
            }

            wait(1000);
        }
    }

    /**
     *  Hanlde the received datagram passed to this connection.
     *  The datagram is not corrupt - no need to check it again.
     * @param receivedDg
     * @throws IOException
     */
    public synchronized void receiveAck(Datagram receivedDg) throws IOException {
        // Received datagram.
        this.datagram = receivedDg;
        payLoad = (PayLoad) datagram.getData();
        actualData = payLoad.getActualData();

        if (payLoad.getACK()) {

        } else if (payLoad.getSeqNum() == maxSeq){
            timer.cancel();
        } else if (payLoad.getAck() == (short) 1) {
            // When the actualData == null.
            base = payLoad.getSeqNum() + 1;
            if (base == nextSeqNum) {
                if (ttpService == null) {
                    return;
                }
                stopTimer();
                System.out.println("TTPServerCon: timer stopped.");
            }

            else {
                if (ttpService == null) {
                    return;
                }
                startTimer();
                System.out.println("TTPServerCon: timer started when base increases.");
            }
        }
    }

    private Datagram makeDatagram(int nextSeqNum) {
        Datagram dataToSend = new Datagram();
        dataToSend.setSrcaddr(datagram.getDstaddr());
        dataToSend.setSrcport(datagram.getDstport());
        dataToSend.setDstaddr(datagram.getSrcaddr());
        dataToSend.setDstport(datagram.getSrcport());
        PayLoad data = new PayLoad();
        dataToSend.setData(data);

        if (fileSize < maxLoadSize && nextSeqNum == 1) {
            data.setLAST(true);
            data.setSeqNum(nextSeqNum);
            data.setWindowSize(ttpService.getWindowSize());
            data.setRetransTimeInterval(ttpService.getRetransTimeInterval());
            data.setActualData(totalDataToSend);
            data.setMD5(MD5);

            dataToSend.setSize((short) data.getActualData().length);
            System.out.println("TTPServerCon: A datagram has been made when the file size < maxloadsize");

        } else if (fileSize % maxLoadSize == 0 && nextSeqNum == fileSize / maxLoadSize) {

            byte[] thisSegment = new byte[maxLoadSize];
            System.arraycopy(totalDataToSend, (fileSize - maxLoadSize), thisSegment, 0, maxLoadSize);

            data.setLAST(true);
            data.setSeqNum(nextSeqNum);
            data.setWindowSize(ttpService.getWindowSize());
            data.setRetransTimeInterval(ttpService.getRetransTimeInterval());
            data.setActualData(thisSegment);
            data.setMD5(MD5);

            dataToSend.setSize((short) data.getActualData().length);
            System.out.println("TTPServerCon: A datagram has been made for last segment when fileSize % maxLoadSize == 0");

        } else if (fileSize % maxLoadSize != 0 && nextSeqNum == fileSize / maxLoadSize + 1) {
            byte[] thisSegment = new byte[fileSize - (fileSize / maxLoadSize) * maxLoadSize];
            System.arraycopy(totalDataToSend, (nextSeqNum - 1) * maxLoadSize, thisSegment, 0, thisSegment.length);

            data.setLAST(true);
            data.setSeqNum(nextSeqNum);
            data.setWindowSize(ttpService.getWindowSize());
            data.setRetransTimeInterval(ttpService.getRetransTimeInterval());
            data.setActualData(thisSegment);
            data.setMD5(MD5);

            System.out.println("TTPServerCon: A datagram has been made for last segment when fileSize % maxLoadSize != 0");

        } else {
            byte[] thisSegment = new byte[maxLoadSize];

            System.arraycopy(totalDataToSend, (nextSeqNum - 1) * maxLoadSize, thisSegment, 0, thisSegment.length);

            data.setSeqNum(nextSeqNum);
            data.setWindowSize(ttpService.getWindowSize());
            data.setRetransTimeInterval(ttpService.getRetransTimeInterval());
            data.setActualData(thisSegment);
            System.out.println("TTPServerCon: A datagram has been made for intermediate segement");
        }

        // Calculate checksum for the data to send to client.
        short checkSum = CheckSumService.generateCheckSum(dataToSend);
        dataToSend.setChecksum(checkSum);

        System.out.println("Checksum generated!");

        return dataToSend;
    }

    /**
     * Start timer.
     */
    private void startTimer() {
        timer = new Timer();
        timer.schedule(new Retransmission(), ttpService.getRetransTimeInterval(), ttpService.getRetransTimeInterval());
    }

    /**
     * Stop timer.
     */
    private void stopTimer() {
        timer.cancel();
    }


    private class Retransmission extends TimerTask {
        public void run() {
            for (int i = base; i < nextSeqNum; ++i) {
                try {
                    ttpService.sendDatagram(makeDatagram(i));
                } catch (IOException e) {
                    System.err.println("There is error trying to retransmit segments");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Close this TTP connection.
     */
    public void closeConnetion() {
        base = 0;
        nextSeqNum = 0;
        if (timer != null) {
            timer.cancel();
        }
        ttpService = null;
    }


}
