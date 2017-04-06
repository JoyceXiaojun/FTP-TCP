package datatypes;

import java.io.Serializable;

/** TTP Segment.
 * The payload will be the data object type in the parent class Datagram.
 *
 * Created by ShuqinYe on 20/3/16.
 */
public class PayLoad implements Serializable {

    // Below are all the flags.

    /**
     * Flag for acknowledgement, check whether this packet is an ACK.
     */
    private boolean ACK;

    /**
     * Flag for syn, check whether this datagram is a SYN.
     * If syn == true, it's a SYN, otherwise not.
     */
    private boolean SYN;

    /**
     * Decide whether the datagram of a seqNum is the last sequence in the transmitted file.
     * If true, the client can close the connection.
     * If yes, the client needs to wait for more sequence.
     */
    private boolean LAST;

    /**
     * true if the client/server wants to close the connection.
     */
    private boolean FIN;

    /**
     * true if the file requested by the client doesn't exist.
     */
    private boolean ERR;


    // Below are all the other variables of short type.

    /**
     * The current sequence number of this packet.
     * The sequence number is the DATAGRAM count for the current transmitted data, not the current BYTE count.
     * The value starts from 1.
     */
    private int seqNum;

    /**
     * Decide whether the seqNum is valid in the datagram.
     * If ack == 1, it's an acknowledgement for the seqNum and before, if ack == 0, it is not an acknowledgment.
     */
    private short ack;

    /**
     * The window size of the client-server connection.
     */
    private short windowSize;

    /**
     * The retransmission time interval in milliseconds.
     */
    private short retransTimeInterval;

    /**
     * MD5 of the file.
     */
    private byte[] MD5 = new byte[16];

    /**
     * File name for the ftp application - it's the actual data.
     */
    private byte[] actualData;


    /**
     * FLAGS for the payload.
     * @return
     */
    public boolean getACK() {
        return ACK;
    }

    public void setACK(boolean ACK) {
        this.ACK = ACK;
    }

    public boolean getSYN() {
        return SYN;
    }

    public void setSYN(boolean SYN) {
        this.SYN = SYN;
    }

    public boolean getLAST() {
        return LAST;
    }

    public void setLAST(boolean LAST) {
        this.LAST = LAST;
    }

    public boolean getFIN() {
        return FIN;
    }

    public void setFIN(boolean FIN) {
        this.FIN = FIN;
    }

    public boolean getERR() {
        return ERR;
    }

    public void setERR(boolean ERR) {
        this.ERR = ERR;
    }


    /**
     * All other variables in the payload - need to be used for checkSum calculation.
     * @return
     */
    public int getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public short getAck() {
        return ack;
    }

    public void setAck(short ack) {
        this.ack = ack;
    }

    public short getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(short windowSize) {
        this.windowSize = windowSize;
    }

    public short getRetransTimeInterval() {
        return retransTimeInterval;
    }

    public void setRetransTimeInterval(short retransTimeInterval) {
        this.retransTimeInterval = retransTimeInterval;
    }

    public byte[] getActualData() {
        return actualData;
    }

    public void setActualData(byte[] actualData) {
        this.actualData = actualData;
    }

    public byte[] getMD5() {
        return MD5;
    }

    public void setMD5(byte[] MD5) {
        this.MD5 = MD5;
    }

}
