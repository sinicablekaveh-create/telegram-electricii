package com.telegram.electric.core;

import static org.junit.Assert.assertFalse;

import com.telegram.electric.core.tdlib.TdClientManager;
import org.junit.Test;

public class TelegramTransportUnavailableTest {
    @Test
    public void engineDoesNotReportRunning() {
        assertFalse(new CoreEngine().isRunning());
    }

    @Test(expected = IllegalStateException.class)
    public void engineCannotStartWithoutTdlib() {
        new CoreEngine().start(-100L);
    }

    @Test
    public void senderDoesNotReportSuccessfulDeliveryWithoutTdlib() {
        assertFalse(new MessageSender().sendMessage("-1001234567890", "test"));
    }

    @Test
    public void tdlibManagerDoesNotClaimInitialization() {
        assertFalse(new TdClientManager().isInitialized());
    }

    @Test(expected = IllegalStateException.class)
    public void tdlibManagerRejectsInitializationWithoutNativeRuntime() {
        new TdClientManager().initialize();
    }
}
