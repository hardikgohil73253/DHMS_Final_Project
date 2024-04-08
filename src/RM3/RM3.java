package RM3;


import RM3.RMmodel.Message;
import RM3.Server3;

import java.io.IOException;
import java.net.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RM3 {
    private static final String Bug_ID = "MTLM8888";
    private static final String Crash_ID = "MTLM9999";

    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static int lastSequenceID = 1;
    public static int bug_counter = 0;
    public static ConcurrentHashMap<Integer, Message> message_list = new ConcurrentHashMap<Integer, Message>();
    public static Queue<Message> message_q = new ConcurrentLinkedQueue<>();
    private static boolean serversFlag = true;
    private static boolean BugFlag = false;

    public static void main(String[] args) throws Exception {
        Run();
    }

    private static void Run() {
        Runnable task = () -> {
            try {
                receive();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    private static void receive() throws Exception {
        MulticastSocket socket = null;
        try {
            NetworkInterface ni = NetworkInterface.getByName("en0");
            socket = new MulticastSocket(1234);
            socket.setInterface(ni.getInetAddresses().nextElement());
            socket.joinGroup(InetAddress.getByName("230.1.1.10"));

            byte[] buffer = new byte[1000];
            System.out.println("RM3 UDP Server Started(port=1234)............");

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
                    11-RM3 has bug
                    12-Rm2 has bug
                    13-Rm3 has bug
                    21-RM3 is down
                    22-Rm2 is down
                    23-Rm3 is down
                */
                System.out.println("RM3 received message. Detail:" + data);
                if (parts[2].equalsIgnoreCase("00")) {
                    Message message = message_obj_create(data);
                    Message message_To_RMs = message_obj_create(data);
                    message_To_RMs.MessageType = "01";
                    send_multicast_toRM(message_To_RMs);
                    if (message.sequenceId - lastSequenceID > 1) {
                        Message initial_message = new Message(0, "Null", "02", Integer.toString(lastSequenceID), Integer.toString(message.sequenceId), "RM3", "Null", "Null", "Null", 0);
                        System.out.println("RM3 send request to update its message list. from:" + lastSequenceID + "To:" + message.sequenceId);
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
                } else if (parts[2].equalsIgnoreCase("03") && parts[5].equalsIgnoreCase("RM3")) {
                    update_message_list(parts[1]);
                } else if (parts[2].equalsIgnoreCase("11")) {
                    Message message = message_obj_create(data);
                    System.out.println("RM3 has bug:" + message.toString());
                } else if (parts[2].equalsIgnoreCase("12")) {
                    Message message = message_obj_create(data);
                    System.out.println("Rm2 has bug:" + message.toString());
                } else if (parts[2].equalsIgnoreCase("13")) {
                    Message message = message_obj_create(data);
                    System.out.println("Rm3 has bug:" + message.toString());
                } else if (parts[2].equalsIgnoreCase("14")) {
                    BugFlag = true;
                    Message message = message_obj_create(data);
                    System.out.println("RM3 has bug:" + message.toString());
                    System.out.println(ANSI_RED_BACKGROUND +"RM3 is terminated" + ANSI_RESET);
                }
                else if (parts[2].equalsIgnoreCase("24")) {
                    Thread handleThread = getHandleThread();
                    handleThread.join();
                    System.out.println("RM3 handled the crash!");
                    serversFlag = true;
                }
            }

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (socket != null)
                socket.close();
        }
    }

    private static Thread getHandleThread() {
        Runnable crash_task = () -> {
            try {

                System.out.println("RM3 is going to shutdown");

                Server3.main(new String[0]);
                Thread.sleep(500);

                System.out.println("RM3 is reloading servers hashmap");
                reloadServers();

            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Thread handleThread = new Thread(crash_task);
        handleThread.start();
        return handleThread;
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
        System.out.println("RM3 sending its list of messages for initialization. list of messages:" + list);
        send_multicast_toRM(message);
    }

    private static void update_message_list(String data) {
        String[] parts = data.split("@");
        for (String part : parts) {
            Message message = message_obj_create(part);
            //we get the list from 2 other RMs and will ensure that there will be no duplication
            if (!message_list.containsKey(message.sequenceId)) {
                System.out.println("RM3 update its message list" + message);
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
            System.out.println("Message multicasted from RM3 to other RMs. Detail:" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Execute all request from the lastSequenceID, send the response back to Front and update the counter(lastSequenceID)
    private static void executeAllRequests() throws Exception {
        System.out.println("before while true");
        while (true) {
            synchronized (RM3.class) {
                for (Message data : message_q) {
                    //when the servers are down serversFlag is False therefore, no execution untill all servers are up.
                    if (data.sequenceId == lastSequenceID && serversFlag) {
                        if (BugFlag) {
//                            if (bug_counter == 0)
                            System.out.println(ANSI_RED_BACKGROUND + "RM3 is terminated" + ANSI_RESET);
//                            requestToServers(data);
                            Message bug_message = new Message(data.sequenceId, "Null", "RM3",
                                    data.Function, data.userID, data.newAppointmentID,
                                    data.newAppointmentType, data.oldAppointmentID,
                                    data.oldAppointmentType, data.bookingCapacity);
//                            bug_counter += 1;
                            lastSequenceID += 1;
//                            messsageToFront(bug_message.toString(), data.FrontIpAddress);
                            message_q.poll();
                        } else {
                            System.out.println("RM3 is executing message request. Detail:" + data);
                            String response = requestToServers(data);

                            System.out.println(data);

                            Message message = new Message(data.sequenceId, response, "RM3", data.Function, data.userID, data.newAppointmentID, data.newAppointmentType, data.oldAppointmentID, data.oldAppointmentType, data.bookingCapacity);
                            lastSequenceID += 1;

                            System.out.println(message.toString());
                            messsageToFront(message.toString(), data.FrontIpAddress);
                            message_q.poll();
                        }
                    }
                }
            }
        }
    }

    //Send RMI request to server
    private static String requestToServers(Message input) throws Exception {
        String serverName = input.userID.substring(0, 3);
        System.out.println(input.userID.substring(0, 3));
        int port = 0;

        if(serverName.equalsIgnoreCase("MTL")) {
            port = 3211;
        } else if(serverName.equalsIgnoreCase("QUE")) {
            port = 3212;
        } else if(serverName.equalsIgnoreCase("SHE")) {
            port = 3213;
        }

        try (DatagramSocket aSocket = new DatagramSocket()) {
            byte[] m = input.toString().getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(m, m.length, aHost, port);
            aSocket.send(request);
            System.out.println("RM3 sending request to server:" + ' ' + port);

            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            aSocket.receive(reply);

            String response = new String(reply.getData(), 0,
                    reply.getLength());
            System.out.println("RM3 received response from server: " + response);
            return response;
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
        return "Null response from server" + input.userID.substring(0, 3);
    }

    public static void messsageToFront(String message, String FrontIpAddress) {
        System.out.println("Message to front:" + message);
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

    public static void reloadServers() throws Exception {
        for (ConcurrentHashMap.Entry<Integer, Message> entry : message_list.entrySet()) {
            System.out.println("Recovery Mood-RM3 is executing message request. Detail:" + entry.getValue().toString());
            requestToServers(entry.getValue());
            if (entry.getValue().sequenceId >= lastSequenceID)
                lastSequenceID = entry.getValue().sequenceId + 1;
        }
        message_q.clear();
    }
}