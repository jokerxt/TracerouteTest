package ru.jxt.traceroutetest.main;

public abstract class PingerRoot implements IPingerRoot {
    private String error;

    public void setError(String err) {
        error = err;
    }

    public String getError() {
        return error;
    }
}
