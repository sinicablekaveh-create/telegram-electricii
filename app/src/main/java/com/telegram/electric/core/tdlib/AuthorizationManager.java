package com.telegram.electric.core.tdlib;

public class AuthorizationManager {
    public enum State {
        WAIT_PHONE,
        WAIT_CODE,
        WAIT_PASSWORD,
        READY
    }

    private State state = State.WAIT_PHONE;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
