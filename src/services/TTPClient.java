/*
 * A sample client that uses DatagramService
 */

package services;

import applications.FTPClient;
import datatypes.Datagram;
import datatypes.PayLoad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class TTPClient {

    private TTPService ttpService;
    private int expectedSeqNum = 1;
    private String fileName;
    private Timer SYNTimer;
    private Timer requestTimer;
    private Timer FINTimer;


    /**
     * Loop back address is the server in our case.
     */
    private static final String SRCADDR = "127.0.0.1";
    private static final String DSTADDR = "127.0.0.1";

    /**
     * srcport is local port.
     * dstport is server port.
     */
    private short srcport;
    private short dstport;

    /**
     * Initialize the send datagram for expectedSeqNum == 0.
     */
    private Datagram ackPkt;

    private ArrayList<byte[]> completeFile = new ArrayList<>();
    private byte[] file;
    private boolean completeReceive;


    public TTPClient(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void openConnection(short localPort, short serverPort, short windowSize, short reTransTimeInterval)
            throws IOException, ClassNotFoundException {

        // First argument is source port, second argument is destination port.
        srcport = localPort;
        dstport = serverPort;

        // Establish a client service.
        ttpService = new TTPService(srcport, 10);
        ttpService.intializeService(windowSize, reTransTimeInterval);
        System.out.println("TTPClient: windowsize = " + windowSize);
        System.out.println("TTPClient: retransmissiontime = " + reTransTimeInterval + " milliseconds");
        System.out.println("TTPClient: ttpService initialized");

        ackPkt = makeAck((short) 0);

        ttpService.sendDatagram(makeSYN());
        startSYNTimer();
        System.out.println("TTPClient: Sent SYN to establish connection.");

        Datagram receivedDg = null;

        // This statement blocks until a datagram is received.
        do {
            receivedDg = ttpService.receiveDatagram();
        } while (CheckSumService.isCorrupt(receivedDg));

        System.out.println("TTPClient: received a datagram from server:");
        System.out.println("   - ACK = " + ((PayLoad) (receivedDg.getData())).getACK() + " ");
        System.out.println("   - seqNum = " + ((PayLoad) (receivedDg.getData())).getSeqNum() + " ");
        System.out.println("   - FIN = " + ((PayLoad) (receivedDg.getData())).getFIN() + " ");
        System.out.println("   - SYN = " + ((PayLoad) (receivedDg.getData())).getSYN() + " ");
        System.out.println("   - LAST = " + ((PayLoad) (receivedDg.getData())).getLAST() + " ");

        if (((PayLoad) receivedDg.getData()).getACK()) {
            stopSYNTimer();
            ttpService.sendDatagram(makeACK());
            System.out.println("TTPClient: Sent ACK to server at port ");

            ttpService.openConnection();
            System.out.println("TTPClient: Connection established with server at port " + dstport);

            ttpService.sendDatagram(makeRequest());
            startRequestTimer();
            System.out.println("TTPClient: Sent file request to server at port " + dstport);

        }

        // When the connection is open, keep listening for segments from the server.
        while (ttpService.isConnected()) {
            Datagram datagram = ttpService.receiveDatagram();

            System.out.println("TTPClient: One datagram is received from the server: ");
            System.out.println("   - ACK = " + ((PayLoad) (datagram.getData())).getACK() + " ");
            System.out.println("   - ack = " + ((PayLoad) (datagram.getData())).getAck() + " ");
            System.out.println("   - seqNum = " + ((PayLoad) (datagram.getData())).getSeqNum() + " ");
            System.out.println("   - FIN = " + ((PayLoad) (datagram.getData())).getFIN() + " ");
            System.out.println("   - LAST = " + ((PayLoad) (datagram.getData())).getLAST() + "\n");

            handleDatagram(datagram);

        }
    }


    /**
     * Handle the incoming datagram.
     *
     * @param received
     */
    private void handleDatagram(Datagram received) throws IOException {
        if (CheckSumService.isCorrupt(received)) {
            System.out.println("TTPClient: This datagram received from server is corrupt.");
            return;
        }

        PayLoad payLoad = (PayLoad) received.getData();

//        if (!completeReceive) {

        if (payLoad.getFIN()) {
            if (FINTimer != null) {
                stopFINTimer();
            }
            if (payLoad.getERR()) {
                // Print out the error message from the server.
                System.out.println("Error message: " + new String(payLoad.getActualData()));
            }
            // If the server sends the FIN signal.
            System.out.println("TTPClient: A FIN signal has been received from the server to close the connection.");
            ttpService.closeConnection();
            System.out.println("TTPClient: Sever connection closed!");

        } else if (payLoad.getACK()) {
//                stopSYNTimer();
        } else if (payLoad.getSeqNum() == expectedSeqNum) {
            if (expectedSeqNum == 1) {
                stopRequestTimer();
            }

            byte[] data = ((PayLoad) received.getData()).getActualData();
            completeFile.add(data);

            System.out.println("TTPClient: Received one segment of the data for expectedSegNum = " + expectedSeqNum);
            ackPkt = makeAck(expectedSeqNum);

            ttpService.sendDatagram(ackPkt);
            System.out.println("TTPClient: Acknowledgement for seqnum sent to sever. Seqnum = " + expectedSeqNum);

            // Increment the expected sequence number.
            expectedSeqNum++;
            System.out.println("TTPClient: Increased expected seqnum");

        } else {
            ttpService.sendDatagram(ackPkt);
            System.out.println("TTPClient: Acknowlsegemnt sent to server for expectedseqnum = " + (expectedSeqNum - 1));
        }

        if (payLoad.getLAST()) {
            byte[] data = ((PayLoad) received.getData()).getActualData();

            System.out.println("TTPClient: Last segment from the server received.");
            file = reassemble();
            byte[] md5 = ((PayLoad) received.getData()).getMD5();
            System.out.println("TTPClient: MD5 received from the server");

            FTPClient.sendFile(file, md5);
            System.out.println("TTPClient: Complete file and MD5 sent to the ftpclient.");

            ttpService.sendDatagram(makeFIN());
            startFINTimer();
            System.out.println("TTPClient: FIN singal sent to the server to close connection");
            completeReceive = true;
        }
//        }

        // All the packets are received by the client already.
        if (payLoad.getFIN()) {
            if (payLoad.getERR()) {
                // Print out the error message from the server.
                System.err.println("Error message: " + new String(payLoad.getActualData()));
            }

            if (FINTimer != null) {
                stopFINTimer();
            }
            // If the server sends the FIN signal.
            System.out.println("TTPClient: A FIN signal has been received from the server to close the connection.");
            ttpService.closeConnection();
            System.out.println("TTPClient: Sever connection closed!");
        }
        return;


    }

    /**
     * Close the connection.
     */
    public void closeConnection() {
        ttpService = null;
        fileName = null;
    }


    /**
     * Fill source and destination addressed in the datagram.
     *
     * @return the datagram that contains the address.
     */
    private Datagram fillAddress() {
        Datagram datagram = new Datagram();
        datagram.setSrcaddr(SRCADDR);
        datagram.setDstaddr(DSTADDR);

        datagram.setDstport(dstport);
        datagram.setSrcport(srcport);
        return datagram;
    }

    /**
     * Create a SYN datagram to request connection with a server.
     *
     * @return the SYN datagram.
     */
    private Datagram makeSYN() {
        Datagram datagram = fillAddress();

        PayLoad payLoad = new PayLoad();
        payLoad.setSYN(true);
        payLoad.setWindowSize(ttpService.getWindowSize());
        payLoad.setRetransTimeInterval(ttpService.getRetransTimeInterval());
        datagram.setData(payLoad);

        short checkSum = CheckSumService.generateCheckSum(datagram);
        datagram.setChecksum(checkSum);

        return datagram;

    }

    /**
     * Create an ACK to establish connection.
     *
     * @return
     */
    private Datagram makeACK() {
        Datagram datagram = fillAddress();

        PayLoad payLoad = new PayLoad();
        payLoad.setACK(true);
        payLoad.setWindowSize(ttpService.getWindowSize());
        payLoad.setRetransTimeInterval(ttpService.getRetransTimeInterval());
        datagram.setData(payLoad);

        short checkSum = CheckSumService.generateCheckSum(datagram);
        datagram.setChecksum(checkSum);

        return datagram;
    }

    /**
     * Create a FIN to request to close a connection.
     *
     * @return
     */
    private Datagram makeFIN() {
        Datagram datagram = fillAddress();

        PayLoad payLoad = new PayLoad();
        payLoad.setFIN(true);

        payLoad.setWindowSize(ttpService.getWindowSize());
        payLoad.setRetransTimeInterval(ttpService.getRetransTimeInterval());
        datagram.setData(payLoad);

        short checkSum = CheckSumService.generateCheckSum(datagram);
        datagram.setChecksum(checkSum);

        return datagram;
    }

    /**
     * Create a request to send to the server for files.
     *
     * @return
     */
    private Datagram makeRequest() {
        Datagram datagram = fillAddress();

        PayLoad payLoad = new PayLoad();
        payLoad.setActualData(fileName.getBytes());
        payLoad.setWindowSize(ttpService.getWindowSize());
        payLoad.setRetransTimeInterval(ttpService.getRetransTimeInterval());
        datagram.setData(payLoad);

        datagram.setSize(GetSizeService.getSize(payLoad.getActualData()));

        short checkSum = CheckSumService.generateCheckSum(datagram);
        datagram.setChecksum(checkSum);

        return datagram;
    }

    /**
     * Create an acknowledgement telling server all datagrams are received up until this sequence number.
     * (The current sequence number included.)
     *
     * @param expectedSeqNum
     * @return
     */
    private Datagram makeAck(int expectedSeqNum) {
        Datagram sendPkt = fillAddress();
        PayLoad sendPktPl = new PayLoad();
        sendPktPl.setSeqNum(expectedSeqNum);
        sendPktPl.setAck((short) 1);
        sendPktPl.setWindowSize(ttpService.getWindowSize());
        sendPktPl.setRetransTimeInterval(ttpService.getRetransTimeInterval());
        sendPkt.setData(sendPktPl);

        short checkSum = CheckSumService.generateCheckSum(sendPkt);
        sendPkt.setChecksum(checkSum);

        return sendPkt;
    }


    /**
     * Reassemble multiple segments.
     *
     * @return
     */
    private byte[] reassemble() {
        int arraySize = 0;
        System.out.println("Complete file length = " + completeFile.size());

        for (byte[] curByteArr : completeFile) {
            arraySize += curByteArr.length;
        }

        byte[] file = new byte[arraySize];
        int i = 0;

        while (i < arraySize) {
            for (byte[] curByteArr : completeFile) {
                for (int k = 0; k < curByteArr.length; ++k) {
                    file[i] = curByteArr[k];
                    ++i;
                }
            }
        }

        return file;
    }


    /**
     * Start timer.
     */
    private void startSYNTimer() {
        SYNTimer = new Timer();
        SYNTimer.schedule(new RetransmissionSYN(), ttpService.getRetransTimeInterval(), ttpService.getRetransTimeInterval());
    }

    /**
     * Start timer.
     */
    private void startRequestTimer() {
        requestTimer = new Timer();
        requestTimer.schedule(new RetransmissionRequest(), ttpService.getRetransTimeInterval(), ttpService.getRetransTimeInterval());
    }

    private void startFINTimer() {
        FINTimer = new Timer();
        FINTimer.schedule(new RetransmissionFIN(), ttpService.getRetransTimeInterval(), ttpService.getRetransTimeInterval());
    }

    /**
     * Stop timer.
     */
    private void stopSYNTimer() {
        SYNTimer.cancel();
    }

    /**
     * Stop timer.
     */
    private void stopRequestTimer() {
        requestTimer.cancel();
    }

    private void stopFINTimer() {
        FINTimer.cancel();
    }


    private class RetransmissionSYN extends TimerTask {
        public void run() {
            try {
                ttpService.sendDatagram(makeSYN());
            } catch (IOException i) {
                i.printStackTrace();
            }
        }
    }


    private class RetransmissionRequest extends TimerTask {
        public void run() {
            try {
                ttpService.sendDatagram(makeRequest());
            } catch (IOException i) {
                i.printStackTrace();
            }
        }
    }

    private class RetransmissionFIN extends TimerTask {
        public void run() {
            try {
                if (ttpService != null) {
                    ttpService.sendDatagram(makeFIN());
                } else {
                    stopFINTimer();
                }
            } catch (IOException i) {
                i.printStackTrace();
            }
        }
    }
}