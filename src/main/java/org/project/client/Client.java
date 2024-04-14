package org.project.client;

import org.project.interfaces.WebServiceInterface;
import org.project.replica1.models.ClientModel;
import org.project.Logs.Logger;
import org.project.utils.VariableStore;
import org.project.utils.VariableStore.APPOINTMENT_TIME;
import org.project.utils.VariableStore.APPOINTMENT_TYPE;
import org.project.utils.VariableStore.ROLES;
import org.project.utils.VariableStore.SERVERS;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;


public class Client {


    public static Service webService;
    private static Scanner user_Input;
    private static WebServiceInterface webServiceInterface;

    public static void main(String[] args) {
        new Client();
    }

    public Client() {
        ClientOperation();
    }

    private static void ClientOperation() {
        try {
            String url = "http://"+ "localhost" +":"+ 4555 +"/FrontEnd?wsdl";
            URL montrealURL = new URL(url);
            QName montrealQName = new QName("http://front_end.project.org/", "FrontEndImplementationService");
            webService = Service.create(montrealURL, montrealQName);

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        user_Input = new Scanner(System.in);
        String user_Id;
        System.out.println("============== Distributed Health Care Management System ==============");
        System.out.println("Please enter ID :");
        user_Id = user_Input.next().toUpperCase().trim();
        boolean isValidID = checkUserID(user_Id);
        if (!isValidID) {
            System.out.println("Invalid id found.");
            ClientOperation();
        }
        ClientModel clientModel = new ClientModel(user_Id);

        switch (clientModel.getClientType()) {
            case P:
                try {
                    System.out.printf("Successfully logged in as Patient with %s \n", user_Id);
                    patient(clientModel);
                } catch (Exception e) {
                    System.out.println("Error occur! At line 63 Client.java");
                }
                break;
            case A:
                try {
                    System.out.printf("Successfully logged in as Admin with %s \n", user_Id);
                    admin(clientModel);
                } catch (Exception e) {
                    System.out.println("Error occur! At line 71 Client.java");
                }
                break;
            default:
                if ((user_Id.isBlank() || user_Id.isEmpty())) {
                    System.out.println("Please enter a valid userId.");
                } else {
                    System.out.printf("%sPlease try again with correct user_Id.\n", user_Id);
                }
                Logger.deleteLogFile(user_Id);
                ClientOperation();
        }
    }

    private static void patient(ClientModel clientModel) {
        init();
        getMenuOption(ROLES.P);
        int operation = getData();
        String SERVER_RESPONSE = "";
        String APPOINTMENT_TYPE = "";
        String APPOINTMENT_ID = "";
        String OLD_APPOINTMENT_TYPE = "";
        String OLD_APPOINTMENT_ID = "";
        try {
            Logger.log(VariableStore.LOG_TYPE_CLIENT, clientModel.getClientID(), " logged in successfully.");

            switch (operation) {
                case 1 -> {
                    APPOINTMENT_TYPE = appointmentType();
                    APPOINTMENT_ID = appointmentId();
                    userLoggerRequest(clientModel.getClientID(), "Book appointment", APPOINTMENT_TYPE + " " + APPOINTMENT_ID);
                    SERVER_RESPONSE = webServiceInterface.bookAppointment(clientModel.getClientID(), clientModel.getClientID(), APPOINTMENT_ID, APPOINTMENT_TYPE);
                    userLoggerResponse(clientModel.getClientID(), "Book appointment", SERVER_RESPONSE);
                    System.out.println(SERVER_RESPONSE);
                }
                case 2 -> {
                    userLoggerRequest(clientModel.getClientID(), "Appointment schedules", clientModel.getClientID());
                    SERVER_RESPONSE = webServiceInterface.getAppointmentSchedule(clientModel.getClientID(), clientModel.getClientID());
                    userLoggerResponse(clientModel.getClientID(), "Appointment schedules", SERVER_RESPONSE);
                    System.out.println(SERVER_RESPONSE);
                }
                case 3 -> {
                    APPOINTMENT_ID = appointmentId();
                    userLoggerRequest(clientModel.getClientID(), "Cancel appointment", APPOINTMENT_ID);
                    SERVER_RESPONSE = webServiceInterface.cancelAppointment(clientModel.getClientID(), clientModel.getClientID(), APPOINTMENT_ID);
                    userLoggerResponse(clientModel.getClientID(), "Cancel appointment", SERVER_RESPONSE);
                    System.out.println(SERVER_RESPONSE);
                }
                case 4 -> {
                    System.out.println("Please type the old appointment:");
                    OLD_APPOINTMENT_TYPE = appointmentType();
                    OLD_APPOINTMENT_ID = appointmentId();
                    System.out.println("Please type the new appointment:");
                    APPOINTMENT_TYPE = appointmentType();
                    APPOINTMENT_ID = appointmentId();
                    userLoggerRequest(clientModel.getClientID(), "Swap appointment",OLD_APPOINTMENT_TYPE + " " + OLD_APPOINTMENT_ID + " " + APPOINTMENT_TYPE + " " + APPOINTMENT_ID + " " + clientModel.getClientID());
                    SERVER_RESPONSE = webServiceInterface.swapAppointment(clientModel.getClientID(), clientModel.getClientID(), OLD_APPOINTMENT_ID, OLD_APPOINTMENT_TYPE, APPOINTMENT_ID, APPOINTMENT_TYPE);
                    userLoggerResponse(clientModel.getClientID(), "Swap appointment", SERVER_RESPONSE);
                    System.out.println(SERVER_RESPONSE);
                }
                default -> {
                    System.out.println("False operation added.");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void admin(ClientModel clientModel) {
        init();
        getMenuOption(ROLES.A);
        int operation = getData();
        String Server_RESPONSE;
        String APPOINTMENT_TYPE;
        String APPOINTMENT_ID;
        String OLD_APPOINTMENT_tYPE = "";
        String OLD_APPOINTMENT_ID = "";
        int capacity;
        try {
            Logger.log(VariableStore.LOG_TYPE_CLIENT, clientModel.getClientID(), "logged in successfully.");

            switch (operation) {
                case 1 -> {
                    APPOINTMENT_TYPE = appointmentType();
                    APPOINTMENT_ID = appointmentId();
                    capacity = getBookingCapacity();
                    userLoggerRequest(clientModel.getClientID(), "Add appointment", APPOINTMENT_TYPE + " " + APPOINTMENT_ID + " " + capacity);
                    Server_RESPONSE = webServiceInterface.addAppointment(clientModel.getClientID(), APPOINTMENT_ID, APPOINTMENT_TYPE, capacity);
                    userLoggerResponse(clientModel.getClientID(), "Add appointment", Server_RESPONSE);
                    System.out.println(Server_RESPONSE);
                }
                case 2 -> {
                    APPOINTMENT_TYPE = appointmentType();
                    userLoggerRequest(clientModel.getClientID(), "List appointment", APPOINTMENT_TYPE);
                    Server_RESPONSE = webServiceInterface.listAppointmentAvailability(clientModel.getClientID(), APPOINTMENT_TYPE);
                    userLoggerResponse(clientModel.getClientID(), "List appointments", Server_RESPONSE);
                    System.out.println(Server_RESPONSE);
                }
                case 3 -> {
                    APPOINTMENT_TYPE = appointmentType();
                    APPOINTMENT_ID = appointmentId();
                    userLoggerRequest(clientModel.getClientID(), "Remove appointment", APPOINTMENT_TYPE + " " + APPOINTMENT_ID);
                    Server_RESPONSE = webServiceInterface.removeAppointment(clientModel.getClientID(), APPOINTMENT_ID, APPOINTMENT_TYPE);
                    userLoggerResponse(clientModel.getClientID(), "Remove appointment", Server_RESPONSE);
                    System.out.println(Server_RESPONSE);
                }
                case 4 -> {
                    System.out.println("Please enter PatientId to continue :");
                    String userId = user_Input.next().toUpperCase().trim();
                    boolean isIdValid = checkUserID(userId);
                    boolean isSameServer = isBothUserINSameServer(userId, clientModel.getClientID());
                    if (isIdValid && isSameServer) {
                        APPOINTMENT_TYPE = appointmentType();
                        APPOINTMENT_ID = appointmentId();
                        userLoggerRequest(clientModel.getClientID(), "Book appointment", APPOINTMENT_TYPE + " " + APPOINTMENT_ID + " " + userId);
                        Server_RESPONSE = webServiceInterface.bookAppointment(clientModel.getClientID(), userId, APPOINTMENT_ID, APPOINTMENT_TYPE);
                        userLoggerResponse(clientModel.getClientID(), "Book appointment", Server_RESPONSE);
                        System.out.println(Server_RESPONSE);
                    } else {
                        if (!isSameServer)
                            System.out.println("Only perform action of your own city's patient!");

                        System.out.println("Invalid userId.");
                    }
                }
                case 5 -> {
                    System.out.println("Please enter PatientId to continue :");
                    String userId = user_Input.next().toUpperCase().trim();
                    boolean isIdValid = checkUserID(userId);
                    boolean isSameServer = isBothUserINSameServer(userId, clientModel.getClientID());

                    if (isIdValid && isSameServer) {
                        userLoggerRequest(clientModel.getClientID(), "Get schedule of appointment", userId);
                        Server_RESPONSE = webServiceInterface.getAppointmentSchedule(clientModel.getClientID(), userId);
                        userLoggerResponse(clientModel.getClientID(), "Get schedule of appointment", Server_RESPONSE);
                        System.out.println(Server_RESPONSE);
                    } else {
                        if (!isSameServer)
                            System.out.println("Only perform action of your own city's patient!");

                        System.out.println("Invalid user-id.");
                    }
                }
                case 6 -> {
                    System.out.println("Please enter Patientid to continue :");
                    String userId = user_Input.next().toUpperCase().trim();
                    boolean isIdValid = checkUserID(userId);
                    boolean isSameServer = isBothUserINSameServer(userId, clientModel.getClientID());

                    if (isIdValid && isSameServer) {
                        APPOINTMENT_ID = appointmentId();
                        userLoggerRequest(clientModel.getClientID(), "Cancel appointment", APPOINTMENT_ID + " " + userId);
                        Server_RESPONSE = webServiceInterface.cancelAppointment(clientModel.getClientID(), userId, APPOINTMENT_ID);
                        userLoggerResponse(clientModel.getClientID(), "Cancel appointment", Server_RESPONSE);
                        System.out.println(Server_RESPONSE);
                    } else {
                        if (!isSameServer)
                            System.out.println("Only perform action of your own city's patient! -----------");
                        System.out.println("Invalid user-id.");
                    }
                }
                case 7 -> {
                    System.out.println("Please enter Patientid to continue :");
                    String userId = user_Input.next().toUpperCase().trim();
                    boolean isIdValid = checkUserID(userId);
                    boolean isSameServer = isBothUserINSameServer(userId, clientModel.getClientID());

                    if (isIdValid && isSameServer) {
                        System.out.println("Please type the existing appointment details::");
                        OLD_APPOINTMENT_tYPE = appointmentType();
                        OLD_APPOINTMENT_ID = appointmentId();
                        System.out.println("Please type the new appointment details::");
                        APPOINTMENT_TYPE = appointmentType();
                        APPOINTMENT_ID = appointmentId();
                        userLoggerRequest(clientModel.getClientID(), "Swap appointment",OLD_APPOINTMENT_tYPE + " " + OLD_APPOINTMENT_ID + " " + APPOINTMENT_TYPE + " " + APPOINTMENT_ID + " " + clientModel.getClientID());
                        Server_RESPONSE = webServiceInterface.swapAppointment(clientModel.getClientID(), clientModel.getClientID(), OLD_APPOINTMENT_ID, OLD_APPOINTMENT_tYPE, APPOINTMENT_ID, APPOINTMENT_TYPE);
                        userLoggerResponse(clientModel.getClientID(), "Swap appointment", Server_RESPONSE);
                        System.out.println(Server_RESPONSE);
                    } else {
                        if (!isSameServer)
                            System.out.println("Only perform action of your own city's patient! ---------");
                        System.out.println("Invalid userId.");
                    }
                }
                default -> {
                    System.out.println("Not valid option");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static String appointmentId() {
        System.out.println("Please enter your appointmentID");
        String appointmentID = user_Input.next().toUpperCase().trim();
        if (appointmentID.length() == 10) {
            String server = appointmentID.substring(0, 3);
            String appointmentTime = appointmentID.substring(3, 4);
            if (server.equalsIgnoreCase(SERVERS.MTL.toString()) ||
                    server.equalsIgnoreCase(SERVERS.QUE.toString()) ||
                    server.equalsIgnoreCase(SERVERS.SHE.toString())) {
                if (appointmentTime.equalsIgnoreCase(APPOINTMENT_TIME.M.toString()) ||
                        appointmentTime.equalsIgnoreCase(APPOINTMENT_TIME.A.toString()) ||
                        appointmentTime.equalsIgnoreCase(APPOINTMENT_TIME.E.toString())) {
                    if (appointmentID.substring(4, 9).matches("\\d+")) {
                        if (CheckValidDate(appointmentID)) {
                            return appointmentID;
                        } else {
                            System.out.println("Appointment Id is not valid as date mentioned in ID is false.");
                        }
                    }
                }
            }
            System.out.println("Appointment Id is not valid (e.g. of appointment ID is MTLE101023).");
        } else {
            System.out.println("Appointment Id is not valid (e.g. of appointment ID is MTLE101023).");
        }

        return appointmentId();
    }

    private static String appointmentType() {
        System.out.println("Please choose your Appointment Type");
        System.out.printf("\n1.%s\n2.%s\n3.%s\n", APPOINTMENT_TYPE.PHYSICIAN, APPOINTMENT_TYPE.SURGEON, APPOINTMENT_TYPE.DENTAL);

        return switch (getData()) {
            case 1 -> APPOINTMENT_TYPE.PHYSICIAN.toString();
            case 2 -> APPOINTMENT_TYPE.SURGEON.toString();
            case 3 -> APPOINTMENT_TYPE.DENTAL.toString();
            default -> {
                System.out.println("Invalid Appointment type.");
                yield appointmentType();
            }
        };
    }

    private static int getBookingCapacity() {
        System.out.println("Please enter capacity.");
        return getData();
    }

    private static void getMenuOption(ROLES role) {
        System.out.println("Please choose an option :");
        if (role == ROLES.P) {
            System.out.println("[Option 1]. Book an appointment");
            System.out.println("[Option 2]. Get appointment schedule");
            System.out.println("[Option 3]. Cancel the appointment");
            System.out.println("[Option 4]. Swap the appointment");
            System.out.println("Please Enter Option number :");
        } else if (role == ROLES.A) {
            System.out.println("[Option 1]. Add an appointment ");
            System.out.println("[Option 2]. List appointment ");
            System.out.println("[Option 3]. Remove appointment ");
            System.out.println("[Option 4]. Book an appointment");
            System.out.println("[Option 5]. Get appointment schedule");
            System.out.println("[Option 6]. Cancel the appointment");
            System.out.println("[Option 7]. Swap appointment");
            System.out.println("Please Enter Option number :");
        }
    }

    private static boolean CheckValidDate(String appointmentID) {
        String month_Str = appointmentID.substring(6, 8);
        String day_Str = appointmentID.substring(4, 6);
        String year_Str = appointmentID.substring(8, 10);
        int month = Integer.parseInt(month_Str);
        int day = Integer.parseInt(day_Str);
        int year = Integer.parseInt(year_Str);

        if (month < 1 || month > 12 || day < 1 || day > 31 || year < 0) {
            return false;
        }

        if (month == 2) { // February
            if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
                return day <= 29;
            } else {
                return day <= 28;
            }
        } else if (month == 4 || month == 6 || month == 9 || month == 11) {
            return day <= 30;
        } else {
            return true;
        }
    }

    private static boolean checkUserID(String userID
    ) {
        if (userID.length() == 8) {
            String server = userID.substring(0, 3);
            String userType = userID.substring(3, 4);
            String digitID = userID.substring(4, 8);

            if (server.equalsIgnoreCase(SERVERS.MTL.toString()) ||
                    server.equalsIgnoreCase(SERVERS.QUE.toString()) ||
                    server.equalsIgnoreCase(SERVERS.SHE.toString())) {
                if (userType.equals(ROLES.A.toString()) || userType.equals(ROLES.P.toString())) {
                    if (digitID.matches("\\d+")) {
                        return true;
                    }
                }
            }
            System.out.println("User Id is not valid!.");
        }
        return false;
    }

    private static void userLoggerResponse(String clientId, String method, String message) {
        String clientType = clientId.contains("A") ? "Admin" : "Patient";
        try {
            Logger.log(VariableStore.LOG_TYPE_CLIENT, clientId, " " + clientType + " " + clientId + " requested >> " + method + " || Response received from server is >> " +
                    message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void userLoggerRequest(String clientId, String method, String params) {
        String clientType = clientId.contains("A") ? "Admin" : "Patient";
        try {
            Logger.log(VariableStore.LOG_TYPE_CLIENT, clientId, " " + clientType + " " + clientId + " requested >> " + method + " || The parameters are >> " +
                    params);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isBothUserINSameServer(String patientId, String adminId) {
        return patientId.substring(0, 3).equals(adminId.substring(0, 3));

    }

    private static int getData() {
        try {
            return user_Input.nextInt();
        } catch (Exception e) {
            System.out.println("Found Character instead of Number.");
            return 0;
        }
    }

    private static void init() {
        webServiceInterface = webService.getPort(WebServiceInterface.class);
    }
}
