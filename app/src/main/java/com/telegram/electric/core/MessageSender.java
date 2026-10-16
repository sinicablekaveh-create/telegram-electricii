package com.telegram.electric.core;

/**
 * No Telegram transport is configured yet. Never report a message as sent.
 */
public class MessageSender {
    public boolean sendMessage(String groupId, String message) {
        return false;
    }
}
