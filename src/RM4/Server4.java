package RM4;

import RM4.webcontroller.implementation.AppointmentManagement;
import log.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server4 {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.submit(new ServerInstanceRunnable("MTL", args));
        executor.submit(new ServerInstanceRunnable("QUE", args));
        executor.submit(new ServerInstanceRunnable("SHE", args));
        executor.shutdown();
    }

    private static class ServerInstanceRunnable implements Runnable {
        private final String serverName;
        private final String[] args;

        public ServerInstanceRunnable(String serverName, String[] args) {
            this.serverName = serverName;
            this.args = args;
        }

        @Override
        public void run() {
            try {
                ServerInstance serverInstance = new ServerInstance(serverName, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class ServerInstance {

        private String serverID;
        private String serverName;

        private int serverUdpPort;

        private int internalUdpPort ;

        public ServerInstance(String serverID, String[] args) throws Exception {
            this.serverID = serverID;
            switch (serverID) {
                case "MTL":
                    serverName = "MONTREAL";
                    serverUdpPort = 4211;
                    internalUdpPort = 4217;
                    break;
                case "QUE":
                    serverName ="QUEBEC";
                    serverUdpPort = 4212;
                    internalUdpPort = 4218;
                    break;
                case "SHE":
                    serverName = "SHERBROOKE";
                    serverUdpPort = 4213;
                    internalUdpPort = 4219;
                    break;
            }
            try {
                System.out.println(serverName + " RM4 Server Started...");
                Log.serverLog(serverID, " RM4 Server Started...");
                AppointmentManagement service = new AppointmentManagement(serverID, serverName);

                System.out.println(serverName + " Server is Up & Running");
                Log.serverLog(serverID, " Server is Up & Running");

                Runnable task = () -> {
                    receive(service, serverUdpPort, serverName, serverID);
                };
                Thread thread1 = new Thread(task);
                thread1.start();

                Runnable task2 = () -> {
                listenForRequest(service, internalUdpPort, serverName, serverID);
                };
                Thread thread2 = new Thread(task2);
                thread2.start();

            } catch (Exception e) {
                System.err.println("Exception: " + e);
                e.printStackTrace(System.out);
                Log.serverLog(serverID, "Exception: " + e);
            }

        System.out.println(serverName + " Server Shutting down");
            Log.serverLog(serverID, " Server Shutting down");

        }


        //TODO: write the receive method which will receive the request from the RM and send the response back to the RM

        private static void receive(AppointmentManagement obj, int serverUdpPort, String serverName, String serverID) {
            String response = "";
            try (DatagramSocket aSocket = new DatagramSocket(serverUdpPort)) {
                byte[] buffer = new byte[1000];
                System.out.println(serverName + "UDP Server Started at port " + aSocket.getLocalPort() + " ............");
                while (true) {
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    aSocket.receive(request);
                    String sentence = new String(request.getData(), 0, request.getLength());
                    System.out.println(sentence);
                    String[] parts = sentence.split(";");
//                    String method = parts[0];
//                    String userID = parts[1];
//                    String appointmentType = parts[2];
//                    String appointmentID = parts[3];
                    String function = parts[3];
                    String userID = parts[4];
                    String appointmentID = parts[5];
                    String appointmentType = parts[6];
                    int bookingCapacity = Integer.parseInt(parts[0]);

                    System.out.println(function + ' ' + userID + ' ' + appointmentID + ' ' + appointmentType + ' ' + bookingCapacity);
                    if (function.equalsIgnoreCase("addAppointment")) {
                        //System.out.println("UDP request received in server " + method + " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ...");

                        response = obj.addAppointment(appointmentID, appointmentType, bookingCapacity);
                    } else if (function.equalsIgnoreCase("removeAppointment")) {

                        response = obj.removeAppointment(appointmentID, appointmentType);

                    } else if (function.equalsIgnoreCase("bookAppointment")) {
                        response = obj.bookAppointment(userID, appointmentID, appointmentType);
                    } else if (function.equalsIgnoreCase("getBookingSchedule")) {
                        response = obj.getBookingSchedule(userID);
                    } else if (function.equalsIgnoreCase("cancelBooking")) {
                        response = obj.cancelAppointment(userID, appointmentID);
                    }

                    DatagramPacket reply = new DatagramPacket(response.getBytes(), response.length(), request.getAddress(), request.getPort());
                    aSocket.send(reply);
                }
            } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
            }
        }
        
        private static void listenForRequest(AppointmentManagement obj, int serverUdpPort, String serverName, String serverID) {
            String sendingResult = "";
            try (DatagramSocket aSocket = new DatagramSocket(serverUdpPort)) {
                byte[] buffer = new byte[1000];
                System.out.println(serverName + ' ' + "Internal UDP Server Started at port " + aSocket.getLocalPort() + " ............");
                Log.serverLog(serverID, " UDP Server Started at port " + aSocket.getLocalPort());
                while (true) {
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    aSocket.receive(request);
                    String sentence = new String(request.getData(), 0, request.getLength());
                    String[] parts = sentence.split(";");
                    String method = parts[0];
                    String userID = parts[1];
                    String appointmentType = parts[2];
                    String appointmentID = parts[3];
                    if (method.equalsIgnoreCase("removeAppointment")) {
                        Log.serverLog(serverID, userID, " UDP request received " + method + " ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", " ...");
                        String result = obj.removeAppointmentUDP(appointmentID, appointmentType, userID);
                        sendingResult = result + ";";
                    } else if (method.equalsIgnoreCase("listAppointmentAvailability")) {
                        Log.serverLog(serverID, userID, " UDP request received " + method + " ", " appointmentType: " + appointmentType + " ", " ...");
                        String result = obj.listAppointmentAvailabilityUDP(appointmentType);
                        sendingResult = result + ";";
                    } else if (method.equalsIgnoreCase("bookAppointment")) {
                        Log.serverLog(serverID, userID, " UDP request received " + method + " ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", " ...");
                        String result = obj.bookAppointment(userID, appointmentID, appointmentType);
                        sendingResult = result + ";";
                    } else if (method.equalsIgnoreCase("cancelAppointment")) {
                        Log.serverLog(serverID, userID, " UDP request received " + method + " ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", " ...");
                        String result = obj.cancelAppointment(userID, appointmentID);
                        sendingResult = result + ";";
                    }
                    sendingResult = sendingResult.trim();
                    byte[] sendData = sendingResult.getBytes();
                    DatagramPacket reply = new DatagramPacket(sendData, sendingResult.length(), request.getAddress(), request.getPort());
                    aSocket.send(reply);
                    Log.serverLog(serverID, userID, " UDP reply sent " + method + " ", " appointmentID: " + appointmentID + " appointmentType: " + appointmentType + " ", sendingResult);
                }
            } catch (SocketException e) {
                System.err.println("SocketException: " + e);
                e.printStackTrace(System.out);
            } catch (IOException e) {
                System.err.println("IOException: " + e);
                e.printStackTrace(System.out);
            }
        }

    }
}
