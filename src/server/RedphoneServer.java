package server;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Security;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Title:        Sample Server
 * Description:  This utility will accept input from a socket, posting back to the socket before closing the link.
 * It is intended as a template for coders to base servers on. Please report bugs to brad at kieser.net
 * Copyright:    Copyright (c) 2002
 * Company:      Kieser.net
 * @author B. Kieser
 * @version 1.0
 */

public class RedphoneServer {

    private static int port=8443;
    private static int maxConnections=0;
    private static boolean ssl = true;

    // Listen for incoming connections and handle them
    public ConcurrentHashMap cmap = new ConcurrentHashMap();   //"<+306949193033>" , socket


    public static void main(String[] args) {
       RedphoneServer socketserver = new RedphoneServer();
        socketserver.startServer();
    }

    private void startServer(){
        if (Security.getProvider("BC") == null){
            System.out.println("Bouncy Castle provider is NOT available");
            System.exit(-1);
        }
        else{
            System.out.println("Bouncy Castle provider is available");
        }

        int i=0;
        Socket server;
        ServerSocket ssocket = null;
        try{
            if (ssl){
                try {
                    //properties
                    System.setProperty("javax.net.ssl.keyStore","/home/pgouvas/jvm/jdk1.6.0_33/jre/lib/security/cacerts");
                    System.setProperty("javax.net.ssl.keyStorePassword","changeit");
                    // -Djavax.net.ssl.keyStore=/home/pgouvas/jvm/jdk1.6.0_33/jre/lib/security/cacerts -Djavax.net.ssl.keyStorePassword=changeit

                    String location =  "/home/pgouvas/jvm/jdk1.6.0_33/jre/lib/security/cacerts";
                    //load keys
                    SSLContext ctx = null;
                    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                    char[] password = "changeit".toCharArray();
                    InputStream in = (new FileInputStream(location));
                    ks.load(in, password);
                    in.close();


//                    load certificate for test
//                    Certificate cert = ks.getCertificate("redphone");
//                    System.out.println(cert);

                    //socket
                    ServerSocketFactory ssocketFactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
                    ssocket = (SSLServerSocket)ssocketFactory.createServerSocket(port);
//                    final String[] enabled = ((SSLServerSocket)ssocket).getEnabledCipherSuites();
//                    for (String s : enabled) {
//                        System.out.println(s);
//                    }
                    //set chiffer suite
                    final String[] enabledCipherSuites = {
                            //"SSL_DH_anon_WITH_RC4_128_MD5"   ,
                            "TLS_RSA_WITH_AES_128_CBC_SHA"     //,
                            //"TLS_DHE_RSA_WITH_AES_128_CBC_SHA"
                    };
                    ((SSLServerSocket)ssocket).setEnabledCipherSuites( enabledCipherSuites );

                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }  else {
                ssocket = new ServerSocket(port);
            }

            while((i++ < maxConnections) || (maxConnections == 0)){
                server = ssocket.accept();
                ServerSocketHandler handler= new ServerSocketHandler(server,this);
                Thread t = new Thread(handler);
                t.start();
            }
        } catch (IOException ioe) {
            System.out.println("IOException on socket listen: " + ioe);
            ioe.printStackTrace();
        }
    }
}


class ServerSocketHandler implements Runnable {
    private String threadname ="";
    private Socket socket;
    private RedphoneServer redserver;
    private String line="";
    private String input="";
    private boolean isalive = true;
    private Queue outgoing = new ConcurrentLinkedQueue();

    public void addToOutgoingQueue(String str){
        outgoing.add(str);
    }

    ServerSocketHandler(Socket socket, RedphoneServer redserver) {
        System.out.println("New ServerSocketHandler created");
        this.socket=socket;
        this.redserver = redserver;
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    }

    public void run () {
        try {
            // Get input from the client
            DataInputStream in = new DataInputStream (socket.getInputStream());
            PrintStream out = new PrintStream(socket.getOutputStream());

            while (isalive){
                //HANDLE INPUT
                while((line = in.readLine()) != null && !line.trim().equals("")) {
                    input+= line+"\r\n";
                    //System.out.println("LINE:"+line);
                }

                //GET /users/verification
                if (input.indexOf("users/verification")!=-1){
                    System.out.println("MSG is:" + input);
                    String phoneno = input.substring(24,37);
                    this.threadname = phoneno;
                    System.out.println("New phone:"+phoneno+" will be registered to map");
                    //Adding ServerSockethandler to Map
                    redserver.cmap.put(phoneno, this);
                    //sserver.cmap.put(phoneno, socket.getRemoteSocketAddress());
                    //System.out.println("Remote: "+socket.getRemoteSocketAddress());

                    socket.getOutputStream().write("HTTP/1.0 200 OK\r\n".getBytes());
                    socket.getOutputStream().write("Content-Length: 0\r\n".getBytes());
                    socket.getOutputStream().write("\r\n".getBytes());
                    socket.getOutputStream().flush();
                }

                //GET /users/directory
                if (input.indexOf("users/directory")!=-1){
                    System.out.println("MSG is:" + input);
                    socket.getOutputStream().write("HTTP/1.0 200 OK\r\n".getBytes());
                    socket.getOutputStream().write("Content-Length: 0\r\n".getBytes());
                    socket.getOutputStream().write("\r\n".getBytes());
                    socket.getOutputStream().flush();
                }

                //session
                if (input.indexOf("session/")!=-1){
                    String targetphone = input.substring(13,26);
                    String otp = "";
                    int otpindex = input.indexOf("OTP");
                    if (otpindex!=-1){
                        otp = input.substring(otpindex+4,otpindex+64);
                    }
                    System.out.println("Targetphone:"+targetphone +" otp: "+otp);
                    //System.out.println("CurrentList of phones ");
                    //Enumeration<String> enum1 = sserver.cmap.keys();
                    //while (enum1.hasMoreElements()){
                    //    System.out.println(enum1.nextElement());
                    //}

                    //Get Socket of Receiver
                    ServerSocketHandler handler = (ServerSocketHandler) redserver.cmap.get(targetphone);
                    //System.out.println(recsocket);
                    if (handler!=null){
                        System.out.println("Stelnw RINGGGGGGG");
                        String msg  = "RING /session/"+targetphone+" HTTP/1.0\r\n";
                        String auth = "Authorization: OTP "+otp+"\r\n";
                        //handler.addToOutgoingQueue("1");
                        //handler.addToOutgoingQueue("2");
                        //handler.addToOutgoingQueue("3");
                        //---test ok
                        handler.addToOutgoingQueue("HTTP/1.0 200 OK\r\n");
                        handler.addToOutgoingQueue("Content-Length: 0\r\n");
                        handler.addToOutgoingQueue("\r\n");
                        //--- apo queue
                        //handler.addToOutgoingQueue(msg);
                        //handler.addToOutgoingQueue(auth);
                        //handler.addToOutgoingQueue("Content-Length: 0\r\n");
                        //handler.addToOutgoingQueue("\r\n");
                        //--- apo socket
                        //recsocket.getOutputStream().write(msg.getBytes());
                        //recsocket.getOutputStream().write(auth.getBytes());
                        //recsocket.getOutputStream().write("Content-Length: 0\r\n".getBytes());
                        //recsocket.getOutputStream().write("\r\n".getBytes());
                        //recsocket.getOutputStream().flush();
                    }
                    String body="{ \"relayPort\" : \"3478\" , \"sessionId\" : \""+targetphone+"\" , \"serverName\": \"hq.ubitech.eu\" }";
                    String header = "Content-Length: "+body.getBytes().length+"\r\n";
                    socket.getOutputStream().write("HTTP/1.0 200 OK\r\n".getBytes());
                    socket.getOutputStream().write(header.getBytes());
                    socket.getOutputStream().write("\r\n".getBytes());
                    socket.getOutputStream().write(body.getBytes());
                    socket.getOutputStream().flush();
                }

                //Reset Input
                input="";
                //in.close();
                //out.flush();
                //socket.close();
                if (!outgoing.isEmpty()){
                     String str = (String) outgoing.remove();
                     System.out.println("Thread: "+threadname +" picking "+str);
                    socket.getOutputStream().write(str.getBytes());
                }  else {
                    //System.out.println("Thread: "+threadname +" No element to pick ");
                }
                socket.getOutputStream().flush();
                Thread.sleep(1000);

            }//while
        } catch (Exception ioe) {
            System.out.println("IOException on socket listen: " + ioe);
            ioe.printStackTrace();
        }
    }//EoM run

    public void end(){
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        this.isalive = false;
    }

} //EoC





//String response="HTTP/1.0 200 OK\r\n";
//String body="{ \"relayPort\" : \"3478\" , \"sessionId\" : \"12345\" , \"serverName\": \"hq.ubitech.eu\" }";
//System.out.println("Body Length:"+body.getBytes().length);
//String header = "Content-Length: "+body.getBytes().length+"\r\n";
//socket.getOutputStream().write(response.getBytes());
//socket.getOutputStream().write(header.getBytes());
//socket.getOutputStream().write("\r\n".getBytes());
//socket.getOutputStream().write(body.getBytes());


//    public void sendSignal(SocketAddress socaddress){
//        try {
//            //properties
//            System.setProperty("javax.net.ssl.keyStore","/home/pgouvas/jvm/jdk1.6.0_33/jre/lib/security/cacerts");
//            System.setProperty("javax.net.ssl.keyStorePassword","changeit");
//
//            String location =  "/home/pgouvas/jvm/jdk1.6.0_33/jre/lib/security/cacerts";
//            //load keys
//            SSLContext ctx = null;
//            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
//            char[] password = "changeit".toCharArray();
//            InputStream in = (new FileInputStream(location));
//            ks.load(in, password);
//            in.close();
//
//            //socket
//            SocketFactory ssocketFactory = (SocketFactory) SSLSocketFactory.getDefault();
//            System.out.println("socaddress:"+socaddress);
//            InetSocketAddress inetAddr = (InetSocketAddress)socaddress;
//            socket = (SSLSocket)ssocketFactory.createSocket( inetAddr.getHostName() , inetAddr.getPort() );
//            //set chiffer suite
//            final String[] enabledCipherSuites = {"TLS_RSA_WITH_AES_128_CBC_SHA" };
//            ((SSLSocket)socket).setEnabledCipherSuites( enabledCipherSuites );
//
//            socket.getOutputStream().write("HTTP/1.0 200 OK\r\n".getBytes());
//            socket.close();
//        } catch (Exception ex){
//            ex.printStackTrace();
//        }
//    }