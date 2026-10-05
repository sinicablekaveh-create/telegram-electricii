package com.telegram.electric.core;

import java.util.ArrayList;
import java.util.List;

public class GroupSelectionManager {
    private final List<Long> availableGroups = new ArrayList<>();
    private long selectedTarget = 0;

    public void setGroups(List<Long> groups) {
        availableGroups.clear();
        availableGroups.addAll(groups);
    }

    public List<Long> getGroups() {
        return availableGroups;
    }

    public void selectTarget(long groupId) {
        selectedTarget = groupId;
    }

    public long getSelectedTarget() {
        return selectedTarget;
    }
}
