package org.project.replica4.models;

import org.project.utils.VariableStore;


public class ClientModel {

    private VariableStore.ROLES clientRole;
    private String clientID;
    private VariableStore.SERVERS clientServer;
    private int serverPort;

    public ClientModel(String clientID) {
        this.clientID = clientID;
        this.clientRole = detectClientType();
        this.clientServer = detectClientServer();
        this.serverPort = detectClientServerPort();
    }

    public static VariableStore.SERVERS detectClientServer(String clientID) {
        if (clientID.substring(0, 3).equalsIgnoreCase("MTL")) {
            return VariableStore.SERVERS.MTL;
        } else if (clientID.substring(0, 3).equalsIgnoreCase("QUE")) {
            return VariableStore.SERVERS.QUE;
        } else {
            return VariableStore.SERVERS.SHE;
        }
    }


    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    private VariableStore.SERVERS detectClientServer() {
        if (clientID.substring(0, 3).equalsIgnoreCase("MTL")) {
            return VariableStore.SERVERS.MTL;
        } else if (clientID.substring(0, 3).equalsIgnoreCase("QUE")) {
            return VariableStore.SERVERS.QUE;
        } else {
            return VariableStore.SERVERS.SHE;
        }
    }

    private VariableStore.ROLES detectClientType() {
        if (clientID.substring(3, 4).equalsIgnoreCase("A")) {
            return VariableStore.ROLES.A;
        } else {
            return VariableStore.ROLES.P;
        }
    }

    private int detectClientServerPort() {
        if (clientID.substring(0, 3).equalsIgnoreCase("MTL")) {
            return VariableStore.MONTREAL_SERVER_PORT4;
        } else if (clientID.substring(0, 3).equalsIgnoreCase("QUE")) {
            return VariableStore.QUEBEC_SERVER_PORT4;
        } else {
            return VariableStore.SHERBROOKE_SERVER_PORT4;
        }
    }
}
