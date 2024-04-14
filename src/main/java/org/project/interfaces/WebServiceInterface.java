package org.project.interfaces;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.rmi.Remote;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface WebServiceInterface extends Remote {

    default String addAppointment(String userId,String appointmentID, String appointmentType, int capacity) {return "";    }

    default String removeAppointment(String userId,String appointmentID, String appointmentType)  {
        return "";
    }

    default String listAppointmentAvailability(String userId,String appointmentType) {
        return "";
    }

    default String getAppointmentSchedule(String userId,String patientID)  {
        return "";
    }

    default String cancelAppointment(String userId,String patientID, String appointmentID)  {
        return "";
    }

    default String bookAppointment(String userId,String patientID, String appointmentID, String appointmentType)  { return "";    }

    default String swapAppointment(String userId,String patientID, String oldAppointmentID, String oldAppointmentType,String newAppointmentID, String newAppointmentType) {return "";}
}
