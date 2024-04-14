package org.project.interfaces;

import org.project.front_end.ClientRequest;

public interface FrontEndInterface {

    void informRmHasBug(int RmNumber);

    void informRmIsDown(int RmNumber);

    int sendRequestToSequencer(ClientRequest ClientRequest);

    void retryRequest(ClientRequest ClientRequest);

}
