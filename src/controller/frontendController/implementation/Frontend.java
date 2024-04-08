package controller.frontendController.implementation;

import FrontEnd.ClientRequest;
import FrontEnd.ResponseFromRM;
import controller.frontendController.FEInterface;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static FrontEnd.FrontEnd.ANSI_RESET;
import static FrontEnd.FrontEnd.sendUnicastToSequencer;


@WebService(endpointInterface = "controller.frontendController.FEInterface")
@SOAPBinding(style = SOAPBinding.Style.RPC)

public class Frontend implements FEInterface {
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";

    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    private static long DYNAMIC_TIMEOUT = 10000;
    private static int Rm1BugCount = 0;
    private static int Rm2BugCount = 0;
    private static int Rm3BugCount = 0;
    private static int Rm4BugCount = 0;
    private static int Rm1NoResponseCount = 0;
    private static int Rm2NoResponseCount = 0;
    private static int Rm3NoResponseCount = 0;
    private static int Rm4NoResponseCount = 0;
    private final String serverID;
    private final String serverName;
    private long responseTime = DYNAMIC_TIMEOUT;
    private long startTime;
    private CountDownLatch latch;
//    private final FEInterface inter;
    private final List<ResponseFromRM> responses = new ArrayList<>();
//    private ORB orb;

    public Frontend(String serverID,String serverName) {
        super();
        this.serverID = serverID;
        this.serverName = serverName;
    }


    @Override
    public synchronized String addAppointment(String adminID, String appointmentID, String appointmentType, int bookingCapacity) {

        ClientRequest clientRequest = new ClientRequest("addAppointment", adminID);
        clientRequest.setAPPOINTMENT_ID(appointmentID);
        clientRequest.setAPPOINTMENT_TYPE(appointmentType);
        clientRequest.setBookingCapacity(bookingCapacity);
        clientRequest.setSEQUENCER_NUMBER(sendUdpUnicastToSequencer(clientRequest));

        //Message format 0;LOCALHOST;00;ADD Appointment;MTLA101024;MTLM101024;MONTREAL;NULL;NULL;1
        System.out.println("inside FrontEnd Implementation:addAppointment ---> " + clientRequest);
        return validateResponses(clientRequest);
    }

    @Override
    public synchronized String removeAppointment(String adminID, String appointmentID, String appointmentType) {

        ClientRequest clientRequest = new ClientRequest("removeAppointment", adminID);
        clientRequest.setAPPOINTMENT_ID(appointmentID);
        clientRequest.setAPPOINTMENT_TYPE(appointmentType);
        clientRequest.setSEQUENCER_NUMBER(sendUdpUnicastToSequencer(clientRequest));
        System.out.println("FrontEnd Implementation:removeAppointment ---> " + clientRequest);
        return validateResponses(clientRequest);
    }

    @Override
    public synchronized String listAppointmentAvailability(String adminID, String appointmentType) {

        ClientRequest clientRequest = new ClientRequest("listAppointmentAvailability", adminID);
        clientRequest.setAPPOINTMENT_TYPE(appointmentType);
        clientRequest.setSEQUENCER_NUMBER(sendUdpUnicastToSequencer(clientRequest));
        System.out.println("FrontEnd Implementation:listAppointmentAvailability>>>" + clientRequest);
        return validateResponses(clientRequest);
    }

    @Override
    public synchronized String bookAppointment(String patientID, String appointmentID, String appointmentType) {

        ClientRequest clientRequest = new ClientRequest("bookAppointment", patientID);
        clientRequest.setAPPOINTMENT_ID(appointmentID);
        clientRequest.setAPPOINTMENT_TYPE(appointmentType);
        clientRequest.setSEQUENCER_NUMBER(sendUdpUnicastToSequencer(clientRequest));
        System.out.println("FrontEnd Implementation:bookAppointment ---> " + clientRequest);
        return validateResponses(clientRequest);
    }

    @Override
    public synchronized String getBookingSchedule(String patientID) {

        ClientRequest clientRequest = new ClientRequest("getBookingSchedule", patientID);
        clientRequest.setSEQUENCER_NUMBER(sendUdpUnicastToSequencer(clientRequest));
        System.out.println("FrontEnd Implementation:getBookingSchedule ---> " + clientRequest);
        return validateResponses(clientRequest);
    }

    @Override
    public synchronized String cancelAppointment(String patientID, String appointmentID, String appointmentType) {

        ClientRequest clientRequest = new ClientRequest("cancelBooking", patientID);
        clientRequest.setAPPOINTMENT_ID(appointmentID);
        clientRequest.setAPPOINTMENT_TYPE(appointmentType);
        clientRequest.setSEQUENCER_NUMBER(sendUdpUnicastToSequencer(clientRequest));
        System.out.println("FrontEnd Implementation:cancelAppointment ---> " + clientRequest.toString());
        return validateResponses(clientRequest);
    }

    @Override
    public synchronized String swapAppointment(String patientID, String newAppointmentID, String newAppointmentType, String oldAppointmentID, String oldAppointmentType) {

        ClientRequest ClientRequest = new ClientRequest("swapAppointment", patientID);
        ClientRequest.setAPPOINTMENT_ID(newAppointmentID);
        ClientRequest.setAPPOINTMENT_TYPE(newAppointmentType);
        ClientRequest.setOLD_APPOINTMENT_ID(oldAppointmentID);
        ClientRequest.setOLD_APPOINTMENT_TYPE(oldAppointmentType);
        ClientRequest.setSEQUENCER_NUMBER(sendUdpUnicastToSequencer(ClientRequest));
        System.out.println("FrontEnd Implementation:swapAppointment>>>" + ClientRequest);
        return validateResponses(ClientRequest);
    }

    public void waitForResponse() {
        try {
            System.out.println(ANSI_BLUE_BACKGROUND +"waiting For Response.... ResponsesRemain" + ANSI_RESET + latch.getCount());
            boolean timeoutReached = latch.await(DYNAMIC_TIMEOUT, TimeUnit.MILLISECONDS);
            if (timeoutReached) {
                setDynamicTimout();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String validateResponses(ClientRequest clientRequest) {
        String resp;
        switch ((int) latch.getCount()) {
            case 0:
            case 1:
            case 2:
            case 3:
                resp = findMajorityResponse(clientRequest);
                break;
            case 4:
                resp = "Fail: No response from any server";
                System.out.println(ANSI_RED_BACKGROUND + resp + ANSI_RESET);
                if (clientRequest.haveRetries()) {
                    clientRequest.countRetry();
                    resp = retryRequest(clientRequest);
                }
                rmDown(1);
                rmDown(2);
                rmDown(3);
                break;
            default:
                resp = "Fail: " + clientRequest.noRequestSendError();
                break;
        }
        System.out.println(ANSI_GREEN_BACKGROUND +"Frontend validating Responses ::: Responses remain:" +ANSI_RESET+ latch.getCount() + " Response to be sent to client ---> " + resp);
        return resp;
    }

    private String findMajorityResponse(ClientRequest clientRequest) {
        ResponseFromRM res1 = null;
        ResponseFromRM res2 = null;
        ResponseFromRM res3 = null;
        ResponseFromRM res4 = null;

        System.out.println(responses.size());
        for (ResponseFromRM response :
                responses) {
            if (response.getSEQUENCE_ID() == clientRequest.getSEQUENCER_NUMBER()) {
                switch (response.getRM_NUMBER()) {
                    case 1:
                        res1 = response;
                        break;
                    case 2:
                        res2 = response;
                        break;
                    case 3:
                        res3 = response;
                        System.out.println(ANSI_BLUE_BACKGROUND + "RM4 response received" + res3.getRESPONSE() + ANSI_RESET);
                        break;
                    case 4:
                        res4 = response;
                        break;
                }
            }
        }
        System.out.println("FrontEnd finding Majority Response <--- RM1" + ((res1 != null) ? res1.getRESPONSE() : "null"));
        System.out.println("FrontEnd finding Majority Response <--- RM2" + ((res2 != null) ? res2.getRESPONSE() : "null"));
        System.out.println("FrontEnd finding Majority Response <--- RM3" + ((res3 != null) ? res3.getRESPONSE() : "null"));
        System.out.println("FrontEnd finding Majority Response <--- RM4" + ((res4 != null) ? res4.getRESPONSE() : "null"));


        if (res1 == null) {
            rmDown(1);
        } else {
            Rm1NoResponseCount = 0;
            if (res1.equals(res2) && res1.equals(res3)) {
                if (!res1.equals(res4) && res4 != null) {
                    rmBugFound(4);

                }
                return res1.getRESPONSE();

            } else if (res1.equals(res2) && res1.equals(res4)) {
                if (!res1.equals(res3) && res3 != null) {
                    rmBugFound(3);

                }
                return res1.getRESPONSE();
            } else if (res1.equals(res3) && res1.equals(res4)) {
                if (!res1.equals(res2) && res2 != null) {
                    rmBugFound(2);

                }
                return res1.getRESPONSE();
            } else if (res2 == null && res3 == null && res4 == null) {
                return res1.getRESPONSE();
            } else {
//                rmBugFound(1);
            }
        }

        if (res2 == null) {
            rmDown(2);
        } else {
            Rm2NoResponseCount = 0;
            if (res2.equals(res1) && res2.equals(res3)) {
                if (!res2.equals(res4) && res4 != null) {
                    rmBugFound(4);

                }
                return res2.getRESPONSE();
            } else if (res2.equals(res1) && res2.equals(res4)) {
                if (!res2.equals(res3) && res3 != null) {
                    rmBugFound(3);

                }
                return res2.getRESPONSE();
            } else if (res2.equals(res3) && res2.equals(res4)) {
                if (!res2.equals(res1) && res1 != null) {
                    rmBugFound(1);

                }
                return res2.getRESPONSE();
            } else if (res1 == null && res3 == null && res4 == null) {
                return res2.getRESPONSE();
            } else {
//                rmBugFound(2);
            }
        }

        if (res3 == null) {
            rmDown(3);
        } else {
            Rm3NoResponseCount = 0;
            if (res3.equals(res1) && res3.equals(res2)) {
                if (!res3.equals(res4) && res4 != null) {
                    rmBugFound(4);

                }
                return res3.getRESPONSE();
            } else if (res3.equals(res1) && res3.equals(res4)) {
                if (!res3.equals(res2) && res2 != null) {
                    rmBugFound(2);

                }
                return res3.getRESPONSE();
            } else if (res3.equals(res2) && res3.equals(res4)) {
                if (!res3.equals(res1) && res1 != null) {
                    rmBugFound(1);
                }
                return res3.getRESPONSE();
            } else if (res1 == null && res2 == null && res4 == null) {
                return res3.getRESPONSE();
            } else {
//                rmBugFound(3);
            }
        }

        if (res4 == null) {
            rmDown(4);
        } else {
            Rm4NoResponseCount = 0;
            if (res4.equals(res1) && res4.equals(res2)) {
                if (!res4.equals(res3) && res3 != null) {
                    rmBugFound(3);
                }
                return res4.getRESPONSE();
            } else if (res4.equals(res1) && res4.equals(res3)) {
                if (!res4.equals(res2) && res2 != null) {
                    rmBugFound(2);
                }
                return res4.getRESPONSE();
            } else if (res4.equals(res2) && res4.equals(res3)) {
                if (!res4.equals(res1) && res1 != null) {
                    rmBugFound(1);
                }
                return res4.getRESPONSE();
            } else if (res1 == null && res2 == null && res3 == null) {
                return res4.getRESPONSE();
            } else {
//                rmBugFound(4);
            }
        }



        return "!! Fail: majority response not found";
    }

    private void rmBugFound(int rmNumber) {
        switch (rmNumber) {
            case 1:
                Rm1BugCount++;
                if (Rm1BugCount == 3) {
                    Rm1BugCount = 0;
                    informRmHasBug(rmNumber);
                }
                break;
            case 2:
                Rm2BugCount++;
                if (Rm2BugCount == 3) {
                    Rm2BugCount = 0;
                    informRmHasBug(rmNumber);
                }
                break;

            case 3:
                Rm3BugCount++;
                if (Rm3BugCount == 3) {
                    Rm3BugCount = 0;
                    informRmHasBug(rmNumber);
                }
                break;
            case 4:
                Rm4BugCount++;
                if (Rm4BugCount == 3) {
                    Rm4BugCount = 0;
                    informRmHasBug(rmNumber);
                }
                break;
        }
        System.out.println("FrontEnd rmBugFound on ---> RM1 - bugs:" + Rm1BugCount);
        System.out.println("FrontEnd rmBugFound on ---> RM2 - bugs:" + Rm2BugCount);
        System.out.println("FrontEnd rmBugFound on ---> RM3 - bugs:" + Rm3BugCount);
        System.out.println("FrontEnd rmBugFound on ---> RM4 - bugs:" + Rm4BugCount);
    }

    private void rmDown(int rmNumber) {
        DYNAMIC_TIMEOUT = 10000;
        switch (rmNumber) {
            case 1:
                Rm1NoResponseCount++;
                if (Rm1NoResponseCount == 3) {
                    Rm1NoResponseCount = 0;
                    informRmIsDown(rmNumber);
                }
                break;
            case 2:
                Rm2NoResponseCount++;
                if (Rm2NoResponseCount == 3) {
                    Rm2NoResponseCount = 0;
                    informRmIsDown(rmNumber);
                }
                break;

            case 3:
                Rm3NoResponseCount++;
                if (Rm3NoResponseCount == 3) {
                    Rm3NoResponseCount = 0;
                    informRmIsDown(rmNumber);
                }
                break;
            case 4:
                Rm4NoResponseCount++;
                if (Rm4NoResponseCount == 3) {
                    Rm4NoResponseCount = 0;
                    informRmIsDown(rmNumber);
                }
        }
        System.out.println("FrontEnd rmDown ---> RM1 - noResponse:" + Rm1NoResponseCount);
        System.out.println("FrontEnd rmDown ---> RM2 - noResponse:" + Rm2NoResponseCount);
        System.out.println("FrontEnd rmDown ---> RM3 - noResponse:" + Rm3NoResponseCount);
        System.out.println("FrontEnd rmDown ---> RM4 - noResponse:" + Rm4NoResponseCount);

    }

    private void setDynamicTimout() {
        if (responseTime < 4000) {
            DYNAMIC_TIMEOUT = (DYNAMIC_TIMEOUT + (responseTime * 3)) / 2;
//            System.out.println("FrontEnd Implementation:setDynamicTimout>>>" + responseTime * 2);
        } else {
            DYNAMIC_TIMEOUT = 10000;
        }
        System.out.println("Current dynamic Timeout>>>" + DYNAMIC_TIMEOUT);
    }

    private void notifyOKCommandReceived() {
        latch.countDown();
        System.out.println("FrontEnd Received Response : Remaining responses" + latch.getCount());
    }

    public void addReceivedResponse(ResponseFromRM res) {
        long endTime = System.nanoTime();
        responseTime = (endTime - startTime) / 1000000;
        System.out.println("Current Response time is: " + responseTime);
        responses.add(res);
        notifyOKCommandReceived();
    }

    private int sendUdpUnicastToSequencer(ClientRequest clientRequest) {
        startTime = System.nanoTime();
        int sequenceNumber = sendRequestToSequencer(clientRequest);
        clientRequest.setSEQUENCER_NUMBER(sequenceNumber);
        latch = new CountDownLatch(4);
        waitForResponse();
        return sequenceNumber;
    }

    @Override
    public void informRmHasBug(int RmNumber) {

        ClientRequest errorMessage = new ClientRequest(RmNumber, "1");

        sendUnicastToSequencer(errorMessage);

        System.out.println( ANSI_RED_BACKGROUND + "FrontEnd is informing RmHasBug>>>RM" + RmNumber + " has a bug" + ANSI_RESET);

    }

    @Override
    public void informRmIsDown(int RmNumber) {

        ClientRequest errorMessage = new ClientRequest(RmNumber, "2");

        sendUnicastToSequencer(errorMessage);

        System.out.println( ANSI_RED_BACKGROUND + "FrontEnd is informing RmIsDown>>>RM" + RmNumber + " is down" + ANSI_RESET);

    }

//    @Override
//    public void retryRequest(ClientRequest myRequest) {
//        System.out.println("No response from all Rms, Retrying request...");
////        sendUnicastToSequencer(myRequest);
//    }

    @Override
    public int sendRequestToSequencer(ClientRequest clientRequest) {
        return sendUnicastToSequencer(clientRequest);
    }


    //Change it according
    public String retryRequest(ClientRequest clientRequest) {
        System.out.println("FrontEnd Implementation:retryRequest>>>" + clientRequest.toString());
        startTime = System.nanoTime();
        retryRequest(clientRequest);
        latch = new CountDownLatch(4);
        waitForResponse();
        return validateResponses(clientRequest);
    }
}