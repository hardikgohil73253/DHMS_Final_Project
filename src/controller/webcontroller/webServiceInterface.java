package controller.webcontroller;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface webServiceInterface {
//    public String addAppointment(String adminID ,String appointmentID, String appointmentType, int bookingCapacity);
    public String addAppointment(String appointmentID, String appointmentType, int bookingCapacity);

    public String removeAppointment( String appointmentID, String appointmentType);

    public String listAppointmentAvailability(String appointmentType);

    public String bookAppointment(String customerID, String appointmentID, String appointmentType);

    public String getBookingSchedule(String customerID);

    public String cancelAppointment(String customerID, String appointmentID);

    public String swapAppointment(String customerID, String newAppointmentID, String newAppointmentType, String oldAppointmentID, String oldAppointmentType);

}