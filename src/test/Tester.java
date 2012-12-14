package test;

import db.DBConnector;
import org.thoughtcrime.redphone.signaling.signals.CompressedInitiateSignalProtocol;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class Tester {

    public static void main(String[] args) {
        Tester tester = new Tester();

        try {
            //tester.testDBEntries();
            tester.testEncrypt();
            //tester.testDecrypt();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }//EoM main

    private void testEncrypt() throws Exception{
        //KEY
        String key="";
        //key = "8nHaTgB0RPJOXPF7SH2ycaiH0fnAfsUbvyxLoL2b7qp7amL6kDxqnA\u003d\u003d";
        key = "wykOqf7SH6HQjGNSawJraktnqVYK1rAAi3Fu8GLdXPOPOh/y6nDyQg==";

        //IV
        byte[] iv = new byte[16]; //Means 2048 bit
        Random random = new Random();
        random.nextBytes(iv);
        String ivs = org.thoughtcrime.redphone.util.Base64.encodeBytes(iv);
        //MESSAGE
        String message = "test";
        byte msgbytes[] = org.thoughtcrime.redphone.util.Base64.decode(message);
        System.out.println("key:"+key);
        System.out.println("ivs:"+ivs);
        System.out.println("message:"+message);
        byte[] encrypted = util.Utils.encryptAES(iv,key,msgbytes);
        String encryptedstr = org.thoughtcrime.redphone.util.Base64.encodeBytes(encrypted);
        System.out.println("encryptedstr: "+encryptedstr);
    }//testEncrypt

    private void testDecrypt() throws Exception {
        String ivsstr = "ge4dotr8or+gSDDl94nJ/g==";
        String keystr = "8nHaTgB0RPJOXPF7SH2ycaiH0fnAfsUbvyxLoL2b7qp7amL6kDxqnA==";
        String encryptedstr = "Y8aOMTgxfVobAqBumzwepw==";

        byte[] iv = org.thoughtcrime.redphone.util.Base64.decode(ivsstr);
        byte[] encrypted = org.thoughtcrime.redphone.util.Base64.decode(encryptedstr);

        byte[] decrypted = util.Utils.decryptAES(iv,keystr,encrypted);
        String decryptedstr = org.thoughtcrime.redphone.util.Base64.encodeBytes(decrypted);
        System.out.println("decrypted: "+decryptedstr);
    }//testDecrypt

    private void testDBEntries(){
        DBConnector database = new DBConnector();
        database.enumarateKeyEntries();
    }

    private void createCompressedSignal(){
        //1 Byte version
        byte[] vb = {1};
        String version = new String(vb);
        System.out.println("version length:"+version.length());

        //16 bytes IV
        byte[] iv = new byte[16]; //Means 2048 bit
        Random random = new Random();
        random.nextBytes(iv);
        System.out.println("IV length:"+iv.length);

        //3 variable bytes  Signal (~ 35 - 40)
        CompressedInitiateSignalProtocol.CompressedInitiateSignal signal = util.Utils.generateCompressedInitiateSignal("+306949193033",123413,3478,"62.38.242.7");
        byte[] signalbytes = signal.toByteArray();
        System.out.println("signal length:"+signalbytes.length);

        //4 concatinate all and calculate MAC
        byte[] fbytes = util.Utils.concat( vb, util.Utils.concat(iv,signalbytes) );
        System.out.println("final length:"+fbytes.length);

        byte[] mac = util.Utils.generateSHA1("password",fbytes);
        System.out.println("MAC length:"+mac.length);

    }

    private void cryptotests(){
        util.Utils.generateSHA1("12345","12345123451234512345");
    }

    private void dbtests(){
        CompressedInitiateSignalProtocol.CompressedInitiateSignal cisp = CompressedInitiateSignalProtocol.CompressedInitiateSignal.newBuilder().setInitiator("init").setSessionId(12345).setPort(33).setServerName("62.38").build();
        ConcurrentHashMap map = new ConcurrentHashMap();
        map.put("1","2");
        util.Utils.serializeObject("redphone.db",map);

        ConcurrentHashMap map1 = new ConcurrentHashMap();
        try {
            map1 = (ConcurrentHashMap) util.Utils.deserializeObject("redphone.db");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        System.out.println(map1.get("1"));
    }

}//EoClass
