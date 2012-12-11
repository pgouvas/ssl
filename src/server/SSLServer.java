package server;

import push.SMSSender;
import util.Base64;
import util.Credentials;
import util.Generator;
import util.Release;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private static int maxConnections=100;
    private static boolean persistent = true;
    private static boolean two_way_ssl = false;

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
                //1-way SSL
                System.setProperty("javax.net.ssl.keyStore","/home/pgouvas/jvm/jdk1.6.0_33/jre/lib/security/cacerts");
                System.setProperty("javax.net.ssl.keyStorePassword","changeit");

                //Security Properties
//                System.setProperty("javax.net.ssl.keyStore", "/home/pgouvas/workspace/redphoneserver/identitystore.jks");
//                System.setProperty("javax.net.ssl.keyStorePassword", "123456");    //if the storepassword and private key password are different then an exception is thrown
//                System.setProperty("javax.net.ssl.keyStoreType", "jks");
//                System.setProperty("javax.net.ssl.trustStore", "/home/pgouvas/workspace/redphoneserver/truststore.jks");
//                System.setProperty("javax.net.ssl.trustStorePassword", "123456");
//                System.setProperty("javax.net.ssl.trustStoreType", "jks");

                //socket
                ServerSocketFactory ssocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                ssocket = (SSLServerSocket) ssocketFactory.createServerSocket(port);

                //set chiffer suite
                final String[] enabledCipherSuites = {
                        "TLS_RSA_WITH_AES_128_CBC_SHA"     //,
                };

                ((SSLServerSocket) ssocket).setEnabledCipherSuites(enabledCipherSuites);
                if (two_way_ssl) ((SSLServerSocket) ssocket).setNeedClientAuth(true);

            while ((i++ < maxConnections) || (maxConnections == 0)) {
                server = ssocket.accept();
                if (persistent)  server.setKeepAlive(true);
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
    private BufferedReader in;

    private String baseauth = "";
    private String[] verbs = {"GET","PUT","DELETE","RING","BUSY"};

    SocketHandler(Socket socket) {
        Random rand = new Random();
        String name = ""+Math.abs(rand.nextInt());
        this.sockethandlername = name;
        Thread.currentThread().setName(name);
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        this.socket=socket;
        System.out.println("SocketHandler created "+name);
    } //EoConstructor

    public void run () {
        try {
            //The following is the non-Threaded edition
            DataInputStream dis = new DataInputStream (socket.getInputStream());
            this.in = new BufferedReader(new InputStreamReader(dis));
            String segment="";
            String line = "";
            while(isalive && (line = in.readLine()) != null ) {
                //log("LINE FROM "+sockethandlername,line);

                if (line.trim().equals("")){  //if you catch \r\n WITHOUT content-length earlier
                    handleSegment(segment);
                    segment="";
                    line="";
                }  else
                if (line.indexOf("Content-Length:")!=-1){
                    int pivot = line.indexOf(":");
                    String amounts = line.substring(pivot+1,line.length()).trim();
                    int amount = Integer.parseInt(amounts);
                    if (amount!=0){
                        //System.out.println("Have to read more "+amounts);
                        String newlinew = in.readLine();
                        segment+= line+"\r\n";
                        //System.out.println("tin roufiksaaa tin kaini");
                        char[] buffer = new char[Integer.parseInt(amounts)];
                        in.read(buffer);
                        line= new String(buffer);
                        //System.out.println("buffer: "+ line);
                        segment+= line+"\r\n";
                        handleSegment(segment);
                        segment="";
                        line="";
                    } //amount
                } //Content-Length

                else {   //default case
                    segment+= line+"\r\n";
                }

            } //while

            //Close Socket and Streams
            end();

        } catch (Exception ioe) {
            System.out.println("IOException on socket listen: " + ioe);
            ioe.printStackTrace();
        }
    }//EoM run


    public void handleSegment(String input){
        try{
            /*
                This method accepts a verification request.
                According to the Redphone flow this request should end-up in a challenge request. (an always a 200 response)
                TODO since in the enhanced version we have 2-way SSL additional response code should be included

                Example of expected input:
                GET /users/verification HTTP/1.0
                Authorization: Basic KzMwNjk0OTE5MzAzMzp0T1JnZFl0bWl1Q2JnQXMrekRXZUJWbVk=
             */

            log("segment",input);

            if (input.indexOf("GET /users/verification")!=-1){
                log("HANDLER - GET /users/verification");
                int basicindex = input.indexOf("Basic");
                if (basicindex!=-1){
                    baseauth = input.substring(basicindex+6,basicindex+58);
                    byte[] dec = Base64.decode(baseauth);
                    String decoded = new String(dec);
                    String phonenumber = decoded.substring(0,13);
                    String password = decoded.substring(14,decoded.length());
                    String challenge = Generator.getChallenge();
                    //log("Decoded",decoded);
                    //log("phonenumber",phonenumber);
                    //log("challenge",challenge);
                    String smsmsg = "A%20RedPhone%20is%20trying%20to%20verify%20you:"+challenge;
                    SMSSender.sendSMS(Credentials.VOIBUSTER_USERNAME, Credentials.VOIBUSTER_PASSWORD, Credentials.VOIBUSTER_FROM, phonenumber, smsmsg);
                }
                //default response
                sendOK200();

            }

            /*
                This method accepts a challenge-verification request from a specific number

                Example of expected input:

             */
            if (input.indexOf("PUT /users/verification")!=-1){
                log("HANDLER - PUT /users/verification");
                int basicindex = input.indexOf("Basic");
                if (basicindex!=-1){
                    baseauth = input.substring(basicindex+6,basicindex+58);
                    byte[] dec = Base64.decode(baseauth);
                    String decoded = new String(dec);
                    String phonenumber = decoded.substring(0,13);
                    String password = decoded.substring(14,decoded.length());
                    String challenge = Generator.getChallenge();
                    //log("Decoded",decoded);
                }
                //default response
                sendOK200();

            }

            if (input.indexOf("GET users/directory")!=-1){
                log("HANDLER - GET /users/directory");
                //log("REQUEST users/directory", input);
                sendOK200();
            }

            /*
                Description: Sent by an initiator client who wishes to establish a call with a responder
                Verb: GET
                Resource: /session/<responder_number>
                Response Codes:
                404: No such user, the specified responder number isn't registered with RedPhone.
                401: Authentication failed.
                200: Initiate successful.
                Response body (application/json encoded):
                {
                  "relayPort" : <port> // The UDP port for this session allocated on the relay server.
                  "sessionId" : <sessionId> // The session ID allocated for this session.
                  "serverName" : <name> // The name of the relay server.
                }
             */
            if (input.indexOf("GET /session/")!=-1){
                log("HANDLER - GET /session/+xxxxxxxxxxxx");
                int pivot = input.indexOf("/session");
                String phonenumber = input.substring(pivot+9,pivot+22);
                System.out.println("Singaling Initiate to phone: "+phonenumber);
                String sessionid = Generator.getSession();
                String json = constructInitCallJson(Release.RELAY_PORT,sessionid,"relay.ubitech.eu");  //TODO change SERVERRV
                System.out.println("Sending back Json:"+json);
                //SMS
                //String encryptedInitMessage = "";
                //String smsmsg = "RedPhone%20call:"+encryptedInitMessage;
                //System.out.println("Senging SMS");
                //SMSSender.sendSMS(Credentials.VOIBUSTER_USERNAME, Credentials.VOIBUSTER_PASSWORD, Credentials.VOIBUSTER_FROM, phonenumber, smsmsg);
                System.out.println("Senging OK Signal");
                sendOK200(json);
            }


            if (input.indexOf("DELETE /c2dm")!=-1){
                log("HANDLER - DELETE /c2dm");
                sendOK200();
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }
    } //EoM handleSegment


    public String constructInitCallJson(String port, String sessionid, String servername){
        return "{" +
                "\"relayPort\" : \""+port+"\"       ,"+              // The UDP port for this session allocated on the relay server.
                "\"sessionId\" : \""+sessionid+"\"  ,"+       // The session ID allocated for this session.
                "\"serverName\": \""+servername+"\" "+           // The name of the relay server.
                "}";
    }

    public void sendKeepAlive(String authorization){
        try{
            this.socket.getOutputStream().write("GET /keepalive HTTP/1.0\r\n".getBytes());
            this.socket.getOutputStream().write(("Authorization: Basic " + authorization + "\r\n").getBytes());
            this.socket.getOutputStream().write("Content-Length: 0\r\n".getBytes());
            this.socket.getOutputStream().write("\r\n".getBytes());
            this.socket.getOutputStream().flush();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }//EoM keep-alive

    public void sendOK200() throws IOException{
        this.socket.getOutputStream().write("HTTP/1.0 200 OK\r\n".getBytes());
        this.socket.getOutputStream().write("Content-Length: 0\r\n".getBytes());
        this.socket.getOutputStream().write("\r\n".getBytes());
        this.socket.getOutputStream().flush();
    } //EoM sendOK200

    public void sendAuthFailed401() throws IOException{
        this.socket.getOutputStream().write("HTTP/1.0 401 Authentication Failed\r\n".getBytes());
        this.socket.getOutputStream().write("Content-Length: 0\r\n".getBytes());
        this.socket.getOutputStream().write("\r\n".getBytes());
        this.socket.getOutputStream().flush();
    } //EoM sendAuthFailed401

    public void sendNoSuchUser404() throws IOException{
        this.socket.getOutputStream().write("HTTP/1.0 404 No such user\r\n".getBytes());
        this.socket.getOutputStream().write("Content-Length: 0\r\n".getBytes());
        this.socket.getOutputStream().write("\r\n".getBytes());
        this.socket.getOutputStream().flush();
    } //EoM sendNoSuchUser404

    public void sendOK200(String body) throws IOException{
        this.socket.getOutputStream().write("HTTP/1.0 200 OK\r\n".getBytes());
        this.socket.getOutputStream().write(("Content-Length: "+body.getBytes().length+"\r\n").getBytes());
        this.socket.getOutputStream().write("\r\n".getBytes());
        this.socket.getOutputStream().write(body.getBytes());
        this.socket.getOutputStream().flush();
    } //EoM sendOK200

    public void end(){
        try {
            this.socket.close();
            System.out.println("Socket Closed: " + sockethandlername);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }//EoM end()

    public void log(String label,String str){
        System.out.println(label+"----------------------------------");
        System.out.println(str);
    }

    public void log(String str){
        System.out.println(str);
     }

} //EoC SocketHandler

