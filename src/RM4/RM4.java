package RM4;

import RM4.Server4;
import RM4.RMmodel.Message;
import RM4.webcontroller.webInterface;
import controller.frontendController.webServiceInterface;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RM4 {
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static int lastSequenceID = 1;
    public static Service frontendService;
    private static webInterface obj;
    public static ConcurrentHashMap<Integer, Message> message_list = new ConcurrentHashMap<>();
    public static Queue<Message> message_q = new ConcurrentLinkedQueue<Message>();
    private static boolean serversFlag = true;

    public static void main(String[] args) throws Exception {
        Run();
    }

    private static void Run() {
        Runnable task = () -> {
            try {
                receive();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    private static void receive() throws Exception {
        try (MulticastSocket socket = new MulticastSocket(1234)) {
//            NetworkInterface ni = NetworkInterface.getByName("en0");
            //            socket.setInterface(ni.getInetAddresses().nextElement());
            socket.joinGroup(InetAddress.getByName("230.1.1.10"));

            byte[] buffer = new byte[1000];
            System.out.println("RM4 UDP Server Started(port=1234)............");

            //Run thread for executing all messages in queue
            Runnable task = () -> {
                try {
                    executeAllRequests();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            Thread thread = new Thread(task);
            thread.start();

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                String data = new String(request.getData(), 0, request.getLength());

                String[] parts = data.split(";");

                /*
                Message Types:
                    00- Simple message
                    01- Sync request between the RMs
                    02- Initialing RM
                    11-RM4 has bug
                    12-Rm2 has bug
                    13-Rm3 has bug
                    21-RM4 is down
                    22-Rm2 is down
                    23-Rm3 is down
                */
                System.out.println("RM4 received message. Detail:" + data);
                if (parts[2].equalsIgnoreCase("00")) {
                    Message message = message_obj_create(data);
                    Message message_To_RMs = message_obj_create(data);
                    message_To_RMs.MessageType = "01";
                    send_multicast_toRM(message_To_RMs);
                    if (message.sequenceId - lastSequenceID > 1) {
                        Message initial_message = new Message(0, "Null", "02", Integer.toString(lastSequenceID), Integer.toString(message.sequenceId), "RM4", "Null", "Null", "Null", 0);
                        System.out.println("RM4 send request to update its message list. from:" + lastSequenceID + "To:" + message.sequenceId);
                        // Request all RMs to send back list of messages
                        send_multicast_toRM(initial_message);
                    }
                    System.out.println("is adding queue:" + message + "|| lastSequence>>>" + lastSequenceID);
                    message_q.add(message);
                    message_list.put(message.sequenceId, message);
                } else if (parts[2].equalsIgnoreCase("01")) {
                    Message message = message_obj_create(data);
                    if (!message_list.contains(message.sequenceId))
                        message_list.put(message.sequenceId, message);
                } else if (parts[2].equalsIgnoreCase("02")) {
                    initial_send_list(Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), parts[5]);
                } else if (parts[2].equalsIgnoreCase("03") && parts[5].equalsIgnoreCase("RM4")) {
                    update_message_list(parts[1]);
                } else if (parts[2].equalsIgnoreCase("11")) {
                    Message message = message_obj_create(data);
                    System.out.println("RM1 has bug:" + message.toString());
                } else if (parts[2].equalsIgnoreCase("12")) {
                    Message message = message_obj_create(data);
                    System.out.println("Rm2 has bug:" + message.toString());
                } else if (parts[2].equalsIgnoreCase("13")) {
                    Message message = message_obj_create(data);
                    System.out.println("Rm3 has bug:" + message.toString());
                } else if (parts[2].equalsIgnoreCase("14")) {
                    Message message = message_obj_create(data);
                    System.out.println("Rm4 has bug:" + message.toString());
                    System.out.println(ANSI_RED_BACKGROUND + "RM4 is terminated" + ANSI_RESET);
                } else if (parts[2].equalsIgnoreCase("24")) {
                    Runnable crash_task = () -> {
                        try {

                            System.out.println("RM4 is going to shutdown");

                            Server4.main(new String[0]);
                            Thread.sleep(500);

                            System.out.println("RM4 is reloading servers hashmap");
                            reloadServers();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                    Thread handleThread = new Thread(crash_task);
                    handleThread.start();
                    handleThread.join();
                    System.out.println("RM4 handled the crash!");
                    serversFlag = true;
                }
            }

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }

    private static Message message_obj_create(String data) {
        String[] parts = data.split(";");
        int sequenceId = Integer.parseInt(parts[0]);
        String FrontIpAddress = parts[1];
        String MessageType = parts[2];
        String Function = parts[3];
        String userID = parts[4];
        String newAppointmentID = parts[5];
        String newAppointmentType = parts[6];
        String oldAppointmentID = parts[7];
        String oldAppointmentType = parts[8];
        int bookingCapacity = Integer.parseInt(parts[9]);
        Message message = new Message(sequenceId, FrontIpAddress, MessageType, Function, userID, newAppointmentID, newAppointmentType, oldAppointmentID, oldAppointmentType, bookingCapacity);
        return message;
    }

    // Create a list of messages, separating them with @ and send it back to RM
    private static void initial_send_list(Integer begin, Integer end, String RmNumber) {
        String list = "";
        for (ConcurrentHashMap.Entry<Integer, Message> entry : message_list.entrySet()) {
            if (entry.getValue().sequenceId > begin && entry.getValue().sequenceId < end) {
                list += entry.getValue().toString() + "@";
            }
        }
        // Remove the last @ character
        if (list.endsWith("@"))
            list.substring(list.length() - 1);
        Message message = new Message(0, list, "03", begin.toString(), end.toString(), RmNumber, "Null", "Null", "Null", 0);
        System.out.println("RM4 sending its list of messages for initialization. list of messages:" + list);
        send_multicast_toRM(message);
    }

    private static void update_message_list(String data) {
        String[] parts = data.split("@");
        for (String part : parts) {
            Message message = message_obj_create(part);
            //we get the list from 2 other RMs and will ensure that there will be no duplication
            if (!message_list.containsKey(message.sequenceId)) {
                System.out.println("RM4 update its message list" + message);
                message_q.add(message);
                message_list.put(message.sequenceId, message);
            }
        }
    }

    private static void send_multicast_toRM(Message message) {
        int port = 1234;
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            byte[] data = message.toString().getBytes();
            InetAddress aHost = InetAddress.getByName("230.1.1.10");

            DatagramPacket request = new DatagramPacket(data, data.length, aHost, port);
            socket.send(request);
            System.out.println("Message multicasted from RM4 to other RMs. Detail:" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Execute all request from the lastSequenceID, send the response back to Front and update the counter(lastSequenceID)
    private static void executeAllRequests() throws Exception {
        System.out.println("before while true");
        while (true) {
            synchronized (RM4.class) {
                Iterator<Message> itr = message_q.iterator();
                while (itr.hasNext()) {
                    Message data = itr.next();
                    System.out.println("RM4 is executing message request. Detail:" + data);
                    //when the servers are down serversFlag is False therefore, no execution untill all servers are up.
                    if (data.sequenceId == lastSequenceID && serversFlag) {
                        System.out.println("RM4 is executing message request. Detail:" + data);
                        String response = requestToServers(data);
                        Message message = new Message(data.sequenceId, response, "RM4",
                                data.Function, data.userID, data.newAppointmentID,
                                data.newAppointmentType, data.oldAppointmentID,
                                data.oldAppointmentType, data.bookingCapacity);
                        lastSequenceID += 1;
                        messsageToFront(message.toString(), data.FrontIpAddress);
                        message_q.poll();
                    }
                }
            }
        }
//        while (true) {
//            synchronized (RM4.class) {
//                for (Message data : message_q) {
//                    //when the servers are down serversFlag is False therefore, no execution untill all servers are up.
//                    if (data.sequenceId == lastSequenceID && serversFlag) {
//                        if (BugFlag) {
////                            if (bug_counter == 0)
//                            System.out.println(ANSI_RED_BACKGROUND + "RM4 is terminated" + ANSI_RESET);
////                            requestToServers(data);
//                            Message bug_message = new Message(data.sequenceId, "Null", "RM4",
//                                    data.Function, data.userID, data.newAppointmentID,
//                                    data.newAppointmentType, data.oldAppointmentID,
//                                    data.oldAppointmentType, data.bookingCapacity);
////                            bug_counter += 1;
//                            lastSequenceID += 1;
////                            messsageToFront(bug_message.toString(), data.FrontIpAddress);
//                            message_q.poll();
//                        } else {
//                            System.out.println("RM4 is executing message request. Detail:" + data);
//                            String response = requestToServers(data);
//
//                            System.out.println(data);
//
//                            Message message = new Message(data.sequenceId, response, "RM4", data.Function, data.userID, data.newAppointmentID, data.newAppointmentType, data.oldAppointmentID, data.oldAppointmentType, data.bookingCapacity);
//                            lastSequenceID += 1;
//
//                            System.out.println(message.toString());
//                            messsageToFront(message.toString(), data.FrontIpAddress);
//                            message_q.poll();
//                        }
//                    }
//                }
//            }
//        }
    }

    //Send RMI request to server
    private static String requestToServers(Message input) throws Exception {
//        String serverName = input.userID.substring(0, 3);
//        System.out.println(input.userID.substring(0, 3));
//        int port = 0;
//
//        if(serverName.equalsIgnoreCase("MTL")) {
//            port = 4211;
//        } else if(serverName.equalsIgnoreCase("QUE")) {
//            port = 4212;
//        } else if(serverName.equalsIgnoreCase("SHE")) {
//            port = 4213;
//        }
//
//        try (DatagramSocket aSocket = new DatagramSocket()) {
//            byte[] m = input.toString().getBytes();
//            InetAddress aHost = InetAddress.getByName("localhost");
//            DatagramPacket request = new DatagramPacket(m, m.length, aHost, port);
//            aSocket.send(request);
//            System.out.println("RM4 sending request to server:" + ' ' + port);
//
//            byte[] buffer = new byte[1000];
//            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
//            aSocket.receive(reply);
//
//            String response = new String(reply.getData(), 0,
//                    reply.getLength());
//            System.out.println("RM4 received response from server: " + response);
//            return response;
//        } catch (SocketException e) {
//            System.out.println("Socket: " + e.getMessage());
//        } catch (IOException e) {
//            System.out.println("IO: " + e.getMessage());
//        }
//        int portNumber = serverPort(input.userID.substring(0, 3));
//        Registry registry = LocateRegistry.getRegistry(portNumber);
//        Manager obj = (Manager) registry.lookup("Function");
        URL frontendURL = new URL("http://localhost:8080/frontend?wsdl");
        QName frontendQName = new QName("http://implementation.frontendController.controller/", "FrontendService");
        frontendService = Service.create(frontendURL, frontendQName);
        obj = frontendService.getPort(webInterface.class);
        String bookingServ = input.userID.substring(0, 3).toUpperCase();
        if (input.userID.substring(3, 4).equalsIgnoreCase("M")) {
            if (input.Function.equalsIgnoreCase("addEvent")) {
                String response = obj.addAppointment(input.newAppointmentID, input.newAppointmentType, input.bookingCapacity);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("removeEvent")) {
                String response = obj.removeAppointment(input.newAppointmentID, input.newAppointmentType);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("listEventAvailability")) {
                String response = obj.listAppointmentAvailability(input.newAppointmentType);
                System.out.println(response);
                return response;
            }
        } else if (input.userID.substring(3, 4).equalsIgnoreCase("C")) {
            if (input.Function.equalsIgnoreCase("bookEvent")) {
                String response = obj.bookAppointment(input.userID, input.newAppointmentID, input.newAppointmentType);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("getBookingSchedule")) {
                String response = obj.getBookingSchedule(input.userID);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("cancelEvent")) {
                String response = obj.cancelAppointment(input.userID, input.newAppointmentID);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("swapEvent")) {
                String response = obj.swapAppointment(input.userID, input.newAppointmentID, input.newAppointmentType, input.oldAppointmentID, input.oldAppointmentType);
                System.out.println(response);
                return response;
            }
        }
        return "Null response from server" + input.userID.substring(0, 3);
    }


    private static int serverPort(String input) {
        String branch = input.substring(0, 3);
        int portNumber = -1;

        if (branch.equalsIgnoreCase("que"))
            portNumber = 4211;
        else if (branch.equalsIgnoreCase("mtl"))
            portNumber = 4212;
        else if (branch.equalsIgnoreCase("she"))
            portNumber = 4213;

        return portNumber;
    }

    public static void messsageToFront(String message, String FrontIpAddress) {
        try (DatagramSocket socket = new DatagramSocket(4324)) {
            byte[] bytes = message.getBytes();
            InetAddress aHost = InetAddress.getByName(FrontIpAddress);

            System.out.println(aHost);
            DatagramPacket request = new DatagramPacket(bytes, bytes.length, aHost, 1999);
            socket.send(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static void reloadServers() throws Exception {
//        for (ConcurrentHashMap.Entry<Integer, Message> entry : message_list.entrySet()) {
//            System.out.println("Recovery Mood-RM4 is executing message request. Detail:" + entry.getValue().toString());
//            requestToServers(entry.getValue());
//            if (entry.getValue().sequenceId >= lastSequenceID)
//                lastSequenceID = entry.getValue().sequenceId + 1;
//        }
//        message_q.clear();
//    }
    public static void reloadServers() throws Exception {
        for (ConcurrentHashMap.Entry<Integer, Message> entry : message_list.entrySet()) {
            if (entry.getValue().sequenceId < lastSequenceID)
                requestToServers(entry.getValue());
        }
    }
}