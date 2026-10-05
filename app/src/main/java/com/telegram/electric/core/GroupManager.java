package com.telegram.electric.core;

import java.util.ArrayList;
import java.util.List;

public class GroupManager {
    private final List<Long> joinedGroups = new ArrayList<>();
    private long selectedTarget;

    public void addJoinedGroup(long id) {
        if (!joinedGroups.contains(id)) {
            joinedGroups.add(id);
        }
    }

    public List<Long> getJoinedGroups() {
        return joinedGroups;
    }

    public void selectTarget(long id) {
        selectedTarget = id;
    }

    public long getTarget() {
        return selectedTarget;
    }
}
