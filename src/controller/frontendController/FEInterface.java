package controller.frontendController;


import FrontEnd.ClientRequest;

public interface FEInterface {
    void informRmHasBug(int RmNumber);

    void informRmIsDown(int RmNumber);

    int sendRequestToSequencer(ClientRequest clientRequest);

    void retryRequest(ClientRequest clientRequest);
}