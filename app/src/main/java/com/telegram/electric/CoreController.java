package com.telegram.electric;

public class CoreController {
    private boolean running = false;

    public void startCore() {
        running = true;
    }

    public void stopCore() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
