package com.telegram.electric.core;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class TelegramTransportUnavailableTest {
    @Test
    public void engineDoesNotReportRunningBeforeAUserControlledSendFlowExists() {
        assertFalse(new CoreEngine().isRunning());
    }

    @Test(expected = IllegalStateException.class)
    public void engineCannotStartWithoutTheConfirmedSendFlow() {
        new CoreEngine().start(-100L);
    }

    @Test
    public void senderDoesNotReportSuccessfulDeliveryWhileSendingIsDisabled() {
        assertFalse(new MessageSender().sendMessage("-1001234567890", "test"));
    }
}
