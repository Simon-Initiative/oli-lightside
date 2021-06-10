package edu.cmu.side;

import io.netty.handler.codec.http.HttpResponseStatus;

public class RequestException extends Exception{

    private HttpResponseStatus status;

    public RequestException(HttpResponseStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }
}
