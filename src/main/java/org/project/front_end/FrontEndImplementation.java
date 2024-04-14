package org.project.front_end;


import org.project.interfaces.FrontEndInterface;
import org.project.interfaces.WebServiceInterface;
import org.project.utils.VariableStore;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.project.front_end.FrontEnd.FE_IP_Address;


@WebService(endpointInterface = "org.project.interfaces.WebServiceInterface")

@SOAPBinding(style = SOAPBinding.Style.RPC)
public class FrontEndImplementation implements WebServiceInterface {
    private static long DYNAMIC_TIMEOUT = 10000;
    private static int Rm1BugCount = 0;
    private static int Rm2BugCount = 0;
    private static int Rm3BugCount = 0;
    private static int Rm4BugCount = 0;
    private static int Rm1NoResponseCount = 0;
    private static int Rm2NoResponseCount = 0;
    private static int Rm3NoResponseCount = 0;
    private static int Rm4NoResponseCount = 0;
    private final List<ResponseFromRM> responses = new ArrayList<>();
    private long responseTime = DYNAMIC_TIMEOUT;
    private long startTime;
    private CountDownLatch latch;
    private FrontEndInterface inter;

    public FrontEndImplementation(FrontEndInterface inter) {
        super();
        this.inter = inter;
        Runnable task = this::listenForUDPResponses;
        Thread thread = new Thread(task);
        thread.start();
    }

    public FrontEndImplementation() {

    }

    private void addReceivedResponse(ResponseFromRM res) {
        long endTime = System.nanoTime();
        responseTime = (endTime - startTime) / 1000000;
        System.out.println("Current Response time is: " + responseTime);
        responses.add(res);
        notifyOKCommandReceived();
    }

    private void notifyOKCommandReceived() {
        latch.countDown();
        System.out.println("FE :notifyOKCommandReceived>>>Response Received: Remaining responses" + latch.getCount());
    }

    @Override
    public String addAppointment(String userId, String appointmentID, String appointmentType, int capacity) {
        ClientRequest myRequest = new ClientRequest("addAppointment", userId);
        myRequest.setAppointmentID(appointmentID);
        myRequest.setAppointmentType(appointmentType);
        myRequest.setBookingCapacity(capacity);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE :addAppointment-------->>>" + myRequest);
        return validateResponses(myRequest);
    }

    @Override
    public String removeAppointment(String userId, String appointmentID, String appointmentType) {
        ClientRequest myRequest = new ClientRequest("removeAppointment", userId);
        myRequest.setAppointmentID(appointmentID);
        myRequest.setAppointmentType(appointmentType);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE :removeAppointment----->>>" + myRequest);
        return validateResponses(myRequest);
    }

    @Override
    public String listAppointmentAvailability(String userId, String appointmentType) {
        ClientRequest myRequest = new ClientRequest("listAppointmentAvailability", userId);
        myRequest.setAppointmentType(appointmentType);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE :listAppointmentAvailability------>>>" + myRequest);
        return validateResponses(myRequest);
    }

    @Override
    public String getAppointmentSchedule(String userId, String patientID) {
        ClientRequest myRequest = new ClientRequest("getAppointmentSchedule", patientID);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE :getAppointmentSchedule------>>>" + myRequest);
        return validateResponses(myRequest);
    }

    @Override
    public String cancelAppointment(String userId, String patientID, String appointmentID) {
        ClientRequest myRequest = new ClientRequest("cancelAppointment", patientID);
        myRequest.setAppointmentID(appointmentID);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE :cancelAppointment---->>>" + myRequest);
        return validateResponses(myRequest);
    }

    @Override
    public String bookAppointment(String userId, String patientID, String appointmentID, String appointmentType) {
        ClientRequest myRequest = new ClientRequest("bookAppointment", patientID);
        myRequest.setAppointmentID(appointmentID);
        myRequest.setAppointmentType(appointmentType);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE :bookAppointment----->>>" + myRequest);
        return validateResponses(myRequest);
    }

    @Override
    public String swapAppointment(String userId, String patientID, String oldAppointmentID, String oldAppointmentType, String newAppointmentID, String newAppointmentType) {
        ClientRequest myRequest = new ClientRequest("swapAppointment", patientID);
        myRequest.setAppointmentID(newAppointmentID);
        myRequest.setAppointmentType(newAppointmentType);
        myRequest.setOldAppointmentID(oldAppointmentID);
        myRequest.setOldAppointmentType(oldAppointmentType);
        myRequest.setSequenceNumber(sendUdpUnicastToSequencer(myRequest));
        System.out.println("FE :swapAppointment----->>>" + myRequest);
        return validateResponses(myRequest);
    }

    private int sendUdpUnicastToSequencer(ClientRequest myRequest) {
        startTime = System.nanoTime();
        int sequenceNumber = inter.sendRequestToSequencer(myRequest);
        myRequest.setSequenceNumber(sequenceNumber);
        latch = new CountDownLatch(4);
        waitForResponse();
        return sequenceNumber;
    }

    public void waitForResponse() {
        try {
            System.out.println("FE :waitForResponse>>>ResponsesRemain" + latch.getCount());
            boolean timeoutReached = latch.await(DYNAMIC_TIMEOUT, TimeUnit.MILLISECONDS);
            if (timeoutReached) {
                setDynamicTimout();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setDynamicTimout() {
        if (responseTime < 4000) {
            DYNAMIC_TIMEOUT = (DYNAMIC_TIMEOUT + (responseTime * 3)) / 2;
        } else {
            DYNAMIC_TIMEOUT = 10000;
        }
        System.out.println("FE :setDynamicTimout>>>" + DYNAMIC_TIMEOUT);
    }

    private String retryRequest(ClientRequest myRequest) {
        System.out.println("FE :retryRequest>>>" + myRequest);
        startTime = System.nanoTime();
        inter.retryRequest(myRequest);
        latch = new CountDownLatch(4);
        waitForResponse();
        return validateResponses(myRequest);
    }

    private String validateResponses(ClientRequest myRequest) {
        //todo add code to validate rm 4's response
        String resp;
        switch ((int) latch.getCount()) {
            case 0:
            case 1:
            case 2:
            case 3:
                resp = findMajorityResponse(myRequest);
                break;
            case 4:
                resp = "Fail: No response from any server";
                System.out.println(resp);
                if (myRequest.haveRetries()) {
                    myRequest.countRetry();
                    resp = retryRequest(myRequest);
                }
                rmDown(1);
                rmDown(2);
                rmDown(3);
                rmDown(4);
                break;
            default:
                resp = "Fail: " + myRequest.noRequestSendError();
                break;
        }
        System.out.println("FE :validateResponses>>>Responses remain:" + latch.getCount() + " >>>Response to be sent to client " + resp);
        return resp;
    }

    private void rmDown(int rmNumber) {
        DYNAMIC_TIMEOUT = 10000;
        switch (rmNumber) {
            case 1:
                Rm1NoResponseCount++;
                if (Rm1NoResponseCount == 3) {
                    Rm1NoResponseCount = 0;
                    inter.informRmIsDown(rmNumber);
                }
                break;
            case 2:
                Rm2NoResponseCount++;
                if (Rm2NoResponseCount == 3) {
                    Rm2NoResponseCount = 0;
                    inter.informRmIsDown(rmNumber);
                }
                break;

            case 3:
                Rm3NoResponseCount++;
                if (Rm3NoResponseCount == 3) {
                    Rm3NoResponseCount = 0;
                    inter.informRmIsDown(rmNumber);
                }
                break;
            case 4:
                Rm4NoResponseCount++;
                if (Rm4NoResponseCount == 3) {
                    Rm4NoResponseCount = 0;
                    inter.informRmIsDown(rmNumber);
                }
                break;
        }
        System.out.println("FE :rmDown>>>RM1 - noResponse:" + Rm1NoResponseCount);
        System.out.println("FE :rmDown>>>RM2 - noResponse:" + Rm2NoResponseCount);
        System.out.println("FE :rmDown>>>RM3 - noResponse:" + Rm3NoResponseCount);
        System.out.println("FE :rmDown>>>RM4 - noResponse:" + Rm4NoResponseCount);
    }

    private String findMajorityResponse(ClientRequest myRequest) {
        ResponseFromRM res1 = null;
        ResponseFromRM res2 = null;
        ResponseFromRM res3 = null;
        ResponseFromRM res4 = null;
        for (ResponseFromRM response :
                responses) {
            if (response.getSEQUENCE_ID() == myRequest.getSequenceNumber()) {
                switch (response.getRM_NUMBER()) {
                    case 1:
                        res1 = response;
                        break;
                    case 2:
                        res2 = response;
                        break;
                    case 3:
                        res3 = response;
                        break;
                    case 4:
                        res4 = response;
                        break;
                }
            }
        }
        System.out.println("FE :>>>RM1" + ((res1 != null) ? res1.getResponse() : "null"));
        System.out.println("FE :>>>RM2" + ((res2 != null) ? res2.getResponse() : "null"));
        System.out.println("FE :>>>RM3" + ((res3 != null) ? res3.getResponse() : "null"));
        System.out.println("FE :>>>RM4" + ((res4 != null) ? res4.getResponse() : "null"));
        if (res1 == null) {
            rmDown(1);
        } else {
            Rm1NoResponseCount = 0;
            if (res1.equals(res2)) {
                if (!res1.equals(res3) && res3 != null) {
                    rmBugFound(3);
                }
                return res2.getResponse();
            } else if (res1.equals(res3)) {
                if (!res1.equals(res2) && res2 != null) {
                    rmBugFound(2);
                }
                return res1.getResponse();
            } else if (res1.equals(res4)) {
                if (!res1.equals(res2) && res2 != null) {
                    rmBugFound(2);
                } else if (!res1.equals(res3) && res3 != null) {
                    rmBugFound(3);
                }
                return res1.getResponse();
            } else {

                if (res2 == null && res3 == null && res4 == null) {
                    return res1.getResponse();
                } else {

                }

            }
        }
        if (res2 == null) {
            rmDown(2);
        } else {
            Rm2NoResponseCount = 0;
            if (res2.equals(res3)) {
                if (!res2.equals(res1) && res1 != null) {
                    rmBugFound(1);
                }
                return res2.getResponse();
            } else if (res2.equals(res1)) {
                if (!res2.equals(res3) && res3 != null) {
                    rmBugFound(3);
                }
                return res2.getResponse();
            } else if (res2.equals(res4)) {
                if (!res2.equals(res1) && res1 != null) {
                    rmBugFound(1);
                } else if (!res2.equals(res3) && res3 != null) {
                    rmBugFound(3);
                }
                return res2.getResponse();
            } else {

                if (res1 == null && res3 == null && res4 == null) {
                    return res2.getResponse();
                } else {

                }

            }
        }
        if (res3 == null) {
            rmDown(3);
        } else {
            Rm3NoResponseCount = 0;
            if (res3.equals(res2)) {
                if (!res3.equals(res1) && res1 != null) {
                    rmBugFound(1);
                }
                return res2.getResponse();
            } else if (res3.equals(res1) && res2 != null) {
                if (!res3.equals(res2) && res4 != null) {
                    rmBugFound(2);
                }
                return res3.getResponse();
            } else if (res3.equals(res4)) {
                if (!res3.equals(res1) && res1 != null) {
                    rmBugFound(1);
                } else if (!res3.equals(res2) && res2 != null) {
                    rmBugFound(2);
                }
                return res3.getResponse();
            } else {

                if (res1 == null && res2 == null && res4 == null) {
                    return res3.getResponse();
                } else {

                }

            }
        }
        if (res4 == null) {
            rmDown(4);
        } else {
            Rm4NoResponseCount = 0;
            if (res4.equals(res3)) {
                if (!res4.equals(res1) && res1 != null) {
                    rmBugFound(1);
                }
                return res4.getResponse();
            } else if (res4.equals(res1)) {
                if (!res4.equals(res3) && res3 != null) {
                    rmBugFound(3);
                }
                return res4.getResponse();
            } else if (res4.equals(res2)) {
                if (!res4.equals(res1) && res1 != null) {
                    rmBugFound(1);
                } else if (!res4.equals(res3) && res3 != null) {
                    rmBugFound(3);
                }
                return res4.getResponse();
            } else {

                if (res1 == null && res2 == null && res3 == null) {
                    return res4.getResponse();
                } else {

                }

            }
        }

        return "Fail: --------majority response not-------- found";
    }

    private void rmBugFound(int rmNumber) {
        switch (rmNumber) {
            case 1:
                Rm1BugCount++;
                if (Rm1BugCount == 3) {
                    Rm1BugCount = 0;
                    inter.informRmHasBug(rmNumber);
                }
                break;
            case 2:
                Rm2BugCount++;
                if (Rm2BugCount == 3) {
                    Rm2BugCount = 0;
                    inter.informRmHasBug(rmNumber);
                }
                break;

            case 3:
                Rm3BugCount++;
                if (Rm3BugCount == 3) {
                    Rm3BugCount = 0;
                    inter.informRmHasBug(rmNumber);
                }
                break;
            case 4:
                Rm4BugCount++;
                if (Rm4BugCount == 3) {
                    Rm4BugCount = 0;
                    inter.informRmHasBug(rmNumber);
                }
                break;
        }
        System.out.println("FE Implementation:rmBugFound>>>RM1 - bugs:" + Rm1BugCount);
        System.out.println("FE Implementation:rmBugFound>>>RM2 - bugs:" + Rm2BugCount);
        System.out.println("FE Implementation:rmBugFound>>>RM3 - bugs:" + Rm3BugCount);
        System.out.println("FE Implementation:rmBugFound>>>RM4 - bugs:" + Rm4BugCount);
    }

    private void listenForUDPResponses() {
        DatagramSocket aSocket = null;
        try {

            InetAddress desiredAddress = InetAddress.getByName(FE_IP_Address);
            aSocket = new DatagramSocket(VariableStore.FRONT_END_PORT, desiredAddress);
            byte[] buffer = new byte[1000];
            System.out.println("FE Server Started on " + desiredAddress + ":" + VariableStore.FRONT_END_PORT + "............");

            while (true) {
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(response);
                String sentence = new String(response.getData(), 0,
                        response.getLength()).trim();
                System.out.println("FE:Response received from Rm>>>" + sentence);
                ResponseFromRM rmResponse = new ResponseFromRM(sentence);
                System.out.println("Adding response to Front ---- EndImplementation ----:");
                addReceivedResponse(rmResponse);
            }

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }
}
