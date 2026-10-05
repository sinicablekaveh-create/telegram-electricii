package com.telegram.electric.core;

import java.util.ArrayList;
import java.util.List;

public class GroupManager {
    private final List<String> targetGroups = new ArrayList<>();

    public void addTargetGroup(String groupId) {
        if (!targetGroups.contains(groupId)) {
            targetGroups.add(groupId);
        }
    }

    public List<String> getTargetGroups() {
        return targetGroups;
    }
}
