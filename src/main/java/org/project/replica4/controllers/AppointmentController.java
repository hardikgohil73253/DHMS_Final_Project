package org.project.replica4.controllers;

import org.project.replica4.interfaces.WebServiceInterface;
import org.project.replica4.models.ClientModel;
import org.project.replica4.models.AppointmentModel;
import org.project.replica4.Logger;
import org.project.utils.VariableStore;
import org.project.utils.VariableStore.APPOINTMENT_TYPE;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@WebService(endpointInterface = "org.project.replica4.interfaces.WebServiceInterface")

@SOAPBinding(style = SOAPBinding.Style.RPC)
public class AppointmentController implements WebServiceInterface {

    private String currentServerID = "";

    private ConcurrentHashMap<String, Map<String, AppointmentModel>> hashAllAppointment;

    private ConcurrentHashMap<String, Map<String, List<String>>> hashUserAppointment;

    private ConcurrentHashMap<String, ClientModel> hashServerUsers;

    private String serverResponse;

    public AppointmentController(String currentServerID) {
        super();
        this.currentServerID = currentServerID;
        serverResponse = "";
        assignConcurrentHashMaps();
        initAppointmentTypes();

    }

    public AppointmentController() {

    }

    private void assignConcurrentHashMaps() {
        hashAllAppointment = new ConcurrentHashMap<>();
        hashUserAppointment = new ConcurrentHashMap<>();
        hashServerUsers = new ConcurrentHashMap<>();
    }

    private void initAppointmentTypes() {
        hashAllAppointment.put(APPOINTMENT_TYPE.PHYSICIAN.toString(), new ConcurrentHashMap<>());
        hashAllAppointment.put(APPOINTMENT_TYPE.SURGEON.toString(), new ConcurrentHashMap<>());
        hashAllAppointment.put(APPOINTMENT_TYPE.DENTAL.toString(), new ConcurrentHashMap<>());
    }

    @Override
    public String addAppointment(String appointmentID, String appointmentType, int capacity) {
        if (hashAllAppointment.get(appointmentType).containsKey(appointmentID)) {
            if (hashAllAppointment.get(appointmentType).get(appointmentID).getCapacityOfAppointments() <= capacity) {
                hashAllAppointment.get(appointmentType).get(appointmentID).setCapacityOfAppointments(capacity);
                serverResponse = VariableStore.APPOINTMENT_RESULT.Success + " : Appointment " + appointmentID
                        + " capacity has been increased to " + capacity;
            } else {
                serverResponse = VariableStore.APPOINTMENT_RESULT.Exist_Already + " : Appointment slot is already there.";
            }
            return serverResponse;
        }
        if (AppointmentModel.detectAppointmentServer(appointmentID).toString().equals(currentServerID)) {
            AppointmentModel appointmentModel = new AppointmentModel(APPOINTMENT_TYPE.valueOf(appointmentType),
                    appointmentID, capacity);
            Map<String, AppointmentModel> appointmentModelMap = hashAllAppointment.get(appointmentType);
            appointmentModelMap.put(appointmentID, appointmentModel);
            hashAllAppointment.put(appointmentType, appointmentModelMap);
            serverResponse = VariableStore.APPOINTMENT_RESULT.Success + " : Appointment " + appointmentID
                    + " added successfully in the server " + currentServerID;
        } else {
            serverResponse = VariableStore.APPOINTMENT_RESULT.Fail
                    + " : You can't add Appointment to any servers other than " + currentServerID;
        }
        severLogger(serverResponse, "Add appointment");
        return serverResponse;
    }

    @Override
    public  String removeAppointment(String appointmentID, String appointmentType) {

        if (AppointmentModel.detectAppointmentServer(appointmentID).toString().equals(currentServerID)) {
            if (hashAllAppointment.get(appointmentType).containsKey(appointmentID)) {
                List<String> registeredClients = hashAllAppointment.get(appointmentType).get(appointmentID)
                        .getPatientIDs();
                hashAllAppointment.get(appointmentType).remove(appointmentID);

                transferAppointment(appointmentID, appointmentType, registeredClients);

                serverResponse = VariableStore.APPOINTMENT_RESULT.Success + " : Appointment has been removed from "
                        + currentServerID;
            } else {
                serverResponse = VariableStore.APPOINTMENT_RESULT.Not_Found + " : Appointment " + appointmentID
                        + " couldn't be found in " + currentServerID;
            }
        } else {
            serverResponse = VariableStore.APPOINTMENT_RESULT.Fail + " : Cannot Remove Appointment from servers other than "
                    + currentServerID;
        }
        severLogger(serverResponse, "Remove appointment");
        return serverResponse;
    }

    @Override
    public  String listAppointmentAvailability(String appointmentType) {

        Map<String, AppointmentModel> appointments = hashAllAppointment.get(appointmentType);
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(currentServerID).append(" Server ").append(appointmentType).append(":\n");

        if (appointments.isEmpty()) {
            messageBuilder.append("No Appointment of type ").append(appointmentType);
        } else {
            for (AppointmentModel appointment : appointments.values()) {
                messageBuilder.append(appointment.getAppointmentID()).append(" ")
                        .append(appointment.getRemainAppointmentCapacity()).append(" ");
            }
            messageBuilder.append("\n");
        }
        String firstServer, secondServer;
        if (currentServerID.equals(VariableStore.SERVERS.MTL.toString())) {
            firstServer = broadcastMessageToOtherServers(VariableStore.QUEBEC_SOCKET_PORT4, "listAppointmentAvailability",
                    "null", appointmentType, "null");
            secondServer = broadcastMessageToOtherServers(VariableStore.SHERBROOKE_SOCKET_PORT4, "listAppointmentAvailability",
                    "null", appointmentType, "null");
        } else if (currentServerID.equals(VariableStore.SERVERS.SHE.toString())) {
            firstServer = broadcastMessageToOtherServers(VariableStore.QUEBEC_SOCKET_PORT4, "listAppointmentAvailability",
                    "null", appointmentType, "null");
            secondServer = broadcastMessageToOtherServers(VariableStore.MONTREAL_SOCKET_PORT4, "listAppointmentAvailability",
                    "null", appointmentType, "null");
        } else {
            firstServer = broadcastMessageToOtherServers(VariableStore.MONTREAL_SOCKET_PORT4, "listAppointmentAvailability",
                    "null", appointmentType, "null");
            secondServer = broadcastMessageToOtherServers(VariableStore.SHERBROOKE_SOCKET_PORT4, "listAppointmentAvailability",
                    "null", appointmentType, "null");
        }
        messageBuilder.append(firstServer).append(secondServer);
        serverResponse = messageBuilder.toString();
        severLogger(serverResponse, "List availability of appointment");
        return serverResponse;
    }

    @Override
    public  String bookAppointment(String patientID, String appointmentID, String appointmentType) {
        if (!hashServerUsers.containsKey(patientID)) {
            addNewUsersToClients(patientID);
        }
        if (AppointmentModel.detectAppointmentServer(appointmentID).toString().equals(currentServerID)) {
            return bookAppointmentForSameCity(patientID, appointmentID, appointmentType);
        } else {
            return bookAppointmentForOtherCities(patientID, appointmentID, appointmentType);
        }
    }

    private String bookAppointmentForSameCity(String patientID, String appointmentID,
                                                           String appointmentType) {

        List<String> appointmentsWithSameId = getAppointmentTypesForPatient(appointmentID, patientID, appointmentType);
        if (!appointmentsWithSameId.isEmpty()) {
            serverResponse = VariableStore.APPOINTMENT_RESULT.Fail + " : Appointment " + appointmentID
                    + " is already booked with same appointment ID or on same day there is an another appointment.";
            return serverResponse;
        }
        if (hashAllAppointment.get(appointmentType).containsKey(appointmentID)) {
            AppointmentModel bookedAppointment = hashAllAppointment.get(appointmentType).get(appointmentID);
            if (!bookedAppointment.isFull()) {
                if (hashUserAppointment.containsKey(patientID)) {
                    if (hashUserAppointment.get(patientID).containsKey(appointmentType)) {
                        if (!hashUserAppointment.get(patientID).get(appointmentType).contains(appointmentID)) {
                            hashUserAppointment.get(patientID).get(appointmentType).add(appointmentID);
                        } else {
                            serverResponse = VariableStore.APPOINTMENT_RESULT.Fail + " : Appointment " + appointmentID
                                    + " Already Booked";
                            return serverResponse;
                        }
                    } else {
                        List<String> tempAppointments = new ArrayList<>();
                        tempAppointments.add(appointmentID);
                        hashUserAppointment.get(patientID).put(appointmentType, tempAppointments);
                    }
                } else {
                    Map<String, List<String>> appointmentList1 = new ConcurrentHashMap<>();
                    List<String> appointmentList2 = new ArrayList<>();
                    appointmentList2.add(appointmentID);
                    appointmentList1.put(appointmentType, appointmentList2);
                    hashUserAppointment.put(patientID, appointmentList1);
                }
                if (hashAllAppointment.get(appointmentType).get(appointmentID)
                        .addPatientID(patientID) == VariableStore.APPOINTMENT_RESULT.Success) {
                    serverResponse = VariableStore.APPOINTMENT_RESULT.Success + " : Appointment with " + appointmentID
                            + " booked successfully by " + patientID;
                } else if (hashAllAppointment.get(appointmentType).get(appointmentID)
                        .addPatientID(patientID) == VariableStore.APPOINTMENT_RESULT.Fail) {
                    serverResponse = VariableStore.APPOINTMENT_RESULT.Fail + " : Appointment " + appointmentID
                            + " is full right now.";
                } else {
                    serverResponse = VariableStore.APPOINTMENT_RESULT.Fail + " : System can't add you're appointment "
                            + appointmentID;
                }
                return serverResponse;
            } else {
                serverResponse = VariableStore.APPOINTMENT_RESULT.Fail + " : Appointment " + appointmentID + " is full";
                return serverResponse;
            }
        } else {
            serverResponse = VariableStore.APPOINTMENT_RESULT.Fail + " : Appointment " + appointmentID
                    + " is not added/unavailable.";
            severLogger(serverResponse, "Book appointment");
            return serverResponse;
        }
    }

    private String bookAppointmentForOtherCities(String patientID, String appointmentID,
                                                              String appointmentType) {

        if (!checkWeeklySchedule(patientID, appointmentID.substring(4))) {
            String broadcasterResponse = broadcastMessageToOtherServers(
                    VariableStore.getSocketPortsForReplica4(AppointmentModel.detectAppointmentServer(appointmentID)), "bookAppointment",
                    patientID, appointmentType, appointmentID);
            if (broadcasterResponse.startsWith(VariableStore.APPOINTMENT_RESULT.Success.toString())) {
                if (hashUserAppointment.get(patientID).containsKey(appointmentType)) {
                    hashUserAppointment.get(patientID).get(appointmentType).add(appointmentID);
                } else {
                    List<String> temp = new ArrayList<>();
                    temp.add(appointmentID);
                    hashUserAppointment.get(patientID).put(appointmentType, temp);
                }
            }
            severLogger(broadcasterResponse, "Book appointment");
            return broadcasterResponse;
        } else {
            serverResponse = VariableStore.APPOINTMENT_RESULT.Fail
                    + " : You can't book appointment in other cities for certain week since limit of appointments per week is 3.";
            severLogger(serverResponse, "Book appointment");
            return serverResponse;
        }
    }

    @Override
    public String getAppointmentSchedule(String patientID) {
        if (!hashServerUsers.containsKey(patientID)) {
            addNewUsersToClients(patientID);
            serverResponse = "Appointment schedule is empty for " + patientID;
            return serverResponse;
        }
        Map<String, List<String>> appointments = hashUserAppointment.get(patientID);
        if (appointments.isEmpty()) {
            serverResponse = "Appointment schedule Empty For " + patientID;
            return serverResponse;
        }
        StringBuilder builder = new StringBuilder();
        for (String appointmentType : appointments.keySet()) {
            builder.append("\n").append(appointmentType).append(":\n");
            for (String appointmentId : appointments.get(appointmentType)) {
                builder.append(appointmentId).append(" ");
            }
        }
        serverResponse = builder.toString();
        severLogger(serverResponse, "Get appointment schedule");
        return serverResponse;
    }

    @Override
    public String cancelAppointment(String patientID, String appointmentID) {
        List<String> appointmentTypes = fetchAppointmentType(patientID, appointmentID);
        if (AppointmentModel.detectAppointmentServer(appointmentID)
                .equals(VariableStore.SERVERS.valueOf(currentServerID))) {
            if (ClientModel.detectClientServer(patientID).toString().equals(currentServerID)) {
                if (!hashServerUsers.containsKey(patientID)) {
                    addNewUsersToClients(patientID);
                    serverResponse = VariableStore.APPOINTMENT_RESULT.Not_Found + " : You " + patientID
                            + " doesn't have any appointment with id:: " + appointmentID;
                } else {
                    for (String appointmentType : appointmentTypes) {
                        if (hashUserAppointment.get(patientID).get(appointmentType).remove(appointmentID)) {
                            hashAllAppointment.get(appointmentType).get(appointmentID).removePatientID(patientID);
                            serverResponse = VariableStore.APPOINTMENT_RESULT.Success + " : Appointment " + appointmentID
                                    + " has been canceled for " + patientID + "for" + appointmentType;
                        } else {
                            serverResponse = VariableStore.APPOINTMENT_RESULT.Not_Found + " : You " + patientID
                                    + " doesn't have any appointment with id:: " + appointmentID;
                        }
                    }

                }
            } else {
                for (String appointmentType : appointmentTypes) {
                    if (hashAllAppointment.get(appointmentType).get(appointmentID).removePatientID(patientID)) {
                        hashUserAppointment.get(patientID).get(appointmentType).remove(appointmentID);
                        serverResponse = VariableStore.APPOINTMENT_RESULT.Success + " : Appointment " + appointmentID
                                + " has been canceled for " + patientID + "for" + appointmentType;
                    } else {
                        serverResponse = VariableStore.APPOINTMENT_RESULT.Not_Found + " : You " + patientID
                                + " doesn't have any appointment with id:: " + appointmentID;
                    }
                }

            }
            return serverResponse;
        } else {
            if (ClientModel.detectClientServer(patientID).toString().equals(currentServerID)) {
                if (!hashServerUsers.containsKey(patientID)) {
                    addNewUsersToClients(patientID);
                } else {
                    List<String> responseList = new ArrayList<>();
                    if (appointmentTypes.isEmpty()) {
                        appointmentTypes.add(APPOINTMENT_TYPE.PHYSICIAN.toString());
                        appointmentTypes.add(APPOINTMENT_TYPE.SURGEON.toString());
                        appointmentTypes.add(APPOINTMENT_TYPE.DENTAL.toString());

                    }
                    for (String appointmentType : appointmentTypes) {
                        if (!hashUserAppointment.isEmpty()) {
                            if (hashUserAppointment.containsKey(patientID)) {
                                if (hashUserAppointment.get(patientID).containsKey(appointmentType)) {
                                    hashUserAppointment.get(patientID).get(appointmentType).remove(appointmentID);
                                    String list = broadcastMessageToOtherServers(
                                            VariableStore.getSocketPortsForReplica4(
                                                    AppointmentModel.detectAppointmentServer(appointmentID)),
                                            "cancelAppointment", patientID, appointmentType, appointmentID);
                                    if (list.startsWith(VariableStore.APPOINTMENT_RESULT.Success.toString())) {
                                        responseList.add(list);
                                    }
                                }
                            }

                        }

                    }
                    serverResponse = responseList.toString();
                    severLogger(serverResponse, "Cancel appointment");
                    return serverResponse;
                }
            }
            serverResponse = VariableStore.APPOINTMENT_RESULT.Not_Found + " : You " + patientID
                    + " doesn't have any appointment with id:: " + appointmentID;
            severLogger(serverResponse, "Cancel appointment");
            return serverResponse;
        }
    }

    public void addNewUsersToClients(String customerID) {
        ClientModel newUser = new ClientModel(customerID);
        hashServerUsers.put(newUser.getClientID(), newUser);
        hashUserAppointment.put(newUser.getClientID(), new ConcurrentHashMap<>());
    }

    private void transferAppointment(String appointmentIds, String appointmentType, List<String> registeredClients) {
        for (String patientId : registeredClients) {
            hashUserAppointment.get(patientId).get(appointmentType).remove(appointmentIds);
            String nextAppointmentResponse = getNextAppointment(hashAllAppointment.get(appointmentType).keySet(),
                    appointmentType, appointmentIds);
            if (nextAppointmentResponse.equals("Failed")) {
                return;
            } else {
                bookAppointment(patientId, nextAppointmentResponse, appointmentType);
            }
        }
    }

    private String getNextAppointment(Set<String> keySet, String appointmentType, String appointmentId) {
        List<String> sortedListOfIds = new ArrayList<>(keySet);
        sortedListOfIds.add(appointmentId);

        sortedListOfIds.sort((ID1, ID2) -> {
            Integer timeSlot1 = 0;
            switch (ID1.substring(3, 4).toUpperCase()) {
                case "M":
                    timeSlot1 = 1;
                    break;
                case "A":
                    timeSlot1 = 2;
                    break;
                case "E":
                    timeSlot1 = 3;
                    break;
            }
            Integer timeSlot2 = 0;
            switch (ID2.substring(3, 4).toUpperCase()) {
                case "M":
                    timeSlot2 = 1;
                    break;
                case "A":
                    timeSlot2 = 2;
                    break;
                case "E":
                    timeSlot2 = 3;
                    break;
            }
            Integer date1 = Integer.parseInt(ID1.substring(8, 10) + ID1.substring(6, 8) + ID1.substring(4, 6));
            Integer date2 = Integer.parseInt(ID2.substring(8, 10) + ID2.substring(6, 8) + ID2.substring(4, 6));
            int dateCompare = date1.compareTo(date2);
            int timeSlotCompare = timeSlot1.compareTo(timeSlot2);
            if (dateCompare == 0) {
                return ((timeSlotCompare == 0) ? dateCompare : timeSlotCompare);
            } else {
                return dateCompare;
            }
        });
        int index = sortedListOfIds.indexOf(appointmentId) + 1;
        for (int i = index; i < sortedListOfIds.size(); i++) {
            if (!hashAllAppointment.get(appointmentType).get(sortedListOfIds.get(i)).isFull()) {
                return sortedListOfIds.get(i);
            }
        }
        return "Failed";
    }

    public String removeAppointmentBroadcaster(String oldAppointmentID, String appointmentType, String patientID) {
        if (!hashServerUsers.containsKey(patientID)) {
            addNewUsersToClients(patientID);
            return VariableStore.APPOINTMENT_RESULT.Success + " : You " + patientID + " Are Not Registered in "
                    + oldAppointmentID;
        } else {
            if (hashUserAppointment.get(patientID).get(appointmentType).remove(oldAppointmentID)) {
                return VariableStore.APPOINTMENT_RESULT.Success + " : Appointment " + oldAppointmentID
                        + " Was Removed from " + patientID + " Schedule";
            } else {
                return VariableStore.APPOINTMENT_RESULT.Fail + " : You " + patientID + " Are Not Registered in "
                        + oldAppointmentID;
            }
        }
    }

    public String listAppointmentBroadcaster(String appointmentType) {
        Map<String, AppointmentModel> appointments = hashAllAppointment.get(appointmentType);
        StringBuilder builder = new StringBuilder();
        builder.append("\n").append(currentServerID).append(" Server ").append(appointmentType).append(":\n");
        if (!(appointments == null) && appointments.isEmpty()) {
            builder.append("No Appointment of type ").append(appointmentType).append(" ");
        } else {
            assert appointments != null;
            for (AppointmentModel appointmentModel : appointments.values()) {
                builder.append(appointmentModel.getAppointmentID()).append(" ")
                        .append(appointmentModel.getRemainAppointmentCapacity()).append(" ");
            }
        }
        builder.append("\n");
        return builder.toString();
    }

    private String broadcastMessageToOtherServers(int serverPort, String method, String patientID,
                                                  String appointmentType, String appointmentId) {
        DatagramSocket socket = null;
        String broadcastResponse = "";
        String dataFromClientSide = method + ";" + patientID + ";" + appointmentType + ";" + appointmentId;
        try {
            socket = new DatagramSocket();
            byte[] message = dataFromClientSide.getBytes();
            InetAddress inetAddress = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(message, dataFromClientSide.length(), inetAddress, serverPort);
            socket.send(request);

            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            socket.receive(reply);
            broadcastResponse = new String(reply.getData());
            String[] parts = broadcastResponse.split(";");
            broadcastResponse = parts[0];
        } catch (SocketException e) {
            e.printStackTrace();
            System.out.println("Socket error is : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("I/O error is: " + e.getMessage());
        } finally {
            if (socket != null)
                socket.close();
        }
        return broadcastResponse;

    }

    private boolean checkWeeklySchedule(String patientId, String appointmentDate) {
        int limit = 0;
        Map<String, List<String>> patientAppointments = hashUserAppointment.get(patientId);
        if (patientAppointments == null) {
            return false; // No Appointments registered for the patient
        }

        for (List<String> registeredIDs : Arrays.asList(
                patientAppointments.getOrDefault(APPOINTMENT_TYPE.PHYSICIAN.toString(), Collections.emptyList()),
                patientAppointments.getOrDefault(APPOINTMENT_TYPE.SURGEON.toString(), Collections.emptyList()),
                patientAppointments.getOrDefault(APPOINTMENT_TYPE.DENTAL.toString(), Collections.emptyList()))) {
            for (String appointment : registeredIDs) {
                if (appointment.substring(6, 8).equals(appointmentDate.substring(2, 4))
                        && appointment.substring(8, 10).equals(appointmentDate.substring(4, 6))) {
                    int week1 = Integer.parseInt(appointment.substring(4, 6)) / 7;
                    int week2 = Integer.parseInt(appointmentDate.substring(0, 2)) / 7;
                    if (week1 == week2) {
                        limit++;
                    }
                }
                if (limit == 3) {
                    return true;
                }
            }
        }
        return false;
    }

    private void severLogger(String serverResponse, String action) {
        try {
            Logger.log(VariableStore.LOG_TYPE_SERVER, currentServerID,
                    " The request is >> " + action + " || The Response sent from server is >> " + serverResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> getAppointmentTypesForPatient(String appointmentID, String patientID, String appointmentType) {
        List<String> appointmentTypes = new ArrayList<>();
        for (String primaryKey : hashAllAppointment.keySet()) {
            if (hashUserAppointment.containsKey(patientID)) {
                if (hashUserAppointment.get(patientID).containsKey(primaryKey)) {
                    for (String appointment : hashUserAppointment.get(patientID).get(primaryKey)) {
                        if (Objects.equals(appointment, appointmentID)) {
                            appointmentTypes.add(primaryKey);
                        }
                        if (!Objects.equals(appointmentType, primaryKey)) {
                            if (appointmentID.substring(4).equals(appointment.substring(4))) {
                                appointmentTypes.add(primaryKey);
                            }
                        }
                    }
                }
            }
        }
        return appointmentTypes;
    }

    @Override
    public String swapAppointment(String patientID, String oldAppointmentID, String oldAppointmentType, String newAppointmentID, String newAppointmentType) {
        String response;
        if (!checkClientExists(patientID)) {
            response = VariableStore.APPOINTMENT_RESULT.Fail + ": You " + patientID + " is not registered in "
                    + oldAppointmentID;

        } else {
            if (ifAppointExistInUser(patientID, oldAppointmentType, oldAppointmentID)) {
                String bookResp = VariableStore.APPOINTMENT_RESULT.Fail
                        + ": did not send book request for your appointment " + newAppointmentID;
                String cancelResp = VariableStore.APPOINTMENT_RESULT.Fail
                        + ": did not send cancel request for your old appointment " + oldAppointmentID;
                synchronized (this) {
                    if (onTheSameWeek(newAppointmentID.substring(4), oldAppointmentID)
                            && !checkWeeklySchedule(patientID, newAppointmentID.substring(4))) {
                        cancelResp = cancelAppointment(patientID, oldAppointmentID);
                        if (cancelResp.startsWith("[Success") || cancelResp.startsWith("Success")) {
                            bookResp = bookAppointment(patientID, newAppointmentID, newAppointmentType);
                        }
                    } else {
                        bookResp = bookAppointment(patientID, newAppointmentID, newAppointmentType);
                        if (bookResp.startsWith("Success")) {
                            cancelResp = cancelAppointment(patientID, oldAppointmentID);
                        }
                    }
                }
                if (bookResp.startsWith("Success")
                        && (cancelResp.startsWith("[Success") || cancelResp.startsWith("Success"))) {
                    response = VariableStore.APPOINTMENT_RESULT.Success + " : Appointment " + oldAppointmentID + " swapped with "
                            + newAppointmentID;
                } else if (bookResp.startsWith("Success")
                        && cancelResp.startsWith("Fail")) {
                    cancelAppointment(patientID, newAppointmentID);
                    response = "Failed: Your oldAppointment " + oldAppointmentID + " Could not be Canceled reason: " + cancelResp;
                } else if (bookResp.startsWith("Fail")
                        && (cancelResp.startsWith("[Success") || cancelResp.startsWith("Success"))) {
                    String resp1 = bookAppointment(patientID, oldAppointmentID, oldAppointmentType);
                    response = VariableStore.APPOINTMENT_RESULT.Fail + ": Your newAppointment " + newAppointmentID + " Could not be Booked reason: " + bookResp + " And your old Appointment Rolling back: " + resp1;
                } else {
                    response = VariableStore.APPOINTMENT_RESULT.Fail + ": on Both newAppointment " + newAppointmentID + " Booking reason: " + bookResp + " and oldAppointment " + oldAppointmentID + " Canceling reason: " + cancelResp;
                }

            } else {
                response = VariableStore.APPOINTMENT_RESULT.Fail + ": You " + patientID + " is not registered in " + oldAppointmentID;

            }

        }
        severLogger(serverResponse, "Swap appointment");
        return response;
    }

    private boolean checkClientExists(String patientID) {
        if (!hashServerUsers.containsKey(patientID)) {
            addNewUsersToClients(patientID);
            return false;
        } else {
            return true;
        }
    }

    private boolean ifAppointExistInUser(String patientID, String appointmentType, String apointmentID) {
        if (hashUserAppointment.get(patientID).containsKey(appointmentType)) {
            return hashUserAppointment.get(patientID).get(appointmentType).contains(apointmentID);
        } else {
            return false;
        }
    }

    private boolean onTheSameWeek(String newAppointmentDate, String appointmentID) {
        if (appointmentID.substring(6, 8).equals(newAppointmentDate.substring(2, 4))
                && appointmentID.substring(8, 10).equals(newAppointmentDate.substring(4, 6))) {
            int week1 = Integer.parseInt(appointmentID.substring(4, 6)) / 7;
            int week2 = Integer.parseInt(newAppointmentDate.substring(0, 2)) / 7;
            return week1 == week2;
        } else {
            return false;
        }
    }


    private List<String> fetchAppointmentType(String patientID, String appointmentID) {
        List<String> appointmentTypes = new ArrayList<>();
        for (String primaryKey : hashAllAppointment.keySet()) {
            Map<String, AppointmentModel> appointments = hashAllAppointment.get(primaryKey);

            if (appointments.containsKey(appointmentID)) {
                if (hashUserAppointment.get(patientID).containsKey(primaryKey)) {
                    for (String appointmentId : hashUserAppointment.get(patientID).get(primaryKey)) {
                        if (Objects.equals(appointmentId, appointmentID)) {
                            appointmentTypes.add(primaryKey);
                        }
                    }
                }
            }
        }
        return appointmentTypes;
    }
}
