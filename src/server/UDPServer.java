package server;

import java.net.DatagramSocket;

public class UDPServer {

    public static void main(String[] args) {
        try{
            DatagramSocket serverSocket = new DatagramSocket(3478);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }//EoM main



}//EoC UDPServer