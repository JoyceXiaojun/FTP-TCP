package applications;
import services.TTPServer;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FTPServer {

    private static TTPServer ttpServer;
    private static byte[] fileName;


    public static void main(String[] args) throws IOException, InterruptedIOException {
        if (args.length != 3) {
            printUsage();
        }

        short port = Short.parseShort(args[0]);
        short windowsize = Short.parseShort(args[1]);
        short retranstime = Short.parseShort(args[2]);

        ttpServer = new TTPServer();

        try {
            ttpServer.startServer(port, windowsize, retranstime);
            System.out.println("FTPServer: Starting Server at port number " + port);
            System.out.println("FTPServer: Window size is " + windowsize);
            System.out.println("FTPServer: retransmission time is " + retranstime + " milliseconds");

        } catch (Exception i) {
            i.printStackTrace();
        }

        while (true) {
            try {
                fileName = ttpServer.accept();
                if (fileName != null) {
                    // sendFile sendfile = new sendFile(fileName);
                    // new Thread(sendfile).start();

                    System.out.println("FTPServer: A request for file received from the client.");
                    System.out.println("FTPServer: The fileName is " + new String(fileName));

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String filename = new String(fileName);
                            System.out.println("The filename is = " + filename);

                            InputStream in = null;
                            File file = new File(filename);

                            try {
                                in = new FileInputStream(filename);
                                byte[] byteFile = new byte[(int) file.length()];


                                in.read(byteFile, 0, (int) file.length());
                                in.close();
                                MessageDigest mdInst = MessageDigest.getInstance("MD5");
                                mdInst.update(byteFile);

                                byte[] md = mdInst.digest();

//                                for (int i = 0; i < 16; ++i) {
//                                    System.out.println("The md5 value generated at server: " + md[i]);
//                                }

                                try {
                                    ttpServer.sendFile(byteFile, md);
                                } catch (InterruptedException i) {
                                    i.printStackTrace();
                                }

                                System.out.println("FTPServer: The file has been sent to the TTPServer from ftp server.");

                            } catch (FileNotFoundException e) {
                                ttpServer.sendErr();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    continue;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private static void printUsage() {
        System.out.println("Usage: server <serverport> <windowsize> <retranstime>\n");
        System.exit(-1);
    }
}