package services;

import datatypes.Datagram;
import datatypes.PayLoad;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A TTPServer that uses DatagramService.
 */
public class TTPServer {

    private TTPService ttpService;
    private Map<String, TTPServerCon> connections = new HashMap<>();
    private Datagram datagram = null;
    private String curKey;

    public TTPServer() {

    }

    public void startServer(short serverPort, short windowSize,
                            short reTransTimeInterval) throws IOException, ClassNotFoundException {

        ttpService = new TTPService(serverPort, 10);
        ttpService.intializeService(windowSize, reTransTimeInterval);
    }

    public byte[] accept() throws IOException, ClassNotFoundException {

        byte[] actualData = null;

        datagram = ttpService.receiveDatagram();
        System.out.println("TTPServer: received a datagram from client:");
        System.out.println("   - ACK = " + ((PayLoad) (datagram.getData())).getACK() + " ");
        System.out.println("   - ack = " + ((PayLoad) (datagram.getData())).getAck() + " ");
        System.out.println("   - seqNum = " + ((PayLoad) (datagram.getData())).getSeqNum() + " ");
        System.out.println("   - FIN = " + ((PayLoad)(datagram.getData())).getFIN() + " ");
        System.out.println("   - SYN = " + ((PayLoad)(datagram.getData())).getSYN() + " ");

        String srcAddr = datagram.getSrcaddr();
        short srcPort = datagram.getSrcport();

        // If the datagram is corrupt, drop it.
        if (CheckSumService.isCorrupt(datagram)) {
            System.out.println("TTPServer: this datagram received from client is corrupt.");
            return null;
        }

        curKey = srcAddr + srcPort;
        PayLoad payLoad = (PayLoad) (datagram.getData());

        // If the datagram is a SYN request and there is connection established, then open a connection.
        if (payLoad.getSYN()) {
            if (!connections.containsKey(curKey)) {
                ttpService.sendDatagram(makeACK(datagram));
                System.out.println("TTPServer: going to open connection with "
                        + datagram.getSrcaddr() + " at its port number " + datagram.getSrcport());
                System.out.println("TTPServer: sent ACK to client!");

                ttpService.openConnection(connections, curKey, datagram);
                System.out.println("TTPServer: Connection opened.");
            } else {
                ttpService.sendDatagram(makeACK(datagram));
                System.out.println("TTPServer: sent ACK to client!");
            }
        } else if (!connections.containsKey(curKey)) {
            // If there is no connection but the initial datagram is not a SYN, just drop the datagram.
            return null;
        } else if (payLoad.getFIN()) {
            System.out.println("TTPServer: FIN signal received from client.");
            ttpService.sendDatagram(makeFIN(datagram));
            System.out.println("TTPServer: FIN signal sent to client.");
            ttpService.closeConnection(connections, curKey);
            System.out.println("TTPServer: Connection with client closed");

        } else if ((actualData = payLoad.getActualData()) != null) {
            // There is connection and the client requests for a file.
            System.out.println("TTPServer: get the file name from client.");
            return actualData;
        } else {
            // Acknowledgement for a seqNum is received - need to deal with it.
            connections.get(curKey).receiveAck(datagram);
            System.out.println("TTPServer: go to the connection to deal with the ack for segments.");
        }

        System.out.println("TTPServer: no file name to send to ftp client.");
        return null;

    }


    /**
     * @param totalDataToSend
     * @throws IOException
     */
    public void sendFile(byte[] totalDataToSend, byte[] MD5) throws IOException, InterruptedException {
        connections.get(curKey).sendFile(totalDataToSend, MD5);
    }


    /**
     * If file is not found on the server.
     * A datagram with FIN = true and ERR = true are sent to the client.
     */
    public void sendErr() {
        Datagram errDatagram = makeFIN(datagram);
        PayLoad payLoad = (PayLoad) (errDatagram.getData());
        payLoad.setERR(true);
        payLoad.setActualData("File not found on server.".getBytes());
        errDatagram.setChecksum(CheckSumService.generateCheckSum(errDatagram));

        try {
            ttpService.sendDatagram(errDatagram);
        } catch (IOException i) {
            i.printStackTrace();
        }
        System.out.println("TTPServer: ERR datagram sent to client.");
        if (connections.containsKey(curKey)) {
            ttpService.closeConnection(connections, curKey);
        }
        System.out.println("TTPServer: Connection with client closed");
    }


    /**
     * Reply with an ACK.
     *
     * @param datagram
     * @return
     */
    private Datagram makeACK(Datagram datagram) {
        Datagram dataToSend = new Datagram();
        dataToSend.setSrcaddr(datagram.getDstaddr());
        dataToSend.setSrcport(datagram.getDstport());
        dataToSend.setDstaddr(datagram.getSrcaddr());
        dataToSend.setDstport(datagram.getSrcport());
        PayLoad data = new PayLoad();
        data.setACK(true);
        dataToSend.setData(data);

        // Calculate checksum for the data to send to client.
        short checkSum = CheckSumService.generateCheckSum(dataToSend);
        dataToSend.setChecksum(checkSum);

        return dataToSend;
    }

    /**
     * Reply with a FIN.
     *
     * @param datagram
     * @return
     */
    private Datagram makeFIN(Datagram datagram) {
        Datagram dataToSend = new Datagram();
        dataToSend.setSrcaddr(datagram.getDstaddr());
        dataToSend.setSrcport(datagram.getDstport());
        dataToSend.setDstaddr(datagram.getSrcaddr());
        dataToSend.setDstport(datagram.getSrcport());
        PayLoad data = new PayLoad();
        data.setFIN(true);
        dataToSend.setData(data);

        // Calculate checksum for the data to send to client.
        short checkSum = CheckSumService.generateCheckSum(dataToSend);
        dataToSend.setChecksum(checkSum);

        return dataToSend;
    }
}