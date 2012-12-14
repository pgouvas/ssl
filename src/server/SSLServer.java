package server;

import db.DBConnector;
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
import java.net.*;
import java.security.Security;
import java.util.Random;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: pgouvas
 * Date: 12/4/12
 * Time: 9:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class SSLServer {

    private static int port=Release.REDPHONE_SERVER_PORT;
    private static int maxConnections=Release.REDPHONE_SERVER_MAX_CONNECTIONS;
    private static boolean persistent = true;
    private static boolean two_way_ssl = false;
    private DBConnector database;

    public SSLServer(){
        //initialize database
        database = new DBConnector();
    }//EoConstructor

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
                SocketHandler handler = new SocketHandler(server,database);
                Thread t = new Thread(handler);
                t.start();
            }
        } catch (Exception ioe) {
            System.out.println("IOException on socket listen: " + ioe);
            ioe.printStackTrace();
        }
    }  //EoM startServer

    public static void main(String[] args) {
        SSLServer sslServer = new SSLServer();
        sslServer.startServer();
    }//EoM main

} //EoClass


class SocketHandler implements Runnable {
    private String sockethandlername ="";
    private Socket socket;
    private boolean isalive = true;
    private BufferedReader in;
    private String baseauth = "";
    private int minimumPort= Release.MINIMUM_RELAY_PORT;
    private int maximumPort= Release.MAXIMUM_RELAY_PORT;
    private DBConnector database;

    SocketHandler(Socket socket,DBConnector database) {
        Random rand = new Random();
        String name = ""+Math.abs(rand.nextInt());
        this.sockethandlername = name;
        Thread.currentThread().setName(name);
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        this.socket=socket;
        this.database=database;
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
                log("LINE FROM "+sockethandlername,line);

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

            log("SEGMENT",input);

            if (input.indexOf("GET /users/verification")!=-1){
                log("HANDLER - GET /users/verification");
                int basicindex = input.indexOf("Basic");
                if (basicindex!=-1){
                    baseauth = input.substring(basicindex+6,basicindex+58);
                    byte[] dec = Base64.decode(baseauth);
                    String decoded = new String(dec);
                    String phonenumber = decoded.substring(0,13);
                    String password = decoded.substring(14,decoded.length());
                    System.out.println("decoded:"+decoded);
                    String challenge = Generator.getChallenge();
                    System.out.println("Random challenge:"+challenge);

                    String smsmsg = "A%20RedPhone%20is%20trying%20to%20verify%20you:"+challenge;
                    SMSSender.sendSMS(Credentials.VOIBUSTER_USERNAME, Credentials.VOIBUSTER_PASSWORD, Credentials.VOIBUSTER_FROM, phonenumber, smsmsg);
                    System.out.println("Sended SMS with challenge: "+smsmsg);
                }
                //default response
                sendOK200();
                System.out.println("Sended OK200 ");
            }//GET /users/verification

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
                    System.out.println("decoded:"+decoded);
                    System.out.println("password:"+password);
                    int passwordpivot = input.indexOf("\"key\":\"");
                    int lastpivot=input.lastIndexOf("}");
                    String masterpassword = input.substring(passwordpivot+7,lastpivot-1);
                    System.out.println("Master Password:"+masterpassword);
                    database.putEncodingKeyForPhone(phonenumber,masterpassword);
                    System.out.println("Inserted to Database!");

                }//if
                //default response
                sendOK200();
                System.out.println("Sended OK200 ");
            }//PUT /users/verification

            if (input.indexOf("GET /users/directory")!=-1){
                log("HANDLER - GET /users/directory");
                //log("REQUEST users/directory", input);
                sendOK200();
                System.out.println("Sended OK200 ");
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
                String targetphonenumber = input.substring(pivot+9,pivot+22);
                System.out.println("Target phone: "+targetphonenumber);
                int otppivot = input.indexOf("OTP");
                String otp = input.substring(otppivot + 4, otppivot+64);
                System.out.println("OTP: "+otp);
                byte[] dec = Base64.decode(otp);
                String decodedotp = new String(dec);
                System.out.println("DECODEDOTP: "+decodedotp);
                String initiatorphone = decodedotp.substring(0,13);
                System.out.println("Initiator phone: "+initiatorphone);
                String shared_key_of_target=database.getEncodingKeyForPhone(targetphonenumber);
                System.out.println("Key of target:"+shared_key_of_target);
                System.out.println("Key of target:"+shared_key_of_target.length());
                //if (shared_key_of_target.endsWith("\u003d\u003d")) {
                    System.out.println("Normalisation");
                    shared_key_of_target = shared_key_of_target.substring(0,shared_key_of_target.length()-12)+"==";
                //}
                System.out.println("Final Key of target:"+shared_key_of_target);
                //Generate sessionid and port
                String sessionid = Generator.getSession();
                int randomport = getRandomPortWithinRange(minimumPort,maximumPort);
                String relaystr = Release.REDPHONE_SERVER;
                System.out.println("SessionID: "+sessionid+" allocating Port: "+randomport);
                //Adding session to the database
                //Initiate Relay Thread
                RelayHandler relay = new RelayHandler(this,randomport);
                Thread relaythread = new Thread(relay);
                relaythread.start();
                String json = constructInitCallJson(randomport,sessionid,relaystr);
                System.out.println("Sending back Json:"+json);

                //SMS
                String payload = util.Utils.createEncryptedSignalMessage( initiatorphone, new Long(sessionid), randomport,relaystr,shared_key_of_target);
                System.out.println("payload before URL encoding   : "+payload);
                System.out.println("payload length before encoding: "+payload.length());
                //payload=URLEncoder.encode(payload,"ISO-8859-1");
                //System.out.println("payload after URL encoding: "+payload);
                payload=payload.replaceAll("\\+","~");
                System.out.println("payload after Character replacement: "+payload);
                String smsmsg = "RedPhone%20call:"+payload;
                System.out.println("Senging SMS\n"+smsmsg);
                SMSSender.sendSMS(Credentials.VOIBUSTER_USERNAME, Credentials.VOIBUSTER_PASSWORD, Credentials.VOIBUSTER_FROM, targetphonenumber, smsmsg);
                System.out.println("Senging OK Signal");
                sendOK200(json);
            }


            //GET /open/sessionid HTTP/1.0
            if (input.indexOf("GET /open")!=-1){
                log("HANDLER - GET /open");
                sendOK200();
            }

            //RING /session/1263989551 HTTP/1.0
            if (input.indexOf("RING /session")!=-1){
                log("HANDLER - RING /session");
                sendOK200();
            }

            if (input.indexOf("DELETE /c2dm")!=-1){
                log("HANDLER - DELETE /c2dm");
                sendOK200();
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }
    } //EoM handleSegment


    public String constructInitCallJson(int port, String sessionid, String servername){
        return "{" +
                "\"relayPort\" : \""+port+"\"       ,"+       // The UDP port for this session allocated on the relay server.
                "\"sessionId\" : \""+sessionid+"\"  ,"+       // The session ID allocated for this session.
                "\"serverName\": \""+servername+"\" "+        // The name of the relay server.
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

    private int getRandomPortWithinRange(int minimum, int maximum){
        Random rn = new Random();
        int n = maximum - minimum + 1;
        int i = rn.nextInt() % n;
        int randomNum =  minimum + i;
        return randomNum;
    }

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






class RelayHandler implements Runnable{
    private boolean isactive=true;
    SocketHandler sockethandler;
    private int relayPort;
    DatagramSocket serverSocket;
    byte[] receiveData = new byte[1024];
    Vector<Object> remoteAddress = new Vector<Object>();
    Vector<Object> remotePort = new Vector<Object>();

    public RelayHandler(SocketHandler sockethandler,int relayPort){
        this.relayPort=relayPort;
        this.sockethandler = sockethandler;
        try{
            serverSocket=new DatagramSocket(relayPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }//End of constructor

    @Override
    public void run() {
        System.out.println("Initialize UDP Server at "+relayPort);
        while(isactive){
            try{
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String sentence = new String( receivePacket.getData());
                System.out.println("RECEIVED: " + sentence);
                InetAddress IPAddress = receivePacket.getAddress();
                int port = receivePacket.getPort();
                //handle
                handleSentense(sentence,IPAddress,port);

                String sender =  receivePacket.getAddress() +"@"+ receivePacket.getPort();
                //Checks of a connection already exists
                this.checkDuplicate(receivePacket);
                //Unicast the message to other clients
                this.unicastMessage(receivePacket);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }//EoM while
    }//EoM run

    public void handleSentense(String str, InetAddress inet, int port){
        try {
            System.out.println("UDP sends OK200 back to "+inet+":"+port);
            sendOK200(inet,port);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }//EoM handlePacket

    public void sendOK200(InetAddress IPAddress, int port) throws IOException{
        String ret="";
        ret+="HTTP/1.0 200 OK\r\n";
        ret+="Content-Length: 0\r\n";
        ret+="\r\n";
        byte[] sendData = new byte[1024];
        sendData = ret.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        serverSocket.send(sendPacket);
    } //EoM sendOK200

    public void end(){
            this.isactive = false;
    }//EoM end()


    public void  unicastMessage(DatagramPacket request){
        String sender =  request.getAddress() +"@"+ request.getPort();
        DatagramPacket reply = new DatagramPacket(request.getData(),request.getLength(), request.getAddress(), request.getPort());
        int counter =0;
        try {
            System.out.println("\n[ START MESSAGE TRANSMISSION ]\n");
            for(int i=0;i<this.remoteAddress.size();i++){

                String receiver = (this.remoteAddress.get(i).toString()+"@"+this.remotePort.get(i).toString());

                //Send messages to All clients beyond me
                if (!(receiver.equalsIgnoreCase(sender))){
                    counter++;
                    System.out.println("<-- Unicasting message from: "+sender+" to " + receiver);
                    reply =  new DatagramPacket(request.getData(),request.getLength(), (InetAddress) remoteAddress.get(i), Integer.parseInt(remotePort.get(i).toString()) );

                    serverSocket.send(reply);
                } else {
                    //Sent Acknowledge to client
                    //reply =  new DatagramPacket(message,message.length, (InetAddress) remoteAddress.get(i), Integer.parseInt(remotePort.get(i).toString()) );
                    //serverSocket.send(reply);
                }
            }
            System.out.println("\n[ END MESSAGE TRANSMISSION, TOTAL RECEIVERS :"+ counter+" ]\n");

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }//EoM unicastMessage


    public boolean checkDuplicate(DatagramPacket request){
        String sender =  request.getAddress() +"@"+ request.getPort();
        for (int i=0;i<remoteAddress.size();i++)
            if (  (this.remoteAddress.get(i).toString()+"@"+this.remotePort.get(i).toString()).equalsIgnoreCase(sender))
                return true;

        remoteAddress.add( (Object)request.getAddress());
        remotePort.add(request.getPort()) ;
        return false;
    }//EoM checkDuplicate


}//EoC RelayHandler