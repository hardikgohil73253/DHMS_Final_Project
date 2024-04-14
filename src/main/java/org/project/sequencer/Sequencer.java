package org.project.sequencer;

import org.project.utils.VariableStore;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import static org.project.utils.VariableStore.*;


public class Sequencer {
    private static int SEQUENCER_ID = 0;
    private static final String sequencerIP = "localhost";

    public static void main(String[] args) {
        try (DatagramSocket aSocket = new DatagramSocket(SEQUENCER_PORT, InetAddress.getByName(sequencerIP))) {
            byte[] buffer = new byte[1000];
            System.out.println("Sequencer Server Started ... /// ... :)");
            while (true) {
                DatagramPacket packetRequest = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(packetRequest);
                String sentence = new String(packetRequest.getData(), 0, packetRequest.getLength());
                String[] parts = sentence.split(";");
                int SEQUENCER_ID1 = Integer.parseInt(parts[0]);
                String ip = packetRequest.getAddress().getHostAddress();
                String sentence1 = ip + ";" + parts[2] + ";" + parts[3] + ";" + parts[4] + ";" + parts[5] + ";" + parts[6] + ";" + parts[7] + ";" + parts[8] + ";" + parts[9] + ";";
                System.out.println(sentence1);
                sendMessage(sentence1, SEQUENCER_ID1, parts[2].equalsIgnoreCase("00"));

                byte[] SeqId = (Integer.toString(SEQUENCER_ID)).getBytes();
                InetAddress aHost1 = packetRequest.getAddress();
                int port1 = packetRequest.getPort();

                System.out.println(aHost1 + ":" + port1);
                DatagramPacket packetRequest1 = new DatagramPacket(SeqId, SeqId.length, aHost1, port1);
                aSocket.send(packetRequest1);
            }
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }

    public static void sendMessage(String message, int SEQUENCER_ID1, boolean isPacketRequest) {

        if (SEQUENCER_ID1 == 0 && isPacketRequest) {
            SEQUENCER_ID1 = ++SEQUENCER_ID;
        }
        String finalMessage = SEQUENCER_ID1 + ";" + message;

        DatagramSocket aSocket = null;
        for (int i = 0; i <= 3; i++) {
            try {
                aSocket = new DatagramSocket();
                byte[] messages = finalMessage.getBytes();
                InetAddress aHost = InetAddress.getByName("localhost");

                DatagramPacket packetRequest = new DatagramPacket(messages, messages.length, aHost, VariableStore.getReplicaPort(i));
                aSocket.send(packetRequest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

