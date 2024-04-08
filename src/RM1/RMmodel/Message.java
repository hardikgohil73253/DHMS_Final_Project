package RM1.RMmodel;

public class Message {
    public String FrontIpAddress,Function , MessageType, userID, newAppointmentID, newAppointmentType, oldAppointmentID, oldAppointmentType;
    public int bookingCapacity, sequenceId;

    public Message(int sequenceId, String FrontIpAddress,String MessageType, String Function, String userID, String newAppointmentID, String newAppointmentType,String oldAppointmentID,String oldAppointmentType,int bookingCapacity)
    {
        this.sequenceId = sequenceId;
        this.FrontIpAddress = FrontIpAddress;
        this.MessageType = MessageType;
        this.Function = Function;
        this.userID = userID;
        this.newAppointmentID = newAppointmentID;
        this.newAppointmentType = newAppointmentType;
        this.oldAppointmentID = oldAppointmentID;
        this.oldAppointmentType = oldAppointmentType;
        this.bookingCapacity = bookingCapacity;
    }
    @Override
    public String toString() {
        return sequenceId + ";" + FrontIpAddress + ";" +MessageType + ";" +Function + ";" +userID + ";" +newAppointmentID +
                ";" +newAppointmentType + ";" +oldAppointmentID + ";" +oldAppointmentType + ";" +bookingCapacity;
    }
}