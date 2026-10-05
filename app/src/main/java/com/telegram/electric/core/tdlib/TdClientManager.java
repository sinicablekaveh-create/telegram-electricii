package com.telegram.electric.core.tdlib;

/**
 * TDLib client lifecycle manager scaffold.
 * Real TDLib Client initialization will be connected here.
 */
public class TdClientManager {
    private boolean initialized = false;

    public void initialize() {
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
