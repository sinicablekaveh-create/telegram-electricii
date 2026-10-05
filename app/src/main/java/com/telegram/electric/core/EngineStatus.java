package com.telegram.electric.core;

public class EngineStatus {
    private String message = "Offline";

    public void update(String value) {
        message = value;
    }

    public String get() {
        return message;
    }
}
