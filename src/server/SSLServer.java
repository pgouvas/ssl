package server;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Security;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: pgouvas
 * Date: 12/4/12
 * Time: 9:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class SSLServer {

    private static int port=8443;
    private static int maxConnections=0;

    public static void main(String[] args) {
        SSLServer sslServer = new SSLServer();
        sslServer.startServer();
    }//EoM main

    public void startServer() {
        if (Security.getProvider("BC") == null) {
            System.out.println("Bouncy Castle provider is NOT available");
            System.exit(-1);
        } else {
            System.out.println("Bouncy Castle provider is available");
        }

        int i = 0;
        Socket server;
        ServerSocket ssocket = null;
        try {

                //Security Properties
                System.setProperty("javax.net.ssl.keyStore", "/home/pgouvas/workspace/redphoneserver/identitystore.jks");
                System.setProperty("javax.net.ssl.keyStorePassword", "123456");    //if the storepassword and private key password are different then an exception is thrown
                System.setProperty("javax.net.ssl.keyStoreType", "jks");
                System.setProperty("javax.net.ssl.trustStore", "/home/pgouvas/workspace/redphoneserver/truststore.jks");
                System.setProperty("javax.net.ssl.trustStorePassword", "123456");
                System.setProperty("javax.net.ssl.trustStoreType", "jks");

                //socket
                ServerSocketFactory ssocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                ssocket = (SSLServerSocket) ssocketFactory.createServerSocket(port);

                //set chiffer suite
                final String[] enabledCipherSuites = {
                        "TLS_RSA_WITH_AES_128_CBC_SHA"     //,
                };
                ((SSLServerSocket) ssocket).setEnabledCipherSuites(enabledCipherSuites);
                ((SSLServerSocket) ssocket).setNeedClientAuth(true);

            while ((i++ < maxConnections) || (maxConnections == 0)) {
                server = ssocket.accept();
                server.setKeepAlive(true);
                SocketHandler handler = new SocketHandler(server);
                Thread t = new Thread(handler);
                t.start();
            }
        } catch (Exception ioe) {
            System.out.println("IOException on socket listen: " + ioe);
            ioe.printStackTrace();
        }
    }  //EoM startServer

} //EoClass




class SocketHandler implements Runnable {
    private String sockethandlername ="";
    private Socket socket;
    private boolean isalive = true;


    SocketHandler(Socket socket) {
        Random rand = new Random();
        String name = ""+rand.nextInt();
        this.sockethandlername = name;
        Thread.currentThread().setName(name);
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        this.socket=socket;
        System.out.println("SocketHandler created "+name);
    } //EoConstructor

    public void run () {
        try {
            // Get input from the client
            DataInputStream in = new DataInputStream (socket.getInputStream());
            PrintStream out = new PrintStream(socket.getOutputStream());
            //Initiate Handlers since this is a persistent connection
            InputStreamHandler inhandler = new InputStreamHandler(sockethandlername,in);
            Thread inthread = new Thread(inhandler);
            OutputStreamHandler outhandler = new OutputStreamHandler(sockethandlername,out);
            Thread outthread = new Thread(outhandler);
            //StartThreads
            inthread.start();
            outthread.start();

            while (isalive){
                Thread.sleep(100);
            }//while
        } catch (Exception ioe) {
            System.out.println("IOException on socket listen: " + ioe);
            ioe.printStackTrace();
        }
    }//EoM run

    public void end(){
        try {
            this.socket.close();
            System.out.println("Socket Closed: " + sockethandlername);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }//EoM end()

} //EoC SocketHandler

class InputStreamHandler implements Runnable {
    private String inputhandlername="";
    private BufferedReader in;
    private boolean isalive = true;
    private String line = "";

    InputStreamHandler(String name, InputStream inputstream) {
        this.inputhandlername = "input"+name;
        this.in = new BufferedReader(new InputStreamReader(inputstream));
        System.out.println("InputStreamHandler created: "+inputhandlername);
    } //EoConstructor

    public void run () {
        try {
            //HANDLE INPUT
            while(isalive && (line = in.readLine()) != null ) {
                System.out.println("Input Line from "+inputhandlername+line);
            } //while
            System.out.println("while exited @InputHandler: "+inputhandlername);
            end();
        } catch (Exception ioe) {
            System.out.println("IOException on socket listen: " + ioe);
            ioe.printStackTrace();
            end();
        }
    }//EoM run

    public void end(){
        try {
            this.in.close();
            System.out.println("InputStreamHandler ended: "+inputhandlername);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }//EoM end()

} //EoC InputStreamHandler




class OutputStreamHandler implements Runnable {
    private String outputhandlername="";
    private OutputStream outputstream;
    private boolean isalive = true;


    OutputStreamHandler(String name, OutputStream outputstream) {
        this.outputhandlername = "output"+name;
        this.outputstream = outputstream;
        System.out.println("OutputStreamHandler created: "+outputhandlername);
    } //EoConstructor

    public void run () {
        try {

        } catch (Exception ioe) {
            System.out.println("IOException on socket listen: " + ioe);
            ioe.printStackTrace();
        }
    }//EoM run

    public void end(){
        try {
            this.outputstream.close();
            System.out.println("OutputStreamHandler ended: "+outputhandlername);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }//EoM end()

} //EoC OutputStreamHandler


