package push;

import util.Credentials;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * check http://www.voipbuster.com/en/sms/instructions
 */



public class SMSSender {

    public static void main(String[] args) {
        SMSSender sender = new SMSSender();
        sender.sendSMS(Credentials.VOIBUSTER_USERNAME, Credentials.VOIBUSTER_PASSWORD, Credentials.VOIBUSTER_FROM, "+306949193033","koukou");
    }

//TODO exception handling on xml response
/*
xml:<?xml version="1.0" encoding="utf-8"?>
xml:<SmsResponse>
xml:	<version>1</version>
xml:	<result>1</result>
xml:	<resultstring>success</resultstring>
xml:	<description></description>
xml:	<partcount>1</partcount>
xml:	<endcause></endcause>
xml:</SmsResponse>
 */
    public static void sendSMS(String username, String password,String from,String phone,String message) {
        try {
            URL sendurl = new URL("https://www.voipbuster.com/myaccount/sendsms.php?username="+username+"&password="+password+"&from="+from+"&to="+phone+"&text="+message);
            BufferedReader in = new BufferedReader( new InputStreamReader(sendurl.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null){
                //System.out.println("xml:"+inputLine);
            }
            in.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
