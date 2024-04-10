package client;

import Model.AppointmentModel;
import controller.frontendController.webServiceInterface;
import log.Log;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;
import java.util.Scanner;


public class Client {

    static Scanner input;
    public static Service frontendService;
    private static webServiceInterface obj;
    public static final int PATIENT_BOOK_APPOINTMENT = 1;
    public static final int PATIENT_GET_BOOKING_SCHEDULE = 2;
    public static final int PATIENT_CANCEL_APPOINTMENT = 3;
    public static final int PATIENT_SWAP_APPOINTMENT = 4;
    public static final int PATIENT_LOGOUT = 5;

    public static final int ADMIN_ADD_APPOINTMENT =1 ;
    public static final int ADMIN_REMOVE_APPOINTMENT = 2;
    public static final int ADMIN_LIST_APPOINTMENT_AVAILABILITY = 3;
    public static final int ADMIN_BOOK_APPOINTMENT = 8;
    public static final int ADMIN_GET_BOOKING_SCHEDULE = 5;
    public static final int ADMIN_CANCEL_APPOINTMENT = 6;
    public static final int ADMIN_SWAP_APPOINTMENT = 7;
    public static final int ADMIN_LOGOUT = 4;

    public static final int SERVER_ATWATER = 2964;
    public static final int SERVER_VERDUN = 2965;
    public static final int SERVER_OUTRAMONT = 2966;
    public static final String DHMS_NAME = "HEALTH CARE MANAGEMENT SYSTEM";

    public static final int USER_TYPE_PATIENT = 1;
    public static final int USER_TYPE_ADMIN = 2;


    public static void main(String[] args) throws Exception {
        URL frontendURL = new URL("http://localhost:8080/Frontend?wsdl");
        QName frontendQName = new QName("http://implementation.frontendController.controller/", "FrontendImplementationService");
        frontendService = Service.create(frontendURL, frontendQName);
        init();
    }

    public static void init() throws Exception {
        input = new Scanner(System.in);
        String userID;
        System.out.println("********** Health Care Management Systems **********");
        System.out.println("Please Enter ID:(e.g. Admin :  MTLA2345 or Patient : MTLP2345) ");
        userID = input.next().trim().toUpperCase();
        Log.userLog(userID, " login attempt");

            if ( checkUserType(userID) == USER_TYPE_PATIENT) {
                try {
                    System.out.println("Successfully logged in as Patient (" + userID + ")");
                    Log.userLog(userID, " Patient Login successful");
                    Patient(userID);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (checkUserType(userID) == USER_TYPE_ADMIN) {
                try {
                    System.out.println("Successfully logged in as Admin (" + userID + ")");
                    admin(userID);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Invalid userID format !! ( e.g. MTLP2345 or MTLA2345 )");
                init();
            }
    }
    public static int checkUserType(String userID) {
        if (userID.length() == 8 &&
                (userID.startsWith("MTL") || userID.startsWith("SHE") || userID.startsWith("QUE")) &&
                (userID.charAt(3) == 'P' || userID.charAt(3) == 'A')) {
            return userID.charAt(3) == 'P' ? USER_TYPE_PATIENT :USER_TYPE_ADMIN;
        }
        return 0;
    }

    public static int promptForCapacity(Scanner input) {
        System.out.println("---------------------------");
        System.out.println("Please enter the booking capacity:");
        return input.nextInt();
    }
    private static String getServerID(String userID) {
        obj = frontendService.getPort(webServiceInterface.class);
        return userID;
    }

    private static void Patient(String PatientID) throws Exception {
        String serverID = getServerID(PatientID);
        if (serverID.equals("1")) {
            init();
        }
        boolean repeat = true;
        printMenu(USER_TYPE_PATIENT);
        int menuSelection = input.nextInt();
        String appointmentType;
        String appointmentID;
        String serverResponse;
        switch (menuSelection) {
            case PATIENT_BOOK_APPOINTMENT:
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
                Log.userLog(PatientID, " attempting to bookAppointment");
                serverResponse = obj.bookAppointment(PatientID, appointmentID, appointmentType);
                System.out.println(serverResponse);
                Log.userLog(PatientID, " bookAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case PATIENT_GET_BOOKING_SCHEDULE:
                Log.userLog(PatientID, " attempting to getBookingSchedule");
                serverResponse = obj.getBookingSchedule(PatientID);
                System.out.println(serverResponse);
                Log.userLog(PatientID, " bookAppointment", " null ", serverResponse);
                break;
            case PATIENT_CANCEL_APPOINTMENT:
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
                Log.userLog(PatientID, " attempting to cancelAppointment");
                serverResponse = obj.cancelAppointment(PatientID, appointmentID);
                System.out.println(serverResponse);
                Log.userLog(PatientID, " bookAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case PATIENT_SWAP_APPOINTMENT:
                System.out.println("Please Enter the OLD Appointment to be replaced");
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
                System.out.println("Please Enter the NEW Appointment to be replaced");
                String newAppointmentType = promptForAppointmentType();
                String newAppointmentID = promptForAppointmentID();
                Log.userLog(PatientID, " attempting to swapAppointment");
                serverResponse = obj.swapAppointment(PatientID, newAppointmentID, newAppointmentType, appointmentID, appointmentType);
                System.out.println(serverResponse);
                Log.userLog(PatientID, " swapAppointment", " oldAppointmentID: " + appointmentID + " oldAppointmentType: " + appointmentType + " newAppointmentID: " + newAppointmentID + " newAppointmentType: " + newAppointmentType + " ", serverResponse);
                break;
            case PATIENT_LOGOUT:
                repeat = false;
                Log.userLog(PatientID, " attempting to Logout");
                init();
                break;
        }
        if (repeat) {
            Patient(PatientID);
        }
    }

    private static void admin(String adminAppointmentID) throws Exception {
        String serverID = getServerID(adminAppointmentID);
        if (serverID.equals("1")) {
            init();
        }
        boolean repeat = true;
        printMenu(USER_TYPE_ADMIN);
        String PatientID;
        String appointmentType;
        String appointmentID;
        String serverResponse;
        int capacity;
        int menuSelection = input.nextInt();
        switch (menuSelection) {
            case ADMIN_ADD_APPOINTMENT:
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
                capacity = promptForCapacity(input);
                Log.userLog(adminAppointmentID, " attempting to addAppointment");
                serverResponse = obj.addAppointment(serverID,appointmentID, appointmentType, capacity);
                System.out.println(serverResponse);
                Log.userLog(adminAppointmentID, " addAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " appointmentCapacity: " + capacity + " ", serverResponse);
                break;
            case ADMIN_REMOVE_APPOINTMENT:
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
                Log.userLog(adminAppointmentID, " attempting to removeAppointment");
                serverResponse = obj.removeAppointment(serverID,appointmentID, appointmentType);
                System.out.println(serverResponse);
                Log.userLog(adminAppointmentID, " removeAppointment", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_LIST_APPOINTMENT_AVAILABILITY:
                appointmentType = promptForAppointmentType();
                Log.userLog(adminAppointmentID, " attempting to listAppointmentAvailability");
                serverResponse = obj.listAppointmentAvailability(serverID,appointmentType);
                System.out.println(serverResponse);
                Log.userLog(adminAppointmentID, " listAppointmentAvailability", " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_BOOK_APPOINTMENT:
                PatientID = askForPatientIDFromManager(adminAppointmentID.substring(0, 3));
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
                Log.userLog(adminAppointmentID, " attempting to bookAppointment");
                serverResponse = obj.bookAppointment(PatientID, appointmentID, appointmentType);
                System.out.println(serverResponse);
                Log.userLog(adminAppointmentID, " bookAppointment", " PatientID: " + PatientID + " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_GET_BOOKING_SCHEDULE:
                PatientID = askForPatientIDFromManager(adminAppointmentID.substring(0, 3));
                Log.userLog(adminAppointmentID, " attempting to getBookingSchedule");
                serverResponse = obj.getBookingSchedule(PatientID);
                System.out.println(serverResponse);
                Log.userLog(adminAppointmentID, " getBookingSchedule", " PatientID: " + PatientID + " ", serverResponse);
                break;
            case ADMIN_CANCEL_APPOINTMENT:
                PatientID = askForPatientIDFromManager(adminAppointmentID.substring(0, 3));
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
                Log.userLog(adminAppointmentID, " attempting to cancelAppointment");
                serverResponse = obj.cancelAppointment(PatientID, appointmentID);
                System.out.println(serverResponse);
                Log.userLog(adminAppointmentID, " cancelAppointment", " PatientID: " + PatientID + " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", serverResponse);
                break;
            case ADMIN_SWAP_APPOINTMENT:
                PatientID = askForPatientIDFromManager(adminAppointmentID.substring(0, 3));
                System.out.println("Please Enter the OLD Appointment to be swapped");
                appointmentType = promptForAppointmentType();
                appointmentID = promptForAppointmentID();
                System.out.println("Please Enter the NEW Appointment to be swapped");
                String newAppointmentType = promptForAppointmentType();
                String newAppointmentID = promptForAppointmentID();
                Log.userLog(adminAppointmentID, " attempting to swapAppointment");
                serverResponse = obj.swapAppointment(PatientID, newAppointmentID, newAppointmentType, appointmentID, appointmentType);
                System.out.println(serverResponse);
                Log.userLog(adminAppointmentID, " swapAppointment", " PatientID: " + PatientID + " oldAppointmentID: " + appointmentID + " oldAppointmentType: " + appointmentType + " newAppointmentID: " + newAppointmentID + " newAppointmentType: " + newAppointmentType + " ", serverResponse);
                break;
            case ADMIN_LOGOUT:
                repeat = false;
                Log.userLog(adminAppointmentID, "attempting to Logout");
                init();
                break;
        }
        if (repeat) {
            admin(adminAppointmentID);
        }
    }

    private static String askForPatientIDFromManager(String branchAcronym) {
        System.out.println("Please enter a PatientID(Within " + branchAcronym + " Server):");
        String userID = input.next().trim().toUpperCase();
        if (checkUserType(userID) != USER_TYPE_PATIENT || !userID.substring(0, 3).equals(branchAcronym)) {
            return askForPatientIDFromManager(branchAcronym);
        } else {
            return userID;
        }
    }

    private static void printMenu(int userType) {
        System.out.println("---------------------------------");
        System.out.println("Please select one of the following options from the menu below:");
        if (userType == USER_TYPE_PATIENT) {
            System.out.println("1.Book Appointment");
            System.out.println("2.Get booking Schedule");
            System.out.println("3.Cancel Appointment");
            System.out.println("4.Swap Appointment");
            System.out.println("5.Logout");
        } else if (userType == USER_TYPE_ADMIN) {
            System.out.println("1.Add Appointment");
            System.out.println("2.Remove Appointment");
            System.out.println("3.List show Availability");
            System.out.println("4.Book a Appointment");
            System.out.println("5.Get Booking Schedule");
            System.out.println("6.Cancel Appointment");
            System.out.println("7.Swap Appointment");
            System.out.println("4.Logout");
        }
    }

    private static String promptForAppointmentType() {
        System.out.println("--------------------------------");
        System.out.println("Please choose an appointmentType below:");
        System.out.println("1.Physician");
        System.out.println("2.Surgeon");
        System.out.println("3.Dental");
        switch (input.nextInt()) {
            case 1:
                return AppointmentModel.TYPE_PHYSICIAN;
            case 2:
                return AppointmentModel.TYPE_SURGEON;
            case 3:
                return AppointmentModel.TYPE_DENTAL;
        }
        return promptForAppointmentType();
    }

    private static String promptForAppointmentID() {
        System.out.println("-----------------------");
        System.out.println("Please enter the appointmentID (e.g MTLM101024)");
        String appointmentID = input.next().trim().toUpperCase();
        if (appointmentID.length() == 10) {
            if (appointmentID.substring(0, 3).equalsIgnoreCase("MTL") ||
                    appointmentID.substring(0, 3).equalsIgnoreCase("SHE") ||
                    appointmentID.substring(0, 3).equalsIgnoreCase("QUE")) {
                if (appointmentID.substring(3, 4).equalsIgnoreCase("M") ||
                        appointmentID.substring(3, 4).equalsIgnoreCase("A") ||
                        appointmentID.substring(3, 4).equalsIgnoreCase("E")) {
                    return appointmentID;
                }
            }
        }
        return promptForAppointmentID();
    }
}