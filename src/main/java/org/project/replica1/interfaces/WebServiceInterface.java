package org.project.replica1.interfaces;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.rmi.Remote;

/**
 * Remote interface which is extended by {@link org.project.replica1.controllers.AppointmentController}
 * Default methods are used to remove exception for unimplemented.
 */
@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface WebServiceInterface extends Remote {

    default String addAppointment(String appointmentID, String appointmentType, int capacity) {
        return "";
    }

    default String removeAppointment(String appointmentID, String appointmentType)  {
        return "";
    }

    default String listAppointmentAvailability(String appointmentType) {
        return "";
    }

    default String getAppointmentSchedule(String patientID)  {
        return "";
    }

    default String cancelAppointment(String patientID, String appointmentID)  {
        return "";
    }

    default String bookAppointment(String patientID, String appointmentID, String appointmentType)  {
        return "";
    }

    default String swapAppointment(String patientID, String oldAppointmentID, String oldAppointmentType,
                                   String newAppointmentID, String newAppointmentType) {
        return "";
    }

}
