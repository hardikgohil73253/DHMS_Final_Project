package Model;

import java.util.List;
import java.util.ArrayList;



public class AppointmentModel {

    public static final String MORNING_APPOINTMENT = "Morning";
    public static final String AFTERNOON_APPOINTMENT = "Afternoon";
    public static final String EVENING_APPOINTMENT = "Evening";
    public static final String TYPE_PHYSICIAN = "PHYSICIAN", TYPE_SURGEON = "SURGEON", TYPE_DENTAL = "DENTAL";
    public static final String MONTREAL = "MONTREAL";
    public static final String QUEBEC = "QUEBEC";
    public static final String SHERBROOKE = "SHERBROOKE";

    public static final int APPOINTMENT_FULL = -1;
    public static final int ALREADY_BOOKED = 0;
    public static final int ADD_SUCCESS = 1;

    private String APPOINTMENT_TYPE;
    private String APPOINTMENT_ID;
    private int APPOINTMENT_CAPACITY;

    private String APPOINTMENT_TIME_SLOT;
    private String APPOINTMENT_SERVER;

    private String APPOINTMENT_DATE;

    private List<String> registeredClients;


    public AppointmentModel(String APPOINTMENT_TYPE , String APPOINTMENT_ID , int APPOINTMENT_CAPACITY){
        this.APPOINTMENT_ID = APPOINTMENT_ID;
        this.APPOINTMENT_TYPE = APPOINTMENT_TYPE;
        this.APPOINTMENT_CAPACITY = APPOINTMENT_CAPACITY;
        this.APPOINTMENT_TIME_SLOT = checkAPPOINTMENT_TIME_SLOT(APPOINTMENT_ID);
        this.APPOINTMENT_SERVER = checkAPPOINTMENT_SERVER(APPOINTMENT_ID);
        this.APPOINTMENT_DATE = checkAPPOINTMENT_DATE(APPOINTMENT_ID);
        registeredClients = new ArrayList<>();

    }

    public static String checkAPPOINTMENT_SERVER(String APPOINTMENT_ID){
        if (APPOINTMENT_ID.substring(0, 3).equalsIgnoreCase("MTL")) {
            return MONTREAL;
        } else if (APPOINTMENT_ID.substring(0, 3).equalsIgnoreCase("QUE")) {
            return QUEBEC;
        } else {
            return SHERBROOKE;
        }
    }

    public static String checkAPPOINTMENT_TIME_SLOT(String APPOINTMENT_ID){
        if (APPOINTMENT_ID.substring(3, 4).equalsIgnoreCase("M")) {
            return MORNING_APPOINTMENT;
        } else if (APPOINTMENT_ID.substring(3, 4).equalsIgnoreCase("A")) {
            return AFTERNOON_APPOINTMENT;
        } else {
            return EVENING_APPOINTMENT;
        }
    }

    public static String checkAPPOINTMENT_DATE(String APPOINTMENT_ID) {
        return APPOINTMENT_ID.substring(4, 6) + "/" + APPOINTMENT_ID.substring(6, 8) + "/20" + APPOINTMENT_ID.substring(8, 10);
    }

    public String getAPPOINTMENT_TYPE() {
        return APPOINTMENT_TYPE;
    }

    public void setAPPOINTMENT_TYPE(String APPOINTMENT_TYPE) {
        this.APPOINTMENT_TYPE = APPOINTMENT_TYPE;
    }

    public String getAPPOINTMENT_ID() {
        return APPOINTMENT_ID;
    }

    public void setAPPOINTMENT_ID(String APPOINTMENT_ID) {
        this.APPOINTMENT_ID = APPOINTMENT_ID;
    }

    public String getAPPOINTMENT_SERVER() {
        return APPOINTMENT_SERVER;
    }

    public void setAPPOINTMENT_SERVER() {
        this.APPOINTMENT_SERVER = APPOINTMENT_SERVER;
    }

    public int getAPPOINTMENT_CAPACITY() {
        return APPOINTMENT_CAPACITY;
    }

    public void setAPPOINTMENT_CAPACITY(int APPOINTMENT_CAPACITY) {
        this.APPOINTMENT_CAPACITY = APPOINTMENT_CAPACITY;
    }

    public int getAppointmentRemainCapacity() {
        return APPOINTMENT_CAPACITY - registeredClients.size();
    }

    public String getAPPOINTMENT_DATE() {
        return APPOINTMENT_DATE;
    }

    public void setAPPOINTMENT_DATE(String APPOINTMENT_DATE) {
        this.APPOINTMENT_DATE = APPOINTMENT_DATE;
    }

    public String getAPPOINTMENT_TIME_SLOT() {
        return APPOINTMENT_TIME_SLOT;
    }

    public void setAPPOINTMENT_TIME_SLOT(String APPOINTMENT_TIME_SLOT) {
        this.APPOINTMENT_TIME_SLOT = APPOINTMENT_TIME_SLOT;
    }

    public boolean isFull() {
        return getAPPOINTMENT_CAPACITY() == registeredClients.size();
    }

    public List<String> getRegisteredClientIDs() {
        return registeredClients;
    }

    public void setRegisteredClientsIDs(List<String> registeredClientsIDs) {
        this.registeredClients = registeredClientsIDs;
    }

    public int addRegisteredClientID(String registeredClientID) {
        if (!isFull()) {
            if (registeredClients.contains(registeredClientID)) {
                return ALREADY_BOOKED;
            } else {
                registeredClients.add(registeredClientID);
                return ADD_SUCCESS;
            }
        } else {
            return APPOINTMENT_FULL;
        }
    }

    public boolean removeRegisteredClientID(String registeredClientID) {
        return registeredClients.remove(registeredClientID);
    }

    @Override
    public String toString() {
        return " (" + getAPPOINTMENT_ID() + ") in the " + getAPPOINTMENT_TIME_SLOT() + " of " + getAPPOINTMENT_DATE() + " Total[Remaining] Capacity: " + getAPPOINTMENT_CAPACITY() + "[" + getAppointmentRemainCapacity() + "]";
    }

}