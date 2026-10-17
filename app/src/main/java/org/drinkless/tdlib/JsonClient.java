//
// Copyright Aliaksei Levin (levlam@telegram.org), Arseny Smirnov (arseny30@gmail.com) 2014-2026
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file app/src/main/assets/third_party/TDLib_LICENSE_1_0.txt)
//
package org.drinkless.tdlib;

/**
 * Main class for interaction with TDLib through its JSON interface.
 * API surface from the official TDLib Java example at the pinned upstream commit.
 */
public final class JsonClient {
    static {
        System.loadLibrary("tdjsonjava");
    }

    public static native int createClientId();

    public static native void send(int clientId, String request);

    public static native String receive(double timeout);

    public static native String execute(String request);

    public interface LogMessageHandler {
        void onLogMessage(int verbosityLevel, String message);
    }

    public static native void setLogMessageHandler(
            int maxVerbosityLevel,
            JsonClient.LogMessageHandler logMessageHandler
    );

    private JsonClient() {
    }
}
