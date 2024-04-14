package org.project.front_end;


import org.project.utils.VariableStore;

public class ClientRequest {
    private String function = "null";
    private String USER_ID = "null";
    private String APPOINTMENT_TYPE = "null";
    private String APPOINTMENT_ID = "null";
    private String OLD_APPOINTMENT_ID = "null";
    private String FE_IP_ADDRESS = VariableStore.SEQUENCER_IP;
    private int booking_Capacity = 0;
    private int sequence_Number = 0;
    private String Message_Type = "00";
    private int retryCount = 1;

    public ClientRequest(String function, String userID) {
        setFunction(function);
        setUserID(userID);
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

    public String getUserID() {
        return USER_ID;
    }

    public void setUserID(String userID) {
        this.USER_ID = userID;
    }

    public String getAppointmentType() {
        return APPOINTMENT_TYPE;
    }

    public void setAppointmentType(String appointmentType) {
        this.APPOINTMENT_TYPE = appointmentType;
    }

    public String getOldAppointmentType() {
        return APPOINTMENT_TYPE;
    }

    public void setOldAppointmentType(String OldAppointmentType) {
        this.APPOINTMENT_TYPE = OldAppointmentType;
    }

    public String getAppointmentID() {
        return APPOINTMENT_ID;
    }

    public void setAppointmentID(String appointmentID) {
        this.APPOINTMENT_ID = appointmentID;
    }

    public String getOldAppointmentID() {
        return OLD_APPOINTMENT_ID;
    }

    public void setOldAppointmentID(String OldAppointmentID) {
        this.OLD_APPOINTMENT_ID = OldAppointmentID;
    }

    public int getBookingCapacity() {
        return booking_Capacity;
    }

    public void setBookingCapacity(int bookingCapacity) {
        this.booking_Capacity = bookingCapacity;
    }

    public String noRequestSendError() {
        return "request: " + getFunction() + " from " + getUserID() + " not sent";
    }

    public int getSequenceNumber() {
        return sequence_Number;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequence_Number = sequenceNumber;
    }

    public String getFeIpAddress() {
        return FE_IP_ADDRESS;
    }

    public String getMessageType() {
        return Message_Type;
    }

    public void setMessageType(String messageType) {
        Message_Type = messageType;
    }

    public boolean haveRetries() {
        return retryCount > 0;
    }

    public void countRetry() {
        retryCount--;
    }

    @Override
    public String toString() {
        return getSequenceNumber() + ";" + getFeIpAddress().toUpperCase() + ";" + getMessageType().toUpperCase() + ";" + getFunction().toUpperCase() + ";" + getUserID().toUpperCase() + ";" + getAppointmentID().toUpperCase() + ";" + getAppointmentType().toUpperCase() + ";" + getOldAppointmentID().toUpperCase() + ";" + getOldAppointmentType().toUpperCase() + ";" + getBookingCapacity();
    }
}
