package com.telegram.electric.core;

import java.util.concurrent.atomic.AtomicBoolean;

public class CoreEngine {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private long targetGroupId = 0;

    public void start(long groupId) {
        targetGroupId = groupId;
        running.set(true);
    }

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public long getTargetGroupId() {
        return targetGroupId;
    }
}
