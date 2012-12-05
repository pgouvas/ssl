package client;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Random;

public class SocketClient {

    public static void main(String[] args) {

        try {
            //Security Properties
            //By default we activate truststore in order to use one-way-SSL
            System.setProperty("javax.net.ssl.trustStore", "/home/pgouvas/workspace/redphoneserver/user1truststore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "123456");
            System.setProperty("javax.net.ssl.trustStoreType", "jks");
            //The identitystore will be used during two-way-SSL
            System.setProperty("javax.net.ssl.keyStore", "/home/pgouvas/workspace/redphoneserver/user1identitystore.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "123456");    //if the storepassword and private key password are different then an exception is thrown
            System.setProperty("javax.net.ssl.keyStoreType", "jks");

            //Load Keys
            SSLContext ctx = null;

            //create
            int port = 8443;
            String hostname = "127.0.0.1";
            SocketFactory socketFactory = SSLSocketFactory.getDefault();
            Socket socket = socketFactory.createSocket(hostname, port);
            socket.setKeepAlive(true);

            //set chiffer suite
            final String[] enabledCipherSuites = {
                    "TLS_RSA_WITH_AES_128_CBC_SHA"
            };
            ((SSLSocket)socket).setEnabledCipherSuites( enabledCipherSuites );

            PrintStream out = new PrintStream(socket.getOutputStream());
            Random rand = new Random();
            int index = 10;
            while(index!=0){
                int nrand = rand.nextInt(1000);
                Thread.sleep(nrand);
                out.println("Koukou "+nrand);
                index--;
            }
            //out.close();

            PrintStream out1 = new PrintStream(socket.getOutputStream());
            Random rand1 = new Random();
            int index1 = 10;
            while(index1!=0){
                int nrand1 = rand.nextInt(1000);
                Thread.sleep(nrand1);
                out.println("Lala "+nrand1);
                index1--;
            }
            //out1.close();

            //socket.close();
        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }

}
