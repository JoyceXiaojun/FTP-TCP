package services;

import datatypes.Datagram;
import datatypes.PayLoad;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Created by ShuqinYe on 21/3/16.
 */
public class GetSizeService {

    public static short getSize(byte[] actualData) {
        if (actualData == null)
            return (short) 0;
        return (short) actualData.length;
    }

    /**
     * Get the maximum actual data size of one segment.
     * @return
     */
    public static short getMaxLoadSize() throws IOException {
        Datagram datagram = new Datagram();
        datagram.setDstaddr("127.0.0.1");
        datagram.setSrcaddr("127.0.0.1");
        PayLoad payLoad = new PayLoad();
        datagram.setData(payLoad);

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutputStream oStream = new ObjectOutputStream(bStream);
        oStream.writeObject(datagram);
        oStream.flush();

        byte[] data = bStream.toByteArray();

        return (short) (1500 - 9 - data.length);

    }
}
