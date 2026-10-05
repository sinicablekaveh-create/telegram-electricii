package com.telegram.electric.core;

public class MessageSender {

    public boolean sendMessage(String groupId, String message) {
        // TDLib integration will be connected here.
        return groupId != null && message != null && !message.isEmpty();
    }
}
