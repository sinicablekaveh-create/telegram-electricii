package com.telegram.electric.core;

import java.util.LinkedList;
import java.util.Queue;

public class SendEngine {
    private final Queue<String> messageQueue = new LinkedList<>();
    private boolean active = false;

    public void start() {
        active = true;
    }

    public void stop() {
        active = false;
    }

    public void addMessage(String message) {
        if (message != null && !message.isEmpty()) {
            messageQueue.offer(message);
        }
    }

    public String nextMessage() {
        return messageQueue.poll();
    }

    public boolean isActive() {
        return active;
    }
}
