package controller.frontendController;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface webServiceInterface {
    default String addAppointment(String userID,String appointmentID, String appointmentType, int bookingCapacity){
        return "";
    }

    default String removeAppointment( String userID, String appointmentID, String appointmentType){
        return "";
    }

    default String listAppointmentAvailability(String userID, String appointmentType){
        return "";
    }

    default String bookAppointment(String userID, String appointmentID, String appointmentType){
        return "";
    }

    default String getBookingSchedule(String userID){
        return "";
    }

    default String cancelAppointment(String userID, String appointmentID){
        return "";
    }

    default String swapAppointment(String userID, String newAppointmentID, String newAppointmentType, String oldAppointmentID, String oldAppointmentType){
        return "";
    }

}