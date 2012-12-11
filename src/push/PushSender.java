package push;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import java.io.IOException;


public class PushSender {

    public static void main(String[] args) {
        Sender sender = new Sender("AIzaSyBfP5r1HK5kA6WS-VaoYMADCN3W7hp1vAo");
        Message msg = new Message.Builder().build();
        try {
            Result result = sender.send(msg,"APA91bGsPYpzQCjLSEHituoDWDzWx2mGjt__-3N2ylJHSkCU8GP8nig06ZcGbMdiSVEGecTehHjv4866oHKPMaTBe8xG2Zf6qlda_cceO7IJ6aXfXrMqxbSiytm_SMb3S-gzUIZFq4ZPM_P2Aic42N-q64RY9rmnJg",1);
            System.out.println(result.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
