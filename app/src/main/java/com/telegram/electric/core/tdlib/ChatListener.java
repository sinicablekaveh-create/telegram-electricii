package com.telegram.electric.core.tdlib;

import com.telegram.electric.core.GroupManager;

public class ChatListener {
    private final GroupManager groupManager;

    public ChatListener(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void onGroupFound(long chatId) {
        groupManager.addJoinedGroup(chatId);
    }
}
