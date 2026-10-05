package com.telegram.electric.core.tdlib;

import com.telegram.electric.core.GroupManager;

/**
 * Bridge between TDLib chat updates and local group selection logic.
 * Real TDLib update handlers can feed discovered chats into this class.
 */
public class GroupSyncManager {
    private final GroupManager groupManager;

    public GroupSyncManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void onChatLoaded(long chatId, boolean isGroup) {
        if (isGroup) {
            groupManager.addJoinedGroup(chatId);
        }
    }

    public void selectTarget(long chatId) {
        groupManager.selectTarget(chatId);
    }
}
