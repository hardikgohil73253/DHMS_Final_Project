package FrontEnd;


public class ClientRequest {
    private String function = "null";
    private String USER_ID = "null";
    private String APPOINTMENT_TYPE = "null";
    private String OLD_APPOINTMENT_TYPE = "null";
    private String APPOINTMENT_ID = "null";
    private String OLD_APPOINTMENT_ID = "null";
    private String FeIpAddress = FrontEnd.FE_IP_Address;
    private int bookingCapacity = 0;
    private int SEQUENCER_NUMBER = 0;
    private String MessageType = "00";
    private int retryCount = 1;

    public ClientRequest(String function, String USER_ID) {
        setFunction(function);
        setUSER_ID(USER_ID);
    }

    public ClientRequest(int rmNumber, String bugType) {
        setMessageType(bugType + rmNumber);
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

    public String getAPPOINTMENT_TYPE() {
        return APPOINTMENT_TYPE;
    }

    public void setAPPOINTMENT_TYPE(String APPOINTMENT_TYPE) {
        this.APPOINTMENT_TYPE = APPOINTMENT_TYPE;
    }

    public String getOLD_APPOINTMENT_TYPE() {
        return OLD_APPOINTMENT_TYPE;
    }

    public void setOLD_APPOINTMENT_TYPE(String OLD_APPOINTMENT_TYPE) {
        this.OLD_APPOINTMENT_TYPE = OLD_APPOINTMENT_TYPE;
    }

    public String getAPPOINTMENT_ID() {
        return APPOINTMENT_ID;
    }

    public void setAPPOINTMENT_ID(String APPOINTMENT_ID) {
        this.APPOINTMENT_ID = APPOINTMENT_ID;
    }

    public String getOLD_APPOINTMENT_ID() {
        return OLD_APPOINTMENT_ID;
    }

    public void setOLD_APPOINTMENT_ID(String OLD_APPOINTMENT_ID) {
        this.OLD_APPOINTMENT_ID = OLD_APPOINTMENT_ID;
    }

    public int getBookingCapacity() {
        return bookingCapacity;
    }

    public void setBookingCapacity(int bookingCapacity) {
        this.bookingCapacity = bookingCapacity;
    }

    public String noRequestSendError() {
        return "request: " + getFunction() + " from " + getUSER_ID() + " not sent";
    }

    public int getSEQUENCER_NUMBER() {
        return SEQUENCER_NUMBER;
    }

    public void setSEQUENCER_NUMBER(int SEQUENCER_NUMBER) {
        this.SEQUENCER_NUMBER = SEQUENCER_NUMBER;
    }

    public String getFeIpAddress() {
        return FeIpAddress;
    }

    public void setFeIpAddress(String feIpAddress) {
        FeIpAddress = feIpAddress;
    }

    public String getMessageType() {
        return MessageType;
    }

    public void setMessageType(String messageType) {
        MessageType = messageType;
    }

    public boolean haveRetries() {
        return retryCount > 0;
    }

    public void countRetry() {
        retryCount--;
    }

    //Message Format: Sequence_id;FrontIpAddress;Message_Type;function(addAppointment,...);userID; newAppointmentID;newAppointmentType; OLD_APPOINTMENT_ID; OLD_APPOINTMENT_TYPE;bookingCapacity
    @Override
    public String toString() {
        return getSEQUENCER_NUMBER() + ";" +
                getFeIpAddress().toUpperCase() + ";" +
                getMessageType().toUpperCase() + ";" +
                getFunction().toUpperCase() + ";" +
                getUSER_ID().toUpperCase() + ";" +
                getAPPOINTMENT_ID().toUpperCase() + ";" +
                getAPPOINTMENT_TYPE().toUpperCase() + ";" +
                getOLD_APPOINTMENT_ID().toUpperCase() + ";" +
                getOLD_APPOINTMENT_TYPE().toUpperCase() + ";" +
                getBookingCapacity();
    }
}