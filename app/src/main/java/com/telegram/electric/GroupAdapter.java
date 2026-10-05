package com.telegram.electric;

import java.util.ArrayList;
import java.util.List;
import com.telegram.electric.core.TargetGroup;

public class GroupAdapter {
    private final List<TargetGroup> groups = new ArrayList<>();

    public void setGroups(List<TargetGroup> items) {
        groups.clear();
        if (items != null) groups.addAll(items);
    }

    public List<TargetGroup> getGroups() {
        return groups;
    }
}
