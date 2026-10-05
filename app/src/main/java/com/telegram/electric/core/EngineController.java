package com.telegram.electric.core;

public class EngineController {
    private final CoreEngine coreEngine;
    private final SendEngine sendEngine;

    public EngineController(CoreEngine coreEngine, SendEngine sendEngine) {
        this.coreEngine = coreEngine;
        this.sendEngine = sendEngine;
    }

    public void start(long targetGroupId) {
        coreEngine.start(targetGroupId);
        sendEngine.start();
    }

    public void stop() {
        coreEngine.stop();
        sendEngine.stop();
    }

    public boolean isRunning() {
        return coreEngine.isRunning() && sendEngine.isActive();
    }
}
