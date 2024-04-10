package controller.frontendController;


import FrontEnd.ClientRequest;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface FEInterface {
    void informRmHasBug(int RmNumber);

    void informRmIsDown(int RmNumber);

    int sendRequestToSequencer(ClientRequest clientRequest);

    //TODO : Change it according
    void retryRequest(ClientRequest clientRequest);

//    public String addAppointment(String adminID ,String appointmentID, String appointmentType, int bookingCapacity);
//
//    public String removeAppointment( String adminID , String appointmentID, String appointmentType);
//
//    public String listAppointmentAvailability( String adminID,String appointmentType);
//
//    public String bookAppointment(String patientID, String appointmentID, String appointmentType);
//
//    public String getBookingSchedule(String patientID);
//
//    public String cancelAppointment(String patientID, String appointmentID, String appointmentType);
//
//    public String swapAppointment(String patientID, String newAppointmentID, String newAppointmentType, String oldAppointmentID, String oldAppointmentType);
}