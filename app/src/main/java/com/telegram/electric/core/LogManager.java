package com.telegram.electric.core;

import java.util.ArrayList;
import java.util.List;

public class LogManager {
    private final List<String> logs = new ArrayList<>();

    public void add(String message) {
        logs.add(message);
    }

    public List<String> getLogs() {
        return logs;
    }
}
