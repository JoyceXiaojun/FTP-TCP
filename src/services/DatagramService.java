/*
 *  A Stub that provides datagram send and receive functionality
 *  
 *  Feel free to modify this file to simulate network errors such as packet
 *  drops, duplication, corruption etc. But for grading purposes we will
 *  replace this file with out own version. So DO NOT make any changes to the
 *  function prototypes
 */
package services;

import datatypes.Datagram;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


public class DatagramService {

    private int port;
    private int verbose;
    private DatagramSocket socket;
    private int count = 0;

    public DatagramService(int port, int verbose) throws SocketException {
        super();
        this.port = port;
        this.verbose = verbose;

        socket = new DatagramSocket(port);
    }

    public void sendDatagram(Datagram datagram) throws IOException {

        ByteArrayOutputStream bStream = new ByteArrayOutputStream(1500);
        ObjectOutputStream oStream = new ObjectOutputStream(bStream);
        oStream.writeObject(datagram);
        oStream.flush();

        // Create Datagram Packet
        byte[] data = bStream.toByteArray();
        InetAddress IPAddress = InetAddress.getByName(datagram.getDstaddr());

        //API: http://download.java.net/jdk7/archive/b123/docs/api/java/net/DatagramPacket.html#DatagramPacket(byte[], int, java.net.InetAddress, int)
        DatagramPacket packet = new DatagramPacket(data, data.length,
                IPAddress, datagram.getDstport());

        // Send packet
        socket.send(packet);
    }

    public Datagram receiveDatagram() throws IOException,
            ClassNotFoundException {

        byte[] buf = new byte[1500];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        socket.receive(packet);

        ByteArrayInputStream bStream = new ByteArrayInputStream(
                packet.getData());
        ObjectInputStream oStream = new ObjectInputStream(bStream);
        Datagram datagram = (Datagram) oStream.readObject();

        return datagram;
    }
}
