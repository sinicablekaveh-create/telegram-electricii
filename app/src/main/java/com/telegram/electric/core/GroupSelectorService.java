package com.telegram.electric.core;

import java.util.ArrayList;
import java.util.List;

public class GroupSelectorService {
    private final List<TargetGroup> selectedGroups = new ArrayList<>();

    public void selectGroup(TargetGroup group) {
        if (!selectedGroups.contains(group)) {
            selectedGroups.add(group);
        }
    }

    public List<TargetGroup> getSelectedGroups() {
        return selectedGroups;
    }

    public void clear() {
        selectedGroups.clear();
    }
}
