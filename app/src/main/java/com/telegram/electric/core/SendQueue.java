package com.telegram.electric.core;

import java.util.LinkedList;
import java.util.Queue;

public class SendQueue {
    private final Queue<String> messages = new LinkedList<>();

    public void add(String message) {
        messages.offer(message);
    }

    public String next() {
        return messages.poll();
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }
}
