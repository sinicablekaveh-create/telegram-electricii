package com.telegram.electric.core;

public class TargetGroup {
    private final long id;
    private final String title;
    private boolean selected;

    public TargetGroup(long id, String title) {
        this.id = id;
        this.title = title;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
