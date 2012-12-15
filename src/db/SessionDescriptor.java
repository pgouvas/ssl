package db;

import java.io.Serializable;

public class SessionDescriptor implements Serializable {

    public String initiator;
    public String responder;
    public int relayPort;
    public long sessionId;
    public String serverName;

    public SessionDescriptor(String initiator, String responder, int relayPort, long sessionId, String serverName) {
        this.initiator = initiator;
        this.responder = responder;
        this.relayPort = relayPort;
        this.sessionId = sessionId;
        this.serverName = serverName;
    }//EoConstructor

    @Override
    public String toString() {
        return "SessionDescriptor{" +
                "initiator='" + initiator + '\'' +
                ", responder='" + responder + '\'' +
                ", relayPort=" + relayPort +
                ", sessionId=" + sessionId +
                ", serverName='" + serverName + '\'' +
                '}';
    }
}//EoC SessionDescriptor
