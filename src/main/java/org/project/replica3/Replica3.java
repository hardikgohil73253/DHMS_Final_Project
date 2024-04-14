package org.project.replica3;


import org.project.replica3.interfaces.WebServiceInterface;
import org.project.utils.VariableStore;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Replica3 {
    public static int lastSequenceID = 1;
    public static ConcurrentHashMap<Integer, Message> message_list = new ConcurrentHashMap<>();
    public static Queue<Message> message_q = new ConcurrentLinkedQueue<Message>();
    public static Service montrealWebService;
    public static Service sherbrookWebService;
    public static Service quebecWebService;
    private static boolean serversFlag = true;
    private static WebServiceInterface webServiceInterface;

    public static void main(String[] args) throws Exception {
        Run();
        runServers();
    }

    private static void Run() throws Exception {
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

    private static void receive()  {
        DatagramSocket socket = null;
        try {

            socket = new DatagramSocket(9119);


            byte[] buffer = new byte[1000];
            System.out.println("RM3 UDP Server Started.");

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
                    11-Rm1 has bug
                    12-Rm2 has bug
                    13-Rm3 has bug
                    21-Rm1 is down
                    22-Rm2 is down
                    23-Rm3 is down
                */
                System.out.println("RM3 recieved message. Detail:" + data);
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
                    System.out.println("is adding queue:" + message);
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
                    System.out.println("RM3 has bug:" + message.toString());
                } else if (parts[2].equalsIgnoreCase("13")) {
                    Message message = message_obj_create(data);
                    System.out.println("RM3 has bug:" + message.toString());
                } else if (parts[2].equalsIgnoreCase("22")) {
                    Runnable crash_task = () -> {
                        try {
                            //suspend the execution of messages untill all servers are up. (serversFlag=false)
                            serversFlag = false;
                            //wait untill are servers are up
                            Thread.sleep(5000);

                            System.out.println("RM3 is reloading servers hashmap");
                            reloadServers();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                    Thread handleThread = new Thread(crash_task);
                    handleThread.start();
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

    // Create a list of messsages, seperating them with @ and send it back to RM
    private static void initial_send_list(Integer begin, Integer end, String RmNumber) {
        String list = "";
        for (ConcurrentHashMap.Entry<Integer, Message> entry : message_list.entrySet()) {
            if (entry.getValue().sequenceId > begin && entry.getValue().sequenceId < end) {
                list += entry.getValue().toString() + "@";
            }
        }
        // Remove the last @ character
        if (list.length() > 2)
            list.substring(list.length() - 1);
        Message message = new Message(0, list, "03", begin.toString(), end.toString(), RmNumber, "Null", "Null", "Null", 0);
        System.out.println("RM3 sending its list of messages for initialization. list of messages:" + list);
        send_multicast_toRM(message);
    }

    //update the hasmap and and new data to queue to be execited
    private static void update_message_list(String data) {
        String[] parts = data.split("@");
        for (int i = 0; i < parts.length; ++i) {
            Message message = message_obj_create(parts[i]);
            //we get the list from 2 other RMs and will ensure that there will be no duplication
            if (!message_list.containsKey(message.sequenceId)) {
                System.out.println("RM3 update its message list" + message);
                message_q.add(message);
                message_list.put(message.sequenceId, message);
            }
        }
    }

    private static void send_multicast_toRM(Message message) {
        int port = 9119;
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            byte[] data = message.toString().getBytes();
            InetAddress aHost = InetAddress.getByName(VariableStore.INET_MULTICAST_ADDRESS);

            DatagramPacket request = new DatagramPacket(data, data.length, aHost, port);
            socket.send(request);
            System.out.println("Message multicasted from RM3 to other RMs. Detail:" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Execute all request from the lastSequenceID, send the response back to Front and update the counter(lastSequenceID)
    private static void
    executeAllRequests() throws Exception {
        System.out.println("before while true");
        while (true) {
            synchronized (Replica3.class) {
                Iterator<Message> itr = message_q.iterator();
                while (itr.hasNext()) {
                    Message data = itr.next();
                    System.out.println("RM3 is executing message request. Detail:" + data);
                    //when the servers are down serversFlag is False therefore, no execution untill all servers are up.
                    if (data.sequenceId == lastSequenceID && serversFlag) {
                        System.out.println("RM3 is executing message request. Detail:" + data);
                        String response = requestToServers(data);
                        Message message = new Message(data.sequenceId, response, "RM3",
                                data.Function, data.userID, data.newAppointmentID,
                                data.newAppointmentType, data.oldAppointmentID,
                                data.oldAppointmentType, data.bookingCapacity);
                        lastSequenceID += 1;
                        messsageToFront(message.toString(), data.FrontIpAddress);
                        message_q.poll();
                    }
                }
//                message_q.clear();
            }
        }
    }

    //Send RMI request to server
    private static String requestToServers(Message input) {
        int portNumber = serverPort(input.userID.substring(0, 3));
        try {
            String url = "http://" + VariableStore.INET_MULTICAST_ADDRESS + ":" ;
            URL montrealURL = new URL(url + 7177 + "/"+ "montreal?wsdl");
            QName montrealQName = new QName("http://controllers.replica3.project.org/", "AppointmentControllerService");
            montrealWebService = Service.create(montrealURL, montrealQName);

            URL quebecURL = new URL(url + 7179 + "/"+ "quebec?wsdl");
            QName quebecQName = new QName("http://controllers.replica3.project.org/", "AppointmentControllerService");
            quebecWebService = Service.create(quebecURL, quebecQName);

            URL sherbrookURL = new URL(url + 7178 + "/"+ "sherbrook?wsdl");
            QName sherbrookQName = new QName("http://controllers.replica3.project.org/", "AppointmentControllerService");
            sherbrookWebService = Service.create(sherbrookURL, sherbrookQName);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        initialiseWebService(input.userID);

        if (input.userID.substring(3, 4).equalsIgnoreCase("A")) {
            if (input.Function.equalsIgnoreCase("addAppointment")) {
                String response = webServiceInterface.addAppointment(input.newAppointmentID, input.newAppointmentType, input.bookingCapacity);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("removeAppointment")) {
                String response = webServiceInterface.removeAppointment(input.newAppointmentID, input.newAppointmentType);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("listAppointmentAvailability")) {
                String response = webServiceInterface.listAppointmentAvailability(input.newAppointmentType);
                System.out.println(response);
                return response;
            }
        } else if (input.userID.substring(3, 4).equalsIgnoreCase("P")) {
            if (input.Function.equalsIgnoreCase("bookAppointment")) {
                String response = webServiceInterface.bookAppointment(input.userID, input.newAppointmentID, input.newAppointmentType);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("getAppointmentSchedule")) {
                String response = webServiceInterface.getAppointmentSchedule(input.userID);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("cancelAppointment")) {
                String response = webServiceInterface.cancelAppointment(input.userID, input.newAppointmentID);
                System.out.println(response);
                return response;
            } else if (input.Function.equalsIgnoreCase("swapAppointment")) {
                String response = webServiceInterface.swapAppointment(input.userID,  input.oldAppointmentID, input.oldAppointmentType,input.newAppointmentID, input.newAppointmentType);
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
            portNumber = 7179;
        else if (branch.equalsIgnoreCase("mtl"))
            portNumber = 7177;
        else if (branch.equalsIgnoreCase("she"))
            portNumber = 7178;

        return portNumber;
    }

    public static void messsageToFront(String message, String FrontIpAddress) {
        System.out.println("Message to front:" + message);
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(6744);
            byte[] bytes = message.getBytes();
            InetAddress aHost = InetAddress.getByName(FrontIpAddress);

            System.out.println(aHost);
            DatagramPacket request = new DatagramPacket(bytes, bytes.length, aHost, VariableStore.FRONT_END_PORT);
            socket.send(request);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

    }

    public static void reloadServers() throws Exception {
        for (ConcurrentHashMap.Entry<Integer, Message> entry : message_list.entrySet()) {
            if (entry.getValue().sequenceId < lastSequenceID)
                requestToServers(entry.getValue());
        }
    }

    private static String initialiseWebService(String userID) {
        String branchAcronym = userID.substring(0, 3);
        if (branchAcronym.equalsIgnoreCase("MTL")) {
            webServiceInterface = montrealWebService.getPort(WebServiceInterface.class);
            return branchAcronym;
        } else if (branchAcronym.equalsIgnoreCase("SHE")) {
            webServiceInterface = sherbrookWebService.getPort(WebServiceInterface.class);
            return branchAcronym;
        } else if (branchAcronym.equalsIgnoreCase("QUE")) {
            webServiceInterface = quebecWebService.getPort(WebServiceInterface.class);
            return branchAcronym;
        }
        return "1";
    }

    private static void runServers(){
        //Initiate three servers MTL, SHE and QUE
        Runnable task1 = () -> {
            try {
                new Server(VariableStore.SERVERS.MTL);
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        };
        Runnable task2 = () -> {
            try {
                new Server(VariableStore.SERVERS.QUE);
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        };
        Runnable task3 = () -> {
            try {
                new Server(VariableStore.SERVERS.SHE);
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        };
        Thread thread1 = new Thread(task1);
        thread1.start();
        Thread thread2 = new Thread(task2);
        thread2.start();
        Thread thread3 = new Thread(task3);
        thread3.start();
    }
}

