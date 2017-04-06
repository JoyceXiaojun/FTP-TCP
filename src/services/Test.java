package services;

import datatypes.Datagram;
import datatypes.PayLoad;

import java.io.*;

/**
 * Created by ShuqinYe on 20/3/16.
 */
public class Test {


    public static void main(String[] args) {

//        System.out.println("ackkkkkkk".getBytes().length);
//
//        try {
//            System.out.println("maxloadsize = " + getMaxLoadSize());
//        } catch (IOException i) {
//            i.printStackTrace();
//        }

        PrintWriter pr = null;

        try {
            pr = new PrintWriter(new FileOutputStream(new File("test.txt")));
        } catch (IOException i) {
            i.printStackTrace();
        }

        for (int i = 0; i < 300; ++i) {
            pr.write("This is a test: " + i + "\n");
        }
        pr.flush();

    }


    public static short getMaxLoadSize() throws IOException {
        Datagram datagram = new Datagram();
        datagram.setDstaddr("127.0.0.1");
        datagram.setSrcaddr("127.0.0.1");
        PayLoad payLoad = new PayLoad();
        datagram.setData(payLoad);
//        payLoad.setActualData("ackkkkkkk".getBytes());

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutputStream oStream = new ObjectOutputStream(bStream);
        oStream.writeObject(datagram);
        oStream.flush();

        byte[] data = bStream.toByteArray();

        return (short) (1500 - 9 - data.length);

    }
}
