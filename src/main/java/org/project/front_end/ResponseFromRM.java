package org.project.front_end;

public class ResponseFromRM {

    private int SEQUENCE_ID = 0;
    private String response = "null";
    private int RM_NUMBER = 0;
    private String function = "null";
    private String USER_ID = "null";
    private String NEW_APPOINTMENT_ID = "null";
    private String NEW_APPOINTMENT_TYPE = "null";
    private String OLD_APPOINTMENT_ID = "null";
    private String OLD_APPOINTMENT_TYPE = "null";
    private int bookingCapacity = 0;
    private String udpMessage = "null";
    private boolean isSuccess = false;

    public ResponseFromRM(String udpMessage) {
        setUdpMessage(udpMessage.trim());
        String[] messageParts = getUdpMessage().split(";");
        setSEQUENCE_ID(Integer.parseInt(messageParts[0]));
        setResponse(messageParts[1].trim());
        setRM_NUMBER(messageParts[2]);
        setFunction(messageParts[3]);
        setUSER_ID(messageParts[4]);
        setNEW_APPOINTMENT_ID(messageParts[5]);
        setNEW_APPOINTMENT_TYPE(messageParts[6]);
        setOLD_APPOINTMENT_ID(messageParts[7]);
        setOLD_APPOINTMENT_TYPE(messageParts[8]);
        setBookingCapacity(Integer.parseInt(messageParts[9]));
    }

    public int getSEQUENCE_ID() {
        return SEQUENCE_ID;
    }

    public void setSEQUENCE_ID(int SEQUENCE_ID) {
        this.SEQUENCE_ID = SEQUENCE_ID;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        isSuccess = response.contains("Success:");
        this.response = response;
    }

    public int getRM_NUMBER() {
        return RM_NUMBER;
    }

    public void setRM_NUMBER(String RM_NUMBER) {
        if (RM_NUMBER.equalsIgnoreCase("RM1")) {
            this.RM_NUMBER = 1;
        } else if (RM_NUMBER.equalsIgnoreCase("RM2")) {
            this.RM_NUMBER = 2;
        } else if (RM_NUMBER.equalsIgnoreCase("RM3")) {
            this.RM_NUMBER = 3;
        }else if (RM_NUMBER.equalsIgnoreCase("RM4")) {
            this.RM_NUMBER = 4;
        } else {
            this.RM_NUMBER = 0;
        }
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getUSER_ID() {
        return USER_ID;
    }

    public void setUSER_ID(String USER_ID) {
        this.USER_ID = USER_ID;
    }

    public String getNEW_APPOINTMENT_ID() {
        return NEW_APPOINTMENT_ID;
    }

    public void setNEW_APPOINTMENT_ID(String NEW_APPOINTMENT_ID) {
        this.NEW_APPOINTMENT_ID = NEW_APPOINTMENT_ID;
    }

    public String getNEW_APPOINTMENT_TYPE() {
        return NEW_APPOINTMENT_TYPE;
    }

    public void setNEW_APPOINTMENT_TYPE(String NEW_APPOINTMENT_TYPE) {
        this.NEW_APPOINTMENT_TYPE = NEW_APPOINTMENT_TYPE;
    }

    public String getOLD_APPOINTMENT_ID() {
        return OLD_APPOINTMENT_ID;
    }

    public void setOLD_APPOINTMENT_ID(String OLD_APPOINTMENT_ID) {
        this.OLD_APPOINTMENT_ID = OLD_APPOINTMENT_ID;
    }

    public String getOLD_APPOINTMENT_TYPE() {
        return OLD_APPOINTMENT_TYPE;
    }

    public void setOLD_APPOINTMENT_TYPE(String OLD_APPOINTMENT_TYPE) {
        this.OLD_APPOINTMENT_TYPE = OLD_APPOINTMENT_TYPE;
    }

    public int getBookingCapacity() {
        return bookingCapacity;
    }

    public void setBookingCapacity(int bookingCapacity) {
        this.bookingCapacity = bookingCapacity;
    }

    public String getUdpMessage() {
        return udpMessage;
    }

    public void setUdpMessage(String udpMessage) {
        this.udpMessage = udpMessage;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null) {
            if (obj instanceof ResponseFromRM) {
                ResponseFromRM obj1 = (ResponseFromRM) obj;
                return obj1.getFunction().equalsIgnoreCase(this.getFunction())
                        && obj1.getSEQUENCE_ID() == this.getSEQUENCE_ID()
                        && obj1.getUSER_ID().equalsIgnoreCase(this.getUSER_ID())
                        && obj1.isSuccess() == this.isSuccess();
            }
        }
        return false;
    }
}
