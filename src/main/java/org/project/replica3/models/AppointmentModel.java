package org.project.replica3.models;

import org.project.utils.VariableStore;

import java.util.ArrayList;
import java.util.List;

public class AppointmentModel {

    private VariableStore.APPOINTMENT_TYPE appointmentType;
    private String appointmentID;
    private VariableStore.SERVERS appointmentServer;
    private String appointmentDate;
    private VariableStore.APPOINTMENT_TIME appointmentTimeSlot;
    private ArrayList<String> patients;

    private int capacityOfAppointments;

    public AppointmentModel(VariableStore.APPOINTMENT_TYPE appointmentType, String appointmentID, int capacity) {
        this.appointmentID = appointmentID;
        this.appointmentType = appointmentType;
        this.appointmentTimeSlot = detectAppointmentTimeSlot(appointmentID);
        this.appointmentServer = detectAppointmentServer(appointmentID);
        this.appointmentDate = detectAppointmentDate(appointmentID);
        this.capacityOfAppointments = capacity;
        patients = new ArrayList<>();
    }

    public static VariableStore.SERVERS detectAppointmentServer(String appointmentID) {
        if (appointmentID.substring(0, 3).equalsIgnoreCase("MTL")) {
            return VariableStore.SERVERS.MTL;
        } else if (appointmentID.substring(0, 3).equalsIgnoreCase("QUE")) {
            return VariableStore.SERVERS.QUE;
        } else {
            return VariableStore.SERVERS.SHE;
        }
    }

    public static VariableStore.APPOINTMENT_TIME detectAppointmentTimeSlot(String appointmentID) {
        if (appointmentID.substring(3, 4).equalsIgnoreCase("M")) {
            return VariableStore.APPOINTMENT_TIME.M;
        } else if (appointmentID.substring(3, 4).equalsIgnoreCase("A")) {
            return VariableStore.APPOINTMENT_TIME.A;
        } else {
            return VariableStore.APPOINTMENT_TIME.E;
        }
    }

    public static String detectAppointmentDate(String appointmentID) {
        return appointmentID.substring(4, 6) + "/" + appointmentID.substring(6, 8) + "/20" + appointmentID.substring(8, 10);
    }


    public VariableStore.APPOINTMENT_TYPE getAppointmentType() {
        return appointmentType;
    }


    public void setAppointmentType(VariableStore.APPOINTMENT_TYPE appointmentType) {
        this.appointmentType = appointmentType;
    }

    public String getAppointmentID() {
        return appointmentID;
    }

    public void setAppointmentID(String appointmentID) {
        this.appointmentID = appointmentID;
    }

    public List<String> getPatientIDs() {
        return patients;
    }

    public int getCapacityOfAppointments() {
        return capacityOfAppointments;
    }

    public void setCapacityOfAppointments(int capacityOfAppointments) {
        this.capacityOfAppointments = capacityOfAppointments;
    }


    public VariableStore.APPOINTMENT_RESULT addPatientID(String registeredClientID) {
        if (patients.contains(registeredClientID)) {
            return VariableStore.APPOINTMENT_RESULT.Fail;
        } else {
            patients.add(registeredClientID);
            return VariableStore.APPOINTMENT_RESULT.Success;
        }
    }

    public boolean removePatientID(String registeredClientID) {
        return patients.remove(registeredClientID);
    }

    public boolean isFull() {
        return getCapacityOfAppointments() == patients.size();
    }

    public int getRemainAppointmentCapacity() {
        return capacityOfAppointments - patients.size();
    }
}
