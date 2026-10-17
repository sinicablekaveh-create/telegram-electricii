package com.telegram.electric.core;

/**
 * Fail-closed guard until the real TDLib runtime is packaged and initialized.
 */
public class CoreEngine {
    public void start(long groupId) {
        throw new IllegalStateException("TDLib is not connected; the engine cannot start.");
    }

    public void stop() {
        // There is no active engine to stop in this scaffold.
    }

    public boolean isRunning() {
        return false;
    }

    public long getTargetGroupId() {
        return 0L;
    }
}
