package util;

import org.thoughtcrime.redphone.signaling.signals.CompressedInitiateSignalProtocol;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.Random;


public class Utils {

    public static CompressedInitiateSignalProtocol.CompressedInitiateSignal generateCompressedInitiateSignal(String initiator, long sessionid, int port , String hostname) {
         return CompressedInitiateSignalProtocol.CompressedInitiateSignal.newBuilder()
                 .setInitiator(initiator)
                 .setSessionId(sessionid)
                 .setPort(port)
                 .setServerName(hostname).build();
    }//EoM generateCompressedInitiateSignal



//    Version     [1 Byte]   : A one byte version number.
//    IV (Random) [16 Bytes] : A random 16-byte IV
//    Ciphertext  [Variable] : AES-128 in CBC mode
//    MAC         [10 Bytes] : Hmac-SHA1 over the preceding bytes (encrypt-then-authenticate), truncated to 80 bits.

    public static String createEncryptedSignalMessage(String initiator, long sessionid, int port , String hostname, String targetsharedkey){
        String encodedmsg="";

        //1 Byte version
        byte[] vb = {0x00};
        String version = new String(vb);
        System.out.println("***Version length           :"+version.length());

        //16 bytes IV
        byte[] iv = new byte[16]; //Means 2048 bit
        Random random = new Random();
        random.nextBytes(iv);
        System.out.println("***IV length:               : "+iv.length);

        //3 variable bytes  Signal (~ 35 - 40)
        CompressedInitiateSignalProtocol.CompressedInitiateSignal signal = util.Utils.generateCompressedInitiateSignal(initiator,sessionid,port,hostname);
        byte[] signalbytes = signal.toByteArray();
        System.out.println("***InitialSignal.length     : "+ signalbytes.length);
        System.out.println("***InitialSignal            : "+ org.thoughtcrime.redphone.util.Base64.encodeBytes(signalbytes) );
        System.out.println("***Encrypting with TargetKey: "+targetsharedkey);
        byte[] encsignalbytes = util.Utils.encryptAES(iv,targetsharedkey,signalbytes);
        System.out.println("***EncryptedSignal.length   : "+ encsignalbytes.length);
        System.out.println("***EncryptedSignal          : "+ org.thoughtcrime.redphone.util.Base64.encodeBytes(encsignalbytes) );

        //4 concatinate all and calculate MAC
        byte[] fbytes = util.Utils.concat( vb, util.Utils.concat(iv,encsignalbytes) );
        System.out.println("***VRSION+IV+ENSignal.length: "+fbytes.length);
        System.out.println("***VRSION+IV+ENSignal       : "+org.thoughtcrime.redphone.util.Base64.encodeBytes(fbytes));

        byte[] macfull = util.Utils.generateSHA1(targetsharedkey,fbytes);
        System.out.println("***Full digest.length       : "+macfull.length);
        System.out.println("***Full digest              : "+org.thoughtcrime.redphone.util.Base64.encodeBytes(macfull));
        byte[] finalmac = new byte[10];
        System.arraycopy(macfull,10,finalmac,0,finalmac.length);
        System.out.println("***10bytesdigest.length     : "+finalmac.length);
        System.out.println("***10bytesdigest            : "+org.thoughtcrime.redphone.util.Base64.encodeBytes(finalmac));
        byte[] tosend = util.Utils.concat(fbytes,finalmac);
        System.out.println("***VRS+IV+ENC+MAC.length    : "+tosend.length);
        encodedmsg = org.thoughtcrime.redphone.util.Base64.encodeBytes(tosend);
        System.out.println("***VRS+IV+ENC+MAC           :"+encodedmsg);

        return encodedmsg;
    }//EoM createEncryptedSignalMessage

    public static byte[] concat(byte[] A, byte[] B) {
        byte[] C= new byte[A.length+B.length];
        System.arraycopy(A, 0, C, 0, A.length);
        System.arraycopy(B, 0, C, A.length, B.length);
        return C;
    }//EoM concat


    public static void serializeObject(String dbname,Object obj){
        try {
            FileOutputStream fileOut = new FileOutputStream(dbname);
            ObjectOutputStream out   = new ObjectOutputStream(fileOut);
            out.writeObject(obj);
            out.close();
            fileOut.close();
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }//EoM serializeObject

    public static Object deserializeObject(String dbname) throws IOException, ClassNotFoundException {
        Object obj=null;
            FileInputStream fileIn = new FileInputStream(dbname);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            obj = in.readObject();
            in.close();
            fileIn.close();
        return obj;
    }//EoM deserializeObject

    public static byte[] generateSHA1(String key,String message){
        byte[] digestcomplete=null;
        try {
            byte[] macKey       = org.thoughtcrime.redphone.util.Base64.decode(key);
            SecretKeySpec secretkey = new SecretKeySpec(macKey, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(secretkey);
            byte[] messageBytes = org.thoughtcrime.redphone.util.Base64.decode(message);
            mac.update(messageBytes, 0, messageBytes.length);
            //digest
            digestcomplete = mac.doFinal();
            System.out.println("generateSHA1: "+digestcomplete.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return digestcomplete;
    }//EoM generateSHA1

    public static byte[] generateSHA1(String key,byte[] messageBytes){
        byte[] digestcomplete=null;
        try {
            byte[] macKey       = getMacKey(key);
            System.out.println("***macKey.length      : "+macKey.length);
            System.out.println("***macKey             : " + org.thoughtcrime.redphone.util.Base64.encodeBytes(macKey) );
            System.out.println("***messageBytes.length: "+messageBytes.length+""     );
            System.out.println("***messageBytes       : " + org.thoughtcrime.redphone.util.Base64.encodeBytes(messageBytes)  );
            SecretKeySpec secretkey = new SecretKeySpec(macKey, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(secretkey);
            //
            mac.update(messageBytes, 0, messageBytes.length);
            //digest
            digestcomplete = mac.doFinal();
            System.out.println("generateSHA1: "+digestcomplete.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return digestcomplete;
    }//EoM generateSHA1

    public static byte[] encryptAES(byte[] ivBytes, String key, byte[] messageBytes){
        byte[] encrypted=null;
        try {
            SecretKeySpec cipherKey = new SecretKeySpec(getCipherKey(key), "AES");
            Cipher cipher      = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, cipherKey, iv);
            encrypted = cipher.doFinal(messageBytes, 0,messageBytes.length );
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return encrypted;
    }//EoM encryptAES

    public static byte[] decryptAES(byte[] ivBytes, String key, byte[] messageBytes){
        byte[] decrypted = null;
        try{
            SecretKeySpec cipherKey = new SecretKeySpec(getCipherKey(key), "AES");
            Cipher cipher      = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, cipherKey, iv);
            decrypted = cipher.doFinal(messageBytes, 0,messageBytes.length );
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return decrypted;

    }//EoM  decryptAES

    private static byte[] getCipherKey(String key) throws Exception {
        byte[] keyBytes       = getCombinedKey(key);
        byte[] cipherKeyBytes = new byte[16];
        System.arraycopy(keyBytes, 0, cipherKeyBytes, 0, cipherKeyBytes.length);
        return cipherKeyBytes;
    }//EoM

    private static byte[] getCombinedKey(String key) throws Exception {
        if (key == null) throw new Exception("No combined key available!");
        byte[] keyBytes = org.thoughtcrime.redphone.util.Base64.decode(key);
        if (keyBytes.length != 40) throw new Exception("Local cipher+mac key != 40 bytes?");
        return keyBytes;
    }//EoM getCombinedKey

    private static byte[] getMacKey(String key) throws Exception {
        byte[] keyBytes    = getCombinedKey(key);
        byte[] macKeyBytes = new byte[20];
        System.arraycopy(keyBytes, 16, macKeyBytes, 0, macKeyBytes.length);
        return macKeyBytes;
    }

}//EoC Utils
