package com.telegram.electric.core;

public class CoreController {
    private boolean running;

    public void startCore(){
        running = true;
    }

    public void stopCore(){
        running = false;
    }

    public boolean isRunning(){
        return running;
    }
}
