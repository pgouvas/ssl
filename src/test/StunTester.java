package test;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class StunTester {
    private final static int PACKETSIZE = 100;
    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket();
            String data = "Initiate Stun Server";
            byte[] receive = data.getBytes();

            InetAddress host = InetAddress.getByName("94.75.243.141");
            DatagramPacket packet = new DatagramPacket(receive,receive.length,host,39492);
            socket.send(packet);
            // Print the response
            System.out.println( new String(packet.getData()) ) ;

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }//EoM main

}//EoC StunTester
