package applications;
import services.TTPClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class FTPClient {

    private static TTPClient ttpclient;
    private static String file_name;
    private static String outputDir;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 6) {
            printUsage();
        }

        file_name = args[0];
        outputDir = args[5];

        short srcport = Short.parseShort(args[1]);
        short desport = Short.parseShort(args[2]);
        short windowsize = Short.parseShort(args[3]);
        short retranstime = Short.parseShort(args[4]);

        // First argument is source port, second argument is destination port.

        ttpclient = new TTPClient(file_name);

        // Connection with server is open and it will persist until the connection is closed.
        ttpclient.openConnection(srcport, desport, windowsize, retranstime);

        ttpclient.closeConnection();

        System.out.println("FTPClient: Connection with server has been closed.");

        System.exit(0);
    }

    public static void sendFile(byte[] file, byte[] md) {
        byte[] data = file;
        byte[] baseMD = md;
        byte[] currMD;
        MessageDigest mdInst;
        try {
            mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(data);
            currMD = mdInst.digest();

            if (Arrays.equals(baseMD, currMD)) {
                String directory = outputDir;
                System.out.println("filename = " + file_name);

                File f = new File(directory,file_name);
                if(f.exists()) {
                    System.out.println(f.getAbsolutePath());
                    System.out.println(f.getName());
                    System.out.println(f.length());
                } else {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                    FileOutputStream out = new FileOutputStream(f);
                    BufferedOutputStream bufferStream = new BufferedOutputStream(out);
                    bufferStream.write(data);
                    bufferStream.flush();
                    bufferStream.close();
                    //bufferStream = null;
                    System.out.println("FTPClient: File has been stored in output folder.");

                }

            } else {
                System.out.println("baseMD = " + new String(baseMD));
                System.out.println("currMD = " + new String(currMD));
                System.out.println("FTPClient: File was disrupted");
            }

        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: client <file_name> <localport> <serverport> <windowsize> <retranstime> <output_directoy>\n");
        System.exit(-1);
    }

}