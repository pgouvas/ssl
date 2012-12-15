package db;

import util.Release;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class DBConnector {
    //persistent
    private ConcurrentHashMap dbkeys;
    private ConcurrentHashMap dbsessions;
    //volatile
    private HashSet ports;

    public DBConnector(){
        if (Release.debug) System.out.println("DBConnector.class Constructor CALLED");
        //Load keysDB :=HashMap(phonenumber,encryptionkey)
        try {
            dbkeys = (ConcurrentHashMap) util.Utils.deserializeObject(Release.KEYS_DB_FILE);
        } catch (Exception ex) {
            dbkeys = new ConcurrentHashMap();
            //ex.printStackTrace();
        }

        //Load sessionsDB :=HashMap(sessionid, SessionDescriptor)
        dbsessions = new ConcurrentHashMap();
        util.Utils.serializeObject(Release.SESSIONS_DB_FILE,dbsessions);

        //Initialize ports   :=HashMap(port, sessionid)
        ports = new HashSet();

    }//EoConstructor

    /*
     *  Encoding Keys
     */
    public synchronized void putEncodingKeyForPhone(String phoneno,String encodingkey){
        if (Release.debug) System.out.println("DBConnector.class putEncodingKeyForPhone() "+phoneno+" "+encodingkey);
        dbkeys.put(phoneno, encodingkey);
        util.Utils.serializeObject(Release.KEYS_DB_FILE,dbkeys);
    }//EoM putEncodingKeyForPhone

    public String getEncodingKeyForPhone(String phoneno){
        return dbkeys.get(phoneno)==null ? null : (String) dbkeys.get(phoneno);
    }//EoM getEncodingKeyForPhone

    public synchronized void removeEncodingKey(String phoneno){
        dbkeys.remove(phoneno);
        util.Utils.serializeObject(Release.KEYS_DB_FILE,dbkeys);
    }//EoM removeEncodingKey

    /*
     * Sessions
     */
    public synchronized void addSession(long sessionid, SessionDescriptor session){
        dbsessions.put(sessionid,session);
        util.Utils.serializeObject(Release.SESSIONS_DB_FILE,dbsessions);
    }//EoM addSession

    public SessionDescriptor getSession(long sessionid){
        return dbsessions.get(sessionid)!=null?(SessionDescriptor)dbsessions.get(sessionid):null;
    }//EoM getSession

    public boolean sessionExists(long sessionid){
        return dbsessions.get(sessionid)!=null?true:false;
    }//EoM sessionExists

    public synchronized void removeSession(long sessionid){
        dbsessions.remove(sessionid);
        util.Utils.serializeObject(Release.SESSIONS_DB_FILE,dbsessions);
    }//EoM removeSession

    /*
     * Ports
     */
    public synchronized void addPort(int port){
        if (!ports.contains(port))  ports.add(port);
    }//EoM addPort

    public synchronized void removePort(int port){
        if (ports.contains(port))  ports.remove(port);
    }//EoM removePort

    public HashSet getPorts(){
        return (HashSet) ports.clone();
    }//EoM getPorts

    /*
     * Utility
     */

    public synchronized void enumarateKeyEntries(){
        //Read File State
        try{
            ConcurrentHashMap clonedbkeys = (ConcurrentHashMap) util.Utils.deserializeObject(Release.KEYS_DB_FILE);
            ConcurrentHashMap clonedbsessions = (ConcurrentHashMap) util.Utils.deserializeObject(Release.SESSIONS_DB_FILE);


            System.out.println("PHONE-Keys: "+clonedbkeys.size());
            Enumeration enum1 = clonedbkeys.keys();
            while (enum1.hasMoreElements()){
                String key = (String) enum1.nextElement();
                System.out.println("phone: "+key+" key:"+clonedbkeys.get(key));
            }

            System.out.println("Sessions: "+clonedbsessions.size());
            Enumeration enum2 = clonedbsessions.keys();
            while (enum2.hasMoreElements()){
                String key = ""+(Long) enum2.nextElement();
                System.out.println("session: "+key+" descriptor:"+clonedbsessions.get(key));
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }
    }//EoM enumarateKeyEntries


}//EoC DBConnector

