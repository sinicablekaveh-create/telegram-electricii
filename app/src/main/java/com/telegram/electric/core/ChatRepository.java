package com.telegram.electric.core;

import java.util.HashMap;
import java.util.Map;

public class ChatRepository {
    private final Map<Long, String> chats = new HashMap<>();

    public void cacheChat(long id, String title) {
        chats.put(id, title);
    }

    public Map<Long, String> getChats() {
        return chats;
    }
}
