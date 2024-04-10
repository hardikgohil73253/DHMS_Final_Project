package FrontEnd;

import controller.frontendController.implementation.FrontendImplementation;
import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import controller.frontendController.FEInterface;

public class FrontEnd {
    private static final int SEQUENCER_PORT = 1333;
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";
    //    private static final String SEQUENCER_IP = "192.168.2.17";
    private static final String SEQUENCER_IP = "localhost";
    private static final String RM_Multicast_group_address = "230.1.1.10";
    private static final int RM_Multicast_Port = 1234;
    private static final int FE_SQ_PORT = 1414;
    private static final int FE_PORT = 1999;

//    public static String FE_IP_Address = "192.168.2.11";
    public static String FE_IP_Address = "localhost";

    public static void main(String[] args) throws Exception {
        try {
//            String serverID = "FrontEnd";
//            String serverName = "FrontendImplementation";
//            FrontendImplementation service = new FrontendImplementation(serverID,serverName);
            FEInterface inter = new FEInterface() {
                @Override
                public void informRmHasBug(int RmNumber) {
//                    String errorMessage = new MyRequest(RmNumber, "1").toString();
                    ClientRequest errorMessage = new ClientRequest(RmNumber, "1");
                    System.out.println("Rm:" + RmNumber + "has bug");
//                    sendMulticastFaultMessageToRms(errorMessage);
                    sendUnicastToSequencer(errorMessage);
                }

                @Override
                public void informRmIsDown(int RmNumber) {
//                    String errorMessage = new MyRequest(RmNumber, "2").toString();
                    ClientRequest errorMessage = new ClientRequest(RmNumber, "2");
                    System.out.println("Rm:" + RmNumber + "is down");
//                    sendMulticastFaultMessageToRms(errorMessage);
                    sendUnicastToSequencer(errorMessage);
                }

                @Override
                public int sendRequestToSequencer(ClientRequest myRequest) {
                    return sendUnicastToSequencer(myRequest);
                }

                @Override
                public void retryRequest(ClientRequest myRequest) {
                    System.out.println("No response from all Rms, Retrying request...");
                    sendUnicastToSequencer(myRequest);
                }
            };
            FrontendImplementation service = new FrontendImplementation(inter);
            System.out.println("FrontendImplementation Server Starting.....");
            String serverEndpoint = "http://localhost:8080/Frontend";
            Endpoint endpoint = Endpoint.publish(serverEndpoint, service);
            System.out.println( " Server is Up & Running");
//            Runnable task = () -> {
//                listenForUDPResponses(service);
//            };
//            Thread thread = new Thread(task);
//            thread.start();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public static int sendUnicastToSequencer(ClientRequest requestFromClient) {
        DatagramSocket aSocket = null;
        String dataFromClient = requestFromClient.toString();

        System.out.println( ANSI_GREEN_BACKGROUND +"FrontendImplementation Uni casting dataFromClient To Sequencer ---> " + ANSI_RESET + dataFromClient);

        int sequenceID = 0;

        try {
            aSocket = new DatagramSocket(FE_SQ_PORT);
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName(SEQUENCER_IP);
            DatagramPacket requestToSequencer = new DatagramPacket(message, dataFromClient.length(), aHost, SEQUENCER_PORT);
            aSocket.send(requestToSequencer);
            aSocket.setSoTimeout(1000);
            // Set up UPD packet for receiving
            byte[] buffer = new byte[1000];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            // Try to receive the response from the ping
            aSocket.receive(response);
            String sentence = new String(response.getData(), 0, response.getLength());
            sequenceID = Integer.parseInt(sentence.trim());
            System.out.println( ANSI_GREEN_BACKGROUND + "FrontendImplementation received response from Sequencer <--- SequenceID:" + ANSI_RESET + sequenceID);
        } catch (SocketException e) {
            System.out.println("Failed: " + requestFromClient.noRequestSendError());
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Failed: " + requestFromClient.noRequestSendError());
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
        return sequenceID;
    }

//    public static void sendMulticastFaultMessageToRms(String errorMessage) {
//        DatagramSocket aSocket = null;
//        try {
//            aSocket = new DatagramSocket();
//            byte[] messages = errorMessage.getBytes();
//            InetAddress aHost = InetAddress.getByName(RM_Multicast_group_address);
//
//            DatagramPacket request = new DatagramPacket(messages, messages.length, aHost, RM_Multicast_Port);
//            System.out.println("FrontEnd:sendMulticastFaultMessageToRms>>>" + errorMessage);
//            aSocket.send(request);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }

//    private static void listenForUDPResponses(FrontendImplementation service) {
//
//        System.out.println("listening for UDP message from RM through UDP multicast...");
//        DatagramSocket aSocket = null;
//        try {
//            InetAddress desiredAddress = InetAddress.getByName(FE_IP_Address);
//
//            aSocket = new DatagramSocket(FE_PORT, desiredAddress);
//            byte[] buffer = new byte[1000];
//            System.out.println("FrontEnd Server Started fro listening UDPResponses " + desiredAddress + ":" + FE_PORT + "............");
//
//            while (true) {
//                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
//                aSocket.receive(response);
//                String sentence = new String(response.getData(), 0, response.getLength()).trim();
//                String[] parts = sentence.split(";");
//                System.out.println(ANSI_GREEN_BACKGROUND + "FrontendImplementation received Response received from <--- "+ parts[2]+ "::" +ANSI_RESET + sentence);
//                ResponseFromRM rs = new ResponseFromRM(sentence);
//                System.out.println("Adding response to FrontEndImplementation:");
//                service.addReceivedResponse(rs);
//            }
//        } catch (SocketException e) {
//            System.out.println("Socket: " + e.getMessage());
//        } catch (IOException e) {
//            System.out.println("IO: " + e.getMessage());
//        }finally {
//            if (aSocket != null)
//                aSocket.close();
//        }
//    }
}