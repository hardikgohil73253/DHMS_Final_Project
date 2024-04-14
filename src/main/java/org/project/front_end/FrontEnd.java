package org.project.front_end;

import org.project.interfaces.FrontEndInterface;
import org.project.utils.VariableStore;

import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class FrontEnd {
    private static final int sequencerPort = VariableStore.SEQUENCER_PORT;
    private static final String sequencerIP = VariableStore.SEQUENCER_IP;
    private static final int FE_SQ_PORT = 8311;
    public static String FE_IP_Address = "localhost";

    public static void main(String[] args) {
        try {
            FrontEndInterface inter = new FrontEndInterface() {
                @Override
                public void informRmHasBug(int RmNumber) {
                    ClientRequest errorMessage = new ClientRequest(RmNumber, "1");
                    System.out.println("Rm:" + RmNumber + "has bug");
                    sendUnicastToSequencer(errorMessage);
                }
                @Override
                public void informRmIsDown(int RmNumber) {
                    ClientRequest errorMessage = new ClientRequest(RmNumber, "2");
                    System.out.println("Rm:" + RmNumber + "is down");
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
            FrontEndImplementation servant = new FrontEndImplementation(inter);
            String url = "http://" + "localhost" + ":" + VariableStore.FRONT_END_PORT + "/FrontEnd";
            Endpoint endpoint = Endpoint.publish(url, servant);
            System.out.println("FrontEnd Server is Up & Running");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

    private static int sendUnicastToSequencer(ClientRequest requestFromClient) {
        DatagramSocket aSocket = null;
        String dataFromClient = requestFromClient.toString();
        System.out.println("FE: ---- sendUnicastToSequencer>>>" + dataFromClient);
        int sequenceID = 0;
        try {
            aSocket = new DatagramSocket(FE_SQ_PORT);
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName(sequencerIP);
            DatagramPacket requestToSequencer = new DatagramPacket(message, dataFromClient.length(), aHost, sequencerPort);

            aSocket.send(requestToSequencer);

            aSocket.setSoTimeout(1000);
            byte[] buffer = new byte[1000];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            aSocket.receive(response);
            String sentence = new String(response.getData(), 0, response.getLength());
            System.out.println("FE:---- sendUnicastToSequencer/ResponseFromSequencer>>>" + sentence);
            sequenceID = Integer.parseInt(sentence.trim());
            System.out.println("FE: ----- sendUnicastToSequencer/ResponseFromSequencer>>>SequenceID:" + sequenceID);
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
}
