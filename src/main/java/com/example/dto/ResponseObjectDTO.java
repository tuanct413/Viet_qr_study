package com.example.dto;

import java.io.Serializable;

public class ResponseObjectDTO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    public String status;
    public T data;

    public ResponseObjectDTO() {
        super();
    }

    public ResponseObjectDTO(String status, T data) {
        super();
        this.status = status;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
