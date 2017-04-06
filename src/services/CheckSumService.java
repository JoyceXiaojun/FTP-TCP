package services;

import datatypes.Datagram;
import datatypes.PayLoad;

/**
 * Created by ShuqinYe on 21/3/16.
 */
public class CheckSumService {

    /**
     * Check whether the datagram is corrupt.
     * This is used by both client and server.
     * @param datagram the datagram to be checked.
     * @return true if the datagram is corrupt, false if not.
     */
    public static boolean isCorrupt(Datagram datagram) {
        // Get the received checkSum.
        short checkSum = datagram.getChecksum();

        if (checkSum == generateCheckSum(datagram))
            return false;
        return true;
    }


    /**
     * Generate a checksum of a datagram.
     * @param datagram the datagram to calculate checksum.
     * @return the checksum value.
     */
    public static short generateCheckSum(Datagram datagram) {
        PayLoad payLoad = (PayLoad) datagram.getData();

        // variables contained in the payload.
        int seqNum = payLoad.getSeqNum();
        short ack = payLoad.getAck();
        short windowSize = payLoad.getWindowSize();
        short retransTimeInterval = payLoad.getRetransTimeInterval();

        // variables contained in the datagram.
        short size = datagram.getSize();
        String srcaddr = datagram.getSrcaddr();
        String dstaddr = datagram.getDstaddr();
        short srcport = datagram.getSrcport();
        short dstport = datagram.getDstport();

        // Find out the calculated checksum.
        int calculatedCheckSum = 0;
        byte[] byteArray1 = srcaddr.getBytes();
        byte[] byteArray2 = dstaddr.getBytes();

        // srcaddr.
        for (int i = 0; i < byteArray1.length; ++i) {
            calculatedCheckSum += ((short) byteArray1[i]);
        }

        // dstaddr.
        for (int i = 0; i < byteArray2.length; ++i) {
            calculatedCheckSum += ((short) byteArray2[i]);
        }

        // MD5
        if (payLoad.getMD5() != null) {
            for (int i = 0; i < payLoad.getMD5().length; ++i) {
                calculatedCheckSum += ((short) payLoad.getMD5()[i]);
            }
        }


        // Actual data.
        if (payLoad.getActualData() != null) {
            for (int i = 0; i < payLoad.getActualData().length; ++i) {
                calculatedCheckSum += ((short) payLoad.getActualData()[i]);
            }
        }

        // Other short variables in the main datagram.
        calculatedCheckSum += srcport;
        calculatedCheckSum += dstport;
        calculatedCheckSum += size;

        // Variables in the payload.
        calculatedCheckSum += seqNum;
        calculatedCheckSum += ack;
        calculatedCheckSum += windowSize;
        calculatedCheckSum += retransTimeInterval;

        calculatedCheckSum += (payLoad.getACK() ? 1 : 0);
        calculatedCheckSum += (payLoad.getSYN() ? 1 : 0);
        calculatedCheckSum += (payLoad.getLAST() ? 1 : 0);
        calculatedCheckSum += (payLoad.getFIN() ? 1 : 0);
        calculatedCheckSum += (payLoad.getERR() ? 1 : 0);

        return (short) (calculatedCheckSum % Short.MAX_VALUE);
    }
}
