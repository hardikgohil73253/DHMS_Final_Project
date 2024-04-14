package org.project.utils;

public class VariableStore {


    public static final int QUEBEC_SERVER_PORT1 = 8079;

    public static final String INET_MULTICAST_ADDRESS = "localhost";

    public static final int MONTREAL_SERVER_PORT2 = 7077;

    public static final int SHERBROOKE_SERVER_PORT2 = 7078;

    public static final int QUEBEC_SERVER_PORT2 = 7079;

    public static final int MONTREAL_SOCKET_PORT2 = 7033;

    public static final int SHERBROOKE_SOCKET_PORT2 = 7034;

    public static final int MONTREAL_SOCKET_PORT3 = 7133;

    public static final int SHERBROOKE_SOCKET_PORT3 = 7134;

    public static final int QUEBEC_SOCKET_PORT3 = 7135;

    public static final int MONTREAL_SERVER_PORT4 = 7577;

    public static final int SHERBROOKE_SERVER_PORT4 = 7578;

    public static final int QUEBEC_SERVER_PORT4 = 7579;

    public static final int MONTREAL_SOCKET_PORT4 = 7533;

    public static final int SHERBROOKE_SOCKET_PORT4 = 7534;

    public static final int QUEBEC_SOCKET_PORT4 = 7535;

    public static final int SEQUENCER_PORT = 8087;

    public static final int REPLICA4_PORT = 9219;

    public static int getSocketPortsForReplica1(SERVERS servers) {
        switch (servers) {
            case MTL -> {
                return 8033;
            }
            case QUE -> {
                return 8035;
            }
            case SHE -> {
                return 8034;
            }
            default -> {
                return 0;
            }
        }
    }

    public static int getSocketPortsForReplica2(SERVERS servers) {
        switch (servers) {
            case MTL -> {
                return MONTREAL_SOCKET_PORT2;
            }
            case QUE -> {
                return 7035;
            }
            case SHE -> {
                return SHERBROOKE_SOCKET_PORT2;
            }
            default -> {
                return 0;
            }
        }
    }

    public static int getSocketPortsForReplica3(SERVERS servers) {
        switch (servers) {
            case MTL -> {
                return MONTREAL_SOCKET_PORT3;
            }
            case QUE -> {
                return QUEBEC_SOCKET_PORT3;
            }
            case SHE -> {
                return SHERBROOKE_SOCKET_PORT3;
            }
            default -> {
                return 0;
            }
        }
    }

    public static int getSocketPortsForReplica4(SERVERS servers) {
        switch (servers) {
            case MTL -> {
                return MONTREAL_SOCKET_PORT4;
            }
            case QUE -> {
                return QUEBEC_SOCKET_PORT4;
            }
            case SHE -> {
                return SHERBROOKE_SOCKET_PORT4;
            }
            default -> {
                return 0;
            }
        }
    }

    public static int getReplicaPort(int i) {
        switch (i) {
            case 0 -> {
                return 8099;
            }
            case 1 -> {
                return 9099;
            }
            case 2 -> {
                return 9119;
            }
            case 3 -> {
                return REPLICA4_PORT;
            }
            default -> {
                return 0;
            }
        }
    }

    public enum SERVERS {
        MTL,
        SHE,
        QUE
    }

    public enum ROLES {
        A,
        P
    }

    public static final int FRONT_END_PORT = 4555;

    public enum APPOINTMENT_TIME {
        M,
        A,
        E
    }

    public static final String SEQUENCER_IP = "localhost";

    public enum APPOINTMENT_TYPE {
        PHYSICIAN,
        SURGEON,
        DENTAL
    }
    public static final int LOG_TYPE_SERVER = 1;

    public static final int LOG_TYPE_CLIENT = 0;

    public enum APPOINTMENT_RESULT {
        Success,
        Fail,
        Exist_Already,
        Not_Found
    }
}
