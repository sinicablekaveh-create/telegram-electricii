package com.telegram.electric.core.tdlib;

/**
 * TDLib's Java/JNI runtime is not packaged in this scaffold.
 */
public class TdClientManager {
    public void initialize() {
        throw new IllegalStateException("TDLib JNI is not available in this build.");
    }

    public boolean isInitialized() {
        return false;
    }
}
