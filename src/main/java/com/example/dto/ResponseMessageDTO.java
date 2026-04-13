package com.example.dto;

import java.io.Serializable;

public class ResponseMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public String status;
    public String message;

    public ResponseMessageDTO() {
        super();
    }

    public ResponseMessageDTO(String status, String message) {
        super();
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
