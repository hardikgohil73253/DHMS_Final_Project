package Model;

public class ClientModel {
    public static final String USER_TYPE_ADMIN = "ADMIN";
    public static final String USER_TYPE_PATIENT = "PATIENT";
    public static final String USER_SERVER_MONTREAL = "MONTREAL";
    public static final String USER_SERVER_QUEBEC = "QUEBEC";
    public static final String USER_SERVER_SHERBROOKE = "SHERBROOKE";
    private String USER_TYPE;
    private String USER_ID;
    private String USER_SERVER;

    public ClientModel(String USER_ID) {
        this.USER_ID = USER_ID;
        this.USER_TYPE = checkUSER_TYPE();
        this.USER_SERVER = checkUSER_SERVER();
    }

    private String checkUSER_SERVER() {
        if (USER_ID.substring(0, 3).equalsIgnoreCase("MTL")) {
            return USER_SERVER_MONTREAL;
        } else if (USER_ID.substring(0, 3).equalsIgnoreCase("QUE")) {
            return USER_SERVER_QUEBEC;
        } else {
            return USER_SERVER_SHERBROOKE;
        }
    }

    private String checkUSER_TYPE() {
        if (USER_ID.substring(3, 4).equalsIgnoreCase("A")) {
            return USER_TYPE_ADMIN;
        } else {
            return USER_TYPE_PATIENT;
        }
    }

    public String getUSER_TYPE() {
        return USER_TYPE;
    }

    public void setUSER_TYPE(String USER_TYPE) {
        this.USER_TYPE = USER_TYPE;
    }

    public String getUSER_ID() {
        return USER_ID;
    }

    public void setUSER_ID(String USER_ID) {
        this.USER_ID = USER_ID;
    }

    public String getUSER_SERVER() {
        return USER_SERVER;
    }

    public void setUSER_SERVER(String USER_SERVER) {
        this.USER_SERVER = USER_SERVER;
    }

    @Override
    public String toString() {
        return getUSER_TYPE() + "(" + getUSER_ID() + ") on " + getUSER_SERVER() + " Server.";
    }
}