package db;

import util.Release;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

public class DBConnector {

    private ConcurrentHashMap dbkeys;
    private ConcurrentHashMap dbsessions;

    public DBConnector(){
        if (Release.debug) System.out.println("DBConnector.class Constructor CALLED");
        try {
            dbkeys = (ConcurrentHashMap) util.Utils.deserializeObject(Release.KEYS_DB_FILE);
        } catch (Exception ex) {
            dbkeys = new ConcurrentHashMap();
            //ex.printStackTrace();
        }
        try {
            dbsessions = (ConcurrentHashMap) util.Utils.deserializeObject(Release.SESSIONS_DB_FILE);
        } catch (Exception ex) {
            dbsessions = new ConcurrentHashMap();
            //ex.printStackTrace();
        }
    }//EoConstructor

    public synchronized String getEncodingKeyForPhone(String phoneno){
        return dbkeys.get(phoneno)==null ? null : (String) dbkeys.get(phoneno);
    }//EoM

    public synchronized void putEncodingKeyForPhone(String phoneno,String encodingkey){
        if (Release.debug) System.out.println("DBConnector.class putEncodingKeyForPhone() "+phoneno+" "+encodingkey);
        dbkeys.put(phoneno, encodingkey);
        //TODO make persistency on close
        util.Utils.serializeObject(Release.KEYS_DB_FILE,dbkeys);
    }//EoM

    public void enumarateKeyEntries(){
        System.out.println(dbkeys.size());
        Enumeration enum1 = dbkeys.keys();
        while (enum1.hasMoreElements()){
            String key = (String) enum1.nextElement();
            System.out.println("key: "+key+" value:"+dbkeys.get(key));
        }
    }


}//EoC DBConnector

