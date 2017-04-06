package services;

import datatypes.Datagram;
import java.io.IOException;
import java.net.SocketException;
import java.util.Map;

/**
 * Created by ShuqinYe on 19/3/16.
 */
public class TTPService extends DatagramService {

    /**
     * How many datagrams can be sent without receiving ACK.
     */
    private short windowSize;

    /**
     * Time interval (in milliseconds) to re-transmit a datagram.
     */
    private short retransTimeInterval;

    /**
     * Whether the service is connected.
     */
    private boolean connected;

    /**
     * Set up a TTP service. This sets up an underlying datagram service.
     *
     * @param port    the port number used to open a socket.
     * @param verbose
     * @throws SocketException
     */
    public TTPService(int port, int verbose) throws SocketException {
        super(port, verbose);
    }

    /**
     * Initialize the TTP service by setting up the
     *
     * @param windowSize
     * @param retransTimeInterval
     */
    public void intializeService(short windowSize, short retransTimeInterval) {
        this.windowSize = windowSize;
        this.retransTimeInterval = retransTimeInterval;
    }

    /**
     * Get the window size of the TTP service.
     *
     * @return
     */
    public short getWindowSize() {
        return windowSize;
    }

    /**
     * Get the retransmission interval of the TTP service.
     *
     * @return
     */
    public short getRetransTimeInterval() {
        return retransTimeInterval;
    }

    /**
     * To open a connection.
     *
     * @param connections the map to store all connections of the TTP service.
     * @param key         the key to find which connection to add.
     */
    public void openConnection(Map<String, TTPServerCon> connections, String key, Datagram datagram) {
        TTPServerCon con = new TTPServerCon(1, (short) 1, this, datagram);
        connections.put(key, con);
    }

    /**
     * Client opens a connection.
     */
    public void openConnection() {
        connected = true;
    }

    /**
     * Receive the datagram.
     *
     * @return the datagram received.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Datagram receiveDatagram() throws IOException,
            ClassNotFoundException {
        return super.receiveDatagram();
    }

    /**
     * Send the datagram.
     *
     * @param datagram
     * @throws IOException
     */
    public void sendDatagram(Datagram datagram) throws IOException {
        super.sendDatagram(datagram);
    }

    /**
     * Server closes the connection.
     *
     * @param connections the map to store all connections of the TTP service.
     * @param key         the key to find which connection to remove.
     */
    public void closeConnection(Map<String, TTPServerCon> connections, String key) {
        TTPServerCon con = connections.get(key);
        con.closeConnetion();
        connections.remove(key);
    }

    /**
     * Client closes the connection.
     */
    public void closeConnection() {
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }
}
