package org.project.replica3;

import org.project.replica3.controllers.AppointmentController;
import org.project.replica3.interfaces.WebServiceInterface;
import org.project.utils.VariableStore;

import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


public class Server {

    private String serverId;
    private int serverPort;
    private int serverBroadCastPort;
    private String serverEndpoint;
    public Server(VariableStore.SERVERS serverLocation) {
        configServer(serverLocation);
    }

    private static void listenForClientRequest(AppointmentController appointmentController, int serverUdpPort, String serverID) {
        DatagramSocket datagramSocket = null;
        String response = "";
        try {
            datagramSocket = new DatagramSocket(serverUdpPort);
            byte[] buffer = new byte[1000];
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(request);
                String sentence = new String(request.getData(), 0,
                        request.getLength());
                String[] parts = sentence.split(";");
                String method = parts[0];
                String patientID = parts[1];
                String appointmentType = parts[2];
                String appointmentID = parts[3];
                String tempResult = "";
                String logMessage = "Server got request with \n" + sentence + "\nFor" + " Method:: " + method + " patientID:: " + patientID + " appointmentType:: " + appointmentType + " appointmentID:: " + appointmentID;
                Logger.log(VariableStore.LOG_TYPE_SERVER, serverID, logMessage);

                if (method.equalsIgnoreCase("removeAppointment")) {
                    tempResult = appointmentController.removeAppointmentBroadcaster(appointmentID, appointmentType, patientID);
                } else if (method.equalsIgnoreCase("listAppointmentAvailability")) {
                    tempResult = appointmentController.listAppointmentBroadcaster(appointmentType);
                } else if (method.equalsIgnoreCase("bookAppointment")) {
                    tempResult = appointmentController.bookAppointment(patientID, appointmentID, appointmentType);
                } else if (method.equalsIgnoreCase("cancelAppointment")) {
                    tempResult = appointmentController.cancelAppointment(patientID, appointmentID);
                }
                response = tempResult + ";";
                byte[] sendData = response.getBytes();
                DatagramPacket responseDatagram = new DatagramPacket(sendData, response.length(), request.getAddress(),
                        request.getPort());
                datagramSocket.send(responseDatagram);
                logMessage = "Server responded with \n" + response + "\nFor" + " Method:: " + method + " patientID:: " + patientID + " appointmentType:: " + appointmentType + " appointmentID:: " + appointmentID;
                Logger.log(VariableStore.LOG_TYPE_SERVER, serverID, logMessage);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        }
    }

    private void configServer(VariableStore.SERVERS serverLocation) {
        this.serverId = serverLocation.toString();
        String url = "http://" + VariableStore.INET_MULTICAST_ADDRESS + ":" ;
        if (serverLocation.equals(VariableStore.SERVERS.MTL)) {
            this.serverPort = 7177;
            this.serverBroadCastPort = VariableStore.MONTREAL_SOCKET_PORT3;
            serverEndpoint = url+this.serverPort+"/"+"montreal";
        } else if (serverLocation.equals(VariableStore.SERVERS.QUE)) {
            this.serverPort = 7179;
            this.serverBroadCastPort = VariableStore.QUEBEC_SOCKET_PORT3;
            serverEndpoint = url+this.serverPort+"/"+"quebec";
        } else {
            this.serverPort = 7178;
            this.serverBroadCastPort = VariableStore.SHERBROOKE_SOCKET_PORT3;
            serverEndpoint = url+this.serverPort+"/"+"sherbrook";
        }

        AppointmentController controller = new AppointmentController(serverId);

        Endpoint endpoint = Endpoint.publish(serverEndpoint, controller);
        Runnable task = () -> {
            listenForClientRequest(controller, serverBroadCastPort, serverId);
        };
        try {
            String logMessage = "Server " + serverId + " is started and listening at " + serverPort + " and " + serverBroadCastPort;
            System.out.println(logMessage);

            Logger.log(VariableStore.LOG_TYPE_SERVER, serverId, logMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Thread thread = new Thread(task);
        thread.start();
    }
}
