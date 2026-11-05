package com.sinicablekaveh.thereeibaru;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.concurrent.atomic.AtomicInteger;

public class TermuxResultService extends IntentService {
    public static final String PREFS = "theree_v2_status";
    public static final String EXTRA_EXECUTION_ID = "execution_id";
    public static final String EXTRA_COMMAND_TYPE = "command_type";

    private static final AtomicInteger NEXT_ID = new AtomicInteger(2000);

    public TermuxResultService() {
        super("TermuxResultService");
    }

    public static int nextExecutionId() {
        return NEXT_ID.getAndIncrement();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;

        String type = intent.getStringExtra(EXTRA_COMMAND_TYPE);
        if (type == null) type = "unknown";
        int executionId = intent.getIntExtra(EXTRA_EXECUTION_ID, 0);

        Bundle result = intent.getBundleExtra("result");
        String stdout = "";
        String stderr = "";
        String errmsg = "";
        int exitCode = -999;
        int err = Activity.RESULT_OK;

        if (result != null) {
            stdout = result.getString("stdout", "");
            stderr = result.getString("stderr", "");
            errmsg = result.getString("errmsg", "");
            exitCode = result.getInt("exitCode", -999);
            err = result.getInt("err", Activity.RESULT_OK);
        } else {
            errmsg = "Termux returned no result bundle";
            err = 1;
        }

        boolean transportOk = err == Activity.RESULT_OK;
        boolean commandOk = transportOk && exitCode == 0;

        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit();
        e.putInt("result_seq", p.getInt("result_seq", 0) + 1);
        e.putString("last_type", type);
        e.putInt("last_execution_id", executionId);
        e.putInt("last_exit_code", exitCode);
        e.putInt("last_err", err);
        e.putString("last_stdout", trim(stdout, 12000));
        e.putString("last_stderr", trim(stderr, 6000));
        e.putString("last_errmsg", trim(errmsg, 3000));
        e.putLong("last_result_time", System.currentTimeMillis());

        if ("system".equals(type)) {
            e.putBoolean("system_ok", commandOk && stdout.contains("SYSTEM_OK"));
            if (!commandOk) e.putBoolean("preflight_ok", false);
        } else if ("install".equals(type)) {
            boolean ok = commandOk && (stdout.contains("V2_INSTALL_OK") || stdout.contains("INSTALL_OK"));
            e.putBoolean("runtime_ok", ok);
            e.putBoolean("system_ok", ok);
            e.putBoolean("preflight_ok", false);
        } else if ("preflight".equals(type)) {
            e.putBoolean("preflight_ok", commandOk && stdout.contains("V2_PREFLIGHT_OK"));
        } else if ("start".equals(type)) {
            e.putBoolean("running_ok", commandOk && stdout.contains("V2_START_OK"));
        } else if ("stop".equals(type)) {
            if (commandOk && stdout.contains("V2_STOP_OK")) {
                e.putBoolean("running_ok", false);
            }
        }

        e.apply();
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(s.length() - max);
    }
}
