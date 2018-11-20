package com.jelastic.model;

public class Response {
    private int result = 0;
    private String error;

    public Boolean isNotOK() {
        return result != 0;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
