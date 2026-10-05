package com.telegram.electric.core;

public class SendPipeline {
    private final SendEngine sendEngine;
    private final MessageComposer composer;

    public SendPipeline(SendEngine sendEngine, MessageComposer composer) {
        this.sendEngine = sendEngine;
        this.composer = composer;
    }

    public void prepareElectricMessage() {
        sendEngine.addMessage(composer.compose("Electric Message"));
    }

    public String nextMessage() {
        return sendEngine.nextMessage();
    }
}
