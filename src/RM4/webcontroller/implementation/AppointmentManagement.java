package RM4.webcontroller.implementation;

import Model.ClientModel;
import Model.AppointmentModel;
import RM4.webcontroller.webInterface;
import log.Log;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@WebService(endpointInterface = "src.RM4.webcontroller.webInterface")

@SOAPBinding(style = SOAPBinding.Style.RPC)
public class AppointmentManagement implements webInterface {

    private String serverID;
    private String serverName;
    public static final int MONTREAL_SERVER_PORT = 8888;
    public static final int QUEBEC_SERVER_PORT = 7777;
    public static final int SHERBROOKE_SERVER_PORT = 6666;
    private Map<String, Map<String, AppointmentModel>> allAppointments;
    private Map<String, Map<String, List<String>>> clientsAppointments;
    private Map<String, ClientModel> serverClients;
    public AppointmentManagement(){}

    public AppointmentManagement(String serverID, String serverName){
        super();
        this.serverID = serverID;
        this.serverName = serverName;
        allAppointments = new ConcurrentHashMap<>();
        allAppointments.put(AppointmentModel.MONTREAL, new ConcurrentHashMap<>());
        allAppointments.put(AppointmentModel.QUEBEC, new ConcurrentHashMap<>());
        allAppointments.put(AppointmentModel.SHERBROOKE, new ConcurrentHashMap<>());
        clientsAppointments = new ConcurrentHashMap<>();
        serverClients = new ConcurrentHashMap<>();
    }

    private static int getServerPort(String hospitalBranch) {
        if (hospitalBranch.equalsIgnoreCase("MTL")) {
            return MONTREAL_SERVER_PORT;
        } else if (hospitalBranch.equalsIgnoreCase("QUE")) {
            return QUEBEC_SERVER_PORT;
        } else if (hospitalBranch.equalsIgnoreCase("SHE")) {
            return SHERBROOKE_SERVER_PORT;
        }
        return 1;
    }

    @Override
    public String addAppointment( String appointmentID, String appointmentType, int appointmentCapacity) {
        String response;
        if (allAppointments.get(appointmentType).containsKey(appointmentID)) {
            if (allAppointments.get(appointmentType).get(appointmentID).getAPPOINTMENT_CAPACITY() <= appointmentCapacity) {
                allAppointments.get(appointmentType).get(appointmentID).setAPPOINTMENT_CAPACITY(appointmentCapacity);
                response = "Success: Appointment " + appointmentID + " Capacity increased to " + appointmentCapacity;
                try {
                    Log.serverLog(serverID, "null", " web service addAppointment ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " appointmentCapacity " + appointmentCapacity + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                response = "Failed: Appointment Already Exists, Cannot Decrease APPOINTMENT Capacity";
                try {
                    Log.serverLog(serverID, "null", " web service addAppointment ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " appointmentCapacity " + appointmentCapacity + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return response;
        }
        if (AppointmentModel.checkAPPOINTMENT_SERVER(appointmentID).equals(serverName)) {
            AppointmentModel appointment = new AppointmentModel(appointmentType, appointmentID, appointmentCapacity);
            Map<String, AppointmentModel> appointmentHashMap = allAppointments.get(appointmentType);
            appointmentHashMap.put(appointmentID, appointment);
            allAppointments.put(appointmentType, appointmentHashMap);
            response = "Success: Appointment " + appointmentID + " added successfully";
            try {
                Log.serverLog(serverID, "null", " web service addAppointment ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " appointmentCapacity " + appointmentCapacity + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            response = "Failed: Cannot Add Appointment to servers other than " + serverName;
            try {
                Log.serverLog(serverID, "null", " web service addAppointment ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " appointmentCapacity " + appointmentCapacity + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    @Override
    public String removeAppointment(String appointmentID, String appointmentType) {
        String response;
        String logMessage = " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ";

        if (!AppointmentModel.checkAPPOINTMENT_SERVER(appointmentID).equals(serverName)) {
            response = "Failed: Cannot Remove Appointment from servers other than " + serverName;
        } else if (!allAppointments.containsKey(appointmentType) || !allAppointments.get(appointmentType).containsKey(appointmentID)) {
            response = "Failed: Appointment " + appointmentID + " Does Not Exist";
        } else {
            List<String> registeredClients = allAppointments.get(appointmentType).get(appointmentID).getRegisteredClientIDs();
            allAppointments.get(appointmentType).remove(appointmentID);
            addPatientsToNextSameAppointment(appointmentID, appointmentType, registeredClients);
            response = "Success: Appointment Removed Successfully";
        }

        try {
            Log.serverLog(serverID, "null", " web service removeAppointment ", logMessage, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public String listAppointmentAvailability(String appointmentType) {
        StringBuilder builder = new StringBuilder();
        builder.append(serverName).append(" Server ").append(appointmentType).append(":\n");

        Map<String, AppointmentModel> appointments = allAppointments.get(appointmentType);
        if (appointments.isEmpty()) {
            builder.append("No Appointments of Type ").append(appointmentType);
        } else {
            for (AppointmentModel appointment : appointments.values()) {
                builder.append(appointment.toString()).append(" , ");
            }
            builder.append("\n=====================================\n");
        }

        // Retrieve appointment availability from other servers
        String otherServer1 = sendUDPMessage(getOtherServerPort(1), "listAppointmentAvailability", "null", appointmentType, "null");
        String otherServer2 = sendUDPMessage(getOtherServerPort(2), "listAppointmentAvailability", "null", appointmentType, "null");

        builder.append(otherServer1).append(otherServer2);

        String response = builder.toString();
        try {
            Log.serverLog(serverID, "null", " web service listAppointmentAvailability ", " appointmentType: " + appointmentType + " ", response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    private int getOtherServerPort(int serverIndex) {
        switch (serverID) {
            case "MTL":
                return serverIndex == 1 ? QUEBEC_SERVER_PORT : SHERBROOKE_SERVER_PORT;
            case "SHE":
                return serverIndex == 1 ? MONTREAL_SERVER_PORT : QUEBEC_SERVER_PORT;
            default: // For "QUE"
                return serverIndex == 1 ? MONTREAL_SERVER_PORT : SHERBROOKE_SERVER_PORT;
        }
    }
    @Override
    public String bookAppointment(String patientID, String appointmentID, String appointmentType) {
        String response = "";

        // Check if the patient is already in the client list, if not, add them
        if (!serverClients.containsKey(patientID)) {
            addNewPatientToClients(patientID);
        }

        // Check if the patient already has an appointment with the same type or ID
        List<String> uniqueAppointmentID = getAppointmentTypes(patientID, appointmentID, appointmentType);
        if (!uniqueAppointmentID.isEmpty()) {
            response = "Failed: Cannot book appointments with appointment ID " + appointmentID + " as already has an appointment with the same type or same ID cannot be used to book other types.";
        } else {
            // Check if the appointment is on the local server
            if (AppointmentModel.checkAPPOINTMENT_SERVER(appointmentID).equals(serverName)) {
                if (allAppointments.get(appointmentType).containsKey(appointmentID)) {
                    AppointmentModel bookedAppointment = allAppointments.get(appointmentType).get(appointmentID);
                    if (!bookedAppointment.isFull()) {
                        // Add the appointment to the client's appointments
                        clientsAppointments.computeIfAbsent(patientID, k -> new ConcurrentHashMap<>())
                                .computeIfAbsent(appointmentType, k -> new ArrayList<>())
                                .add(appointmentID);

                        // Add the client to the appointment's registered clients
                        if (bookedAppointment.addRegisteredClientID(patientID) == AppointmentModel.ADD_SUCCESS) {
                            response = "Success: Appointment " + appointmentID + " Booked Successfully";
                        } else {
                            response = "Failed: Cannot Add You To Appointment " + appointmentID;
                        }
                    } else {
                        response = "Failed: Appointment " + appointmentID + " is Full";
                    }
                } else {
                    response = "Cannot book an appointment as no slot available.";
                }
            } else {
                // If the appointment is on a remote server
                if (!otherServersWeeklyLimit(patientID, appointmentID.substring(4))) {
                    String serverResponse = sendUDPMessage(getServerPort(appointmentID.substring(0, 3)), "bookappointment", patientID, appointmentType, appointmentID);
                    if (serverResponse.startsWith("Success:")) {
                        // Add the appointment to the client's appointments
                        clientsAppointments.computeIfAbsent(patientID, k -> new ConcurrentHashMap<>())
                                .computeIfAbsent(appointmentType, k -> new ArrayList<>())
                                .add(appointmentID);
                    }
                    response = serverResponse;
                } else {
                    response = "Failed: You Cannot Book Appointment in Other Servers For This Week (Max Weekly Limit = 3)";
                }
            }
        }

        // Log the response
        try {
            Log.serverLog(serverID, patientID, " web service bookappointment ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    private List<String> getAppointmentTypes(String patientID, String appointmentID, String patientAppointmentType) {
        List<String> appointmentTypes = new ArrayList<>();
        for (String primaryKey : allAppointments.keySet()) {
            Map<String, AppointmentModel> appointments = allAppointments.get(primaryKey);

            if (appointments.containsKey(appointmentID)) {
                if (clientsAppointments.get(patientID).containsKey(primaryKey)) {
                    for(String appointmentId :clientsAppointments.get(patientID).get(primaryKey) ){
                        if (Objects.equals(appointmentId, appointmentID) ||
                                (!Objects.equals(primaryKey, patientAppointmentType) && Objects.equals(appointmentId.substring(5, 10), appointmentID.substring(5, 10)))) {
                            appointmentTypes.add(primaryKey);
                            break;
                        }
                    }
                }
            }
        }
        return appointmentTypes;
    }

    @Override
    public String getBookingSchedule(String patientID) {
        if (!serverClients.containsKey(patientID)) {
            addNewPatientToClients(patientID);
        }
        System.out.println(clientsAppointments.get(patientID));
        Map<String, List<String>> appointments = clientsAppointments.getOrDefault(patientID, new HashMap<>());
        StringBuilder builder = new StringBuilder();

        if (appointments.isEmpty()) {
            builder.append("Appointment Schedule Empty For ").append(patientID);
        } else {
            for (Map.Entry<String, List<String>> entry : appointments.entrySet()) {
                String appointmentType = entry.getKey();
                List<String> appointmentIDs = entry.getValue();

                builder.append(appointmentType).append(":\n");
                for (String appointmentID : appointmentIDs) {
                    builder.append(appointmentID).append(" ||");
                }
                builder.append("\n=====================================\n");
            }
        }

        String response = builder.toString();
        try {
            Log.serverLog(serverID, patientID, " web service getAppointmentSchedule ", "null", response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    private List<String> fetchAppointmentType(String patientID, String appointmentID) {
        List<String> appointmentTypes = new ArrayList<>();
        for (String primaryKey : allAppointments.keySet()) {
            Map<String, AppointmentModel> appointments = allAppointments.get(primaryKey);

            if (appointments.containsKey(appointmentID)) {
                if (clientsAppointments.get(patientID).containsKey(primaryKey)) {
                    for(String appointmentId :clientsAppointments.get(patientID).get(primaryKey) ){
                        if(Objects.equals(appointmentId, appointmentID)){
                            appointmentTypes.add(primaryKey);
                        }
                    }
                }
            }
        }
        return appointmentTypes;
    }

    @Override
    public String cancelAppointment(String patientID, String appointmentID) {
        String response = "";
        List<String> appointmentTypes = fetchAppointmentType(patientID, appointmentID);
        if (AppointmentModel.checkAPPOINTMENT_SERVER(appointmentID).equals(serverName)) {
            if (patientID.substring(0, 3).equals(serverID)) {
                if (!serverClients.containsKey(patientID)) {
                    addNewPatientToClients(patientID);
                    response = "Failed: You " + patientID + " does not have an appointment with " + appointmentID;
                    try {
                        Log.serverLog(serverID, patientID, " web service cancelAppointment ", " appointmentID: " + appointmentID + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                } else {
                    if (!appointmentTypes.isEmpty()) {
                        for (String appointmentType : appointmentTypes) {
                            if (allAppointments.get(appointmentType).get(appointmentID).removeRegisteredClientID(patientID)) {
                                clientsAppointments.get(patientID).get(appointmentType).remove(appointmentID);
                                response = "Success: Appointment " + appointmentID + " Canceled for " + patientID;
                                return response;
                            } else {
                                response = "Failed: You " + patientID + " does not have an appointment with " + appointmentID;
                                return response;
                            }
                        }
                        try {
                            Log.serverLog(serverID, patientID, " web service cancelAppointment ", " appointmentID: " + appointmentID + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        response = "Failed: You " + patientID + " does not have an appointment with " + appointmentID;
                        try {
                            Log.serverLog(serverID, patientID, " web service cancelAppointment ", " appointmentID: " + appointmentID + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return response;
                    }
                }
            } else {
                for (String appointmentType : appointmentTypes) {
                    if (allAppointments.get(appointmentType).get(appointmentID).removeRegisteredClientID(patientID)) {
                        clientsAppointments.get(patientID).get(appointmentType).remove(appointmentID);
                        response = "Success: Appointment " + appointmentID + " Canceled for " + patientID;
                        return response;
                    } else {
                        response = "Failed: You " + patientID + " does not have an appointment with " + appointmentID;
                        return response;
                    }
                }
                try {
                    Log.serverLog(serverID, patientID, " web service cancelAppointment ", " appointmentID: " + appointmentID + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (patientID.substring(0, 3).equals(serverID)) {
                if (!serverClients.containsKey(patientID)) {
                    addNewPatientToClients(patientID);
                } else {
                    if (appointmentTypes.isEmpty()) {
                        appointmentTypes.add(AppointmentModel.TYPE_PHYSICIAN);
                        appointmentTypes.add(AppointmentModel.TYPE_SURGEON);
                        appointmentTypes.add(AppointmentModel.TYPE_DENTAL);
                    }
                    List<String> responseList = new ArrayList<>();
                    for (String appointmentType : appointmentTypes) {
                        if(!clientsAppointments.isEmpty()){
                            if(clientsAppointments.containsKey(patientID)){
                                if(clientsAppointments.get(patientID).containsKey(appointmentType)){
                                    clientsAppointments.get(patientID).get(appointmentType).remove(appointmentID);
                                }
                            }
                            String serverResponse = sendUDPMessage(getServerPort(appointmentID.substring(0, 3)), "cancelAppointment", patientID, appointmentType, appointmentID);
                            if (serverResponse.startsWith("Success:")) {
                                responseList.add(serverResponse);
                            }
                        }
                    }
                    response = responseList.toString();
                    return response;
                }
            }
            return "Failed: You " + patientID + " does not have an appointment with " + appointmentID;
        }
        return response;
    }

    private synchronized boolean checkPatientExists(String patinetID) {
        if (!serverClients.containsKey(patinetID)) {
            addNewPatientToClients(patinetID);
            return false;
        } else {
            return true;
        }
    }

    private synchronized boolean clientHasAppointment(String patientID, String appointmentType, String appointmentID) {
        if (clientsAppointments.get(patientID).containsKey(appointmentType)) {
            return clientsAppointments.get(patientID).get(appointmentType).contains(appointmentID);
        } else {
            return false;
        }
    }

    private boolean otherServersWeeklyLimit(String patientID, String appointmentDate) {
        int limit = 0;
        for (int i = 0; i < 3; i++) {
            String appointmentType = "";
            List<String> registeredIDs = new ArrayList<>();
            switch (i) {
                case 0:
                    appointmentType = AppointmentModel.TYPE_PHYSICIAN;
                    break;
                case 1:
                    appointmentType = AppointmentModel.TYPE_SURGEON;
                    break;
                case 2:
                    appointmentType = AppointmentModel.TYPE_DENTAL;
                    break;
                default:
                    // Keep the existing value of appointmentType
                    break;
            }

            if (clientsAppointments.containsKey(patientID) && clientsAppointments.get(patientID).containsKey(appointmentType)) {
                registeredIDs = clientsAppointments.get(patientID).get(appointmentType);
            }
            for (String appointmentID :
                    registeredIDs) {
                if (appointmentID.substring(6, 8).equals(appointmentDate.substring(2, 4)) && appointmentID.substring(8, 10).equals(appointmentDate.substring(4, 6))) {
                    int week1 = Integer.parseInt(appointmentID.substring(4, 6)) / 7;
                    int week2 = Integer.parseInt(appointmentDate.substring(0, 2)) / 7;
                    if (week1 == week2) {
                        limit++;
                    }
                }
                if (limit == 3)
                    return true;
            }
        }
        return false;
    }

    @Override
    public String swapAppointment(String patientID, String newAppointmentID, String newAppointmentType, String oldAppointmentID, String oldAppointmentType) {
        String response;
        if (!checkPatientExists(patientID)) {
            response = "Failed: You " + patientID + " is Not Registered in " + oldAppointmentID;
            try {
                Log.serverLog("Meet", patientID, " web service swapAppointment ", " oldAppointmentID: " + oldAppointmentID + " oldAppointmentType: " + oldAppointmentType + " newAppointmentID: " + newAppointmentID + " newAppointmentType: " + newAppointmentType + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        } else {
            if (clientHasAppointment(patientID, oldAppointmentType, oldAppointmentID)) {
                String bookResp = "Failed: did not send book request for your newappointment " + newAppointmentID;
                String cancelResp = "Failed: did not send cancel request for your oldAppointment " + oldAppointmentID;
                synchronized (this) {
                    if (onTheSameWeek(newAppointmentID.substring(4), oldAppointmentID) && !otherServersWeeklyLimit(patientID, newAppointmentID.substring(4))) {
                        cancelResp = cancelAppointment(patientID, oldAppointmentID);
                        if (cancelResp.startsWith("Success:")) {
                            bookResp = bookAppointment(patientID, newAppointmentID, newAppointmentType);
                        }
                    } else {
                        bookResp = bookAppointment(patientID, newAppointmentID, newAppointmentType);
                        if (bookResp.startsWith("Success:")) {
                            cancelResp = cancelAppointment(patientID, oldAppointmentID);
                        }
                    }
                }
                if (bookResp.startsWith("Success:") && cancelResp.startsWith("Success:")) {
                    response = "Success: Appointment " + oldAppointmentID + " swapped with " + newAppointmentID;
                } else if (bookResp.startsWith("Success:") && cancelResp.startsWith("Failed:")) {
                    cancelAppointment(patientID, newAppointmentID);
                    response = "Failed: Your oldAppointment " + oldAppointmentID + " Could not be Canceled reason: " + cancelResp;
                } else if (bookResp.startsWith("Failed:") && cancelResp.startsWith("Success:")) {
                    //hope this won't happen, but just in case.
                    String resp1 = bookAppointment(patientID, oldAppointmentID, oldAppointmentType);
                    response = "Failed: Your newappointment " + newAppointmentID + " Could not be Booked reason: " + bookResp + " And your old appointment Rolling back: " + resp1;
                } else {
                    response = "Failed: on Both newappointment " + newAppointmentID + " Booking reason: " + bookResp + " and oldAppointment " + oldAppointmentID + " Canceling reason: " + cancelResp;
                }
                try {
                    Log.serverLog(serverID, patientID, " web service swapAppointment ", " oldAppointmentID: " + oldAppointmentID + " oldAppointmentType: " + oldAppointmentType + " newAppointmentID: " + newAppointmentID + " newAppointmentType: " + newAppointmentType + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } else {
                response = "Failed: You " + patientID + " is Not Registered in " + oldAppointmentID;
                try {
                    Log.serverLog(serverID, patientID, " web service swapAppointment ", " oldAppointmentID: " + oldAppointmentID + " oldAppointmentType: " + oldAppointmentType + " newAppointmentID: " + newAppointmentID + " newAppointmentType: " + newAppointmentType + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }
    }

    /**
     * for udp calls only
     *
     * @param oldAppointmentID
     * @param appointmentType
     * @param patientID
     * @return
     */
    public String removeAppointmentUDP(String oldAppointmentID, String appointmentType, String patientID){
        if (!serverClients.containsKey(patientID)) {
            addNewPatientToClients(patientID);
            return "Failed: You " + patientID + " does not have an appointment in " + oldAppointmentID;
        } else {
            if (clientsAppointments.get(patientID).get(appointmentType).remove(oldAppointmentID)) {
                return "Success: Appointment " + oldAppointmentID + " Was Removed from " + patientID + " Schedule";
            } else {
                return "Failed: You " + patientID + " does not have an appointment in " + oldAppointmentID;
            }
        }
    }

    /**
     * for UDP calls only
     *
     * @param appointmentType
     * @return
     */
    public String listAppointmentAvailabilityUDP(String appointmentType){
        Map<String, AppointmentModel> appointments = allAppointments.get(appointmentType);
        StringBuilder builder = new StringBuilder();
        builder.append(serverName + " Server " + appointmentType + ":\n");
        if (appointments.size() == 0) {
            builder.append("No Appointments of Type " + appointmentType);
        } else {
            for (AppointmentModel appointment :
                    appointments.values()) {
                builder.append(appointment.toString() + " , ");
            }
        }
        builder.append("\n=====================================\n");
        return builder.toString();
    }

    private String sendUDPMessage(int serverPort, String method, String patientID, String appointmentType, String appointmentId) {
        DatagramSocket aSocket = null;
        String result = "";
        String dataFromClient = method + ";" + patientID + ";" + appointmentType + ";" + appointmentId;
        try {
            Log.serverLog(serverID, patientID, " UDP request sent " + method + " ", " appointmentID: " + appointmentId + " appointmentType: " + appointmentType + " ", " ... ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            aSocket = new DatagramSocket();
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(message, dataFromClient.length(), aHost, serverPort);
            aSocket.send(request);

            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            aSocket.receive(reply);
            result = new String(reply.getData());
            String[] parts = result.split(";");
            result = parts[0];
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
        try {
            Log.serverLog(serverID, patientID, " UDP reply received" + method + " ", " appointmentID: " + appointmentId + " appointmentType: " + appointmentType + " ", result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;

    }

    private String getNextSameAppointment(Set<String> keySet, String appointmentType, String oldAppointmentID) {
        List<String> sortedIDs = new ArrayList<String>(keySet);
        sortedIDs.add(oldAppointmentID);
        Collections.sort(sortedIDs, new Comparator<String>() {
            @Override
            public int compare(String ID1, String ID2) {
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
            }
        });
        int index = sortedIDs.indexOf(oldAppointmentID) + 1;
        for (int i = index; i < sortedIDs.size(); i++) {
            if (!allAppointments.get(appointmentType).get(sortedIDs.get(i)).isFull()) {
                return sortedIDs.get(i);
            }
        }
        return "Failed";
    }

    private void addPatientsToNextSameAppointment(String oldAppointmentID, String appointmentType, List<String> registeredClients){
        for (String patientID :
                registeredClients) {
            clientsAppointments.get(patientID).get(appointmentType).remove(oldAppointmentID);
            String nextSameAppointmentResult = getNextSameAppointment(allAppointments.get(appointmentType).keySet(), appointmentType, oldAppointmentID);
            if (nextSameAppointmentResult.equals("Failed")) {
                return;
            } else {
                bookAppointment(patientID, nextSameAppointmentResult, appointmentType);
            }
            sendUDPMessage(getServerPort(patientID.substring(0, 3)), "removeAppointment", patientID, appointmentType, oldAppointmentID);
        }
    }

    private boolean onTheSameWeek(String newAppointmentDate, String appointmentID) {
        if (appointmentID.substring(6, 8).equals(newAppointmentDate.substring(2, 4)) && appointmentID.substring(8, 10).equals(newAppointmentDate.substring(4, 6))) {
            int week1 = Integer.parseInt(appointmentID.substring(4, 6)) / 7;
            int week2 = Integer.parseInt(newAppointmentDate.substring(0, 2)) / 7;
            return week1 == week2;
        } else {
            return false;
        }
    }

    public void addNewPatientToClients(String patientID) {
        ClientModel newPatient = new ClientModel(patientID);
        serverClients.put(newPatient.getUSER_ID(), newPatient);
        clientsAppointments.put(newPatient.getUSER_ID(), new ConcurrentHashMap<>());
    }
}