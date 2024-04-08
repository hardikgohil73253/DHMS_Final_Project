package RM2.webcontroller;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface webInterface {

    public String addAppointment(String appointmentID, String appointmentType, int bookingCapacity);

    public String removeAppointment( String appointmentID, String appointmentType);

    public String listAppointmentAvailability(String appointmentType);

    public String bookAppointment(String userID, String appointmentID, String appointmentType);

    public String getBookingSchedule(String userID);

    public String cancelAppointment(String userID, String appointmentID);

    public String swapAppointment(String userID, String newAppointmentID, String newAppointmentType, String oldAppointmentID, String oldAppointmentType);

}