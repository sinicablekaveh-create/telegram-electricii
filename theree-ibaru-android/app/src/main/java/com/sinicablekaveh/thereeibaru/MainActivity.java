package com.sinicablekaveh.thereeibaru;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String TERMUX_PACKAGE = "com.termux";
    private static final String RUN_PERMISSION = "com.termux.permission.RUN_COMMAND";
    private static final String PANEL_URL = "http://127.0.0.1:8765/";
    private static final int REQ_RUN_PERMISSION = 4101;
    private static final int REQ_FILE = 4102;

    private final Handler handler = new Handler();
    private SharedPreferences prefs;
    private WebView webView;
    private TextView status;
    private TextView details;
    private Button startButton;
    private ValueCallback<Uri[]> fileCallback;
    private int lastSeenResultSeq = -1;

    private final Runnable poller = new Runnable() {
        @Override
        public void run() {
            refreshState();
            handler.postDelayed(this, 900);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(TermuxResultService.PREFS, MODE_PRIVATE);
        prefs.edit()
                .putBoolean("preflight_ok", false)
                .putBoolean("running_ok", false)
                .apply();
        buildUi();
        ensureRunPermission();
        handler.post(poller);
        handler.postDelayed(this::loadPanel, 900);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(poller);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 247, 250));

        TextView title = new TextView(this);
        title.setText("theree ibaru V3");
        title.setTextSize(23f);
        title.setTextColor(Color.rgb(22, 48, 82));
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(16), dp(10), dp(16), dp(2));
        root.addView(title);

        status = new TextView(this);
        status.setText("Checking prerequisites...");
        status.setTextSize(13f);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(dp(16), 0, dp(16), dp(4));
        root.addView(status);

        details = new TextView(this);
        details.setTextSize(11f);
        details.setTextColor(Color.rgb(70, 70, 70));
        details.setPadding(dp(16), 0, dp(16), dp(8));
        root.addView(details);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(8), dp(3), dp(8), dp(7));

        bar.addView(makeButton("SETUP TERMUX", v -> showTermuxSetup()));
        bar.addView(makeButton("CHECK SYSTEM", v -> checkSystem()));
        bar.addView(makeButton("INSTALL RUNTIME", v -> installRuntime()));
        bar.addView(makeButton("PREFLIGHT", v -> runPreflight()));

        startButton = makeButton("START", v -> startScheduler());
        startButton.setEnabled(false);
        bar.addView(startButton);

        bar.addView(makeButton("BALE LOGIN", v -> askPhoneAndLogin("bale")));
        bar.addView(makeButton("EITAA LOGIN", v -> askPhoneAndLogin("eitaa")));
        bar.addView(makeButton("RUBIKA LOGIN", v -> askPhoneAndLogin("rubika")));
        bar.addView(makeButton("STOP", v -> stopScheduler()));
        bar.addView(makeButton("TERMUX INFO", v -> openTermuxAppInfo()));
        bar.addView(makeButton("OVERLAY", v -> openTermuxOverlaySettings()));
        bar.addView(makeButton("RELOAD", v -> loadPanel()));

        hsv.addView(bar);
        root.addView(hsv);

        webView = new WebView(this);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (PANEL_URL.equals(url)) {
                    status.setText("Panel connected. Use PREFLIGHT before START.");
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    status.setText("Panel offline. Install Runtime, log in, then run PREFLIGHT.");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "File picker failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private Button makeButton(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(11f);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(44));
        lp.setMargins(dp(3), 0, dp(3), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private boolean isTermuxInstalled() {
        try {
            getPackageManager().getPackageInfo(TERMUX_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String termuxVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(TERMUX_PACKAGE, 0);
            return info.versionName == null ? "unknown" : info.versionName;
        } catch (Exception e) {
            return "missing";
        }
    }

    private boolean termuxVersionSupported() {
        String version = termuxVersion();
        if ("missing".equals(version) || "unknown".equals(version)) return false;
        String[] parts = version.split("\\.");
        int major = parts.length > 0 ? leadingInt(parts[0]) : 0;
        int minor = parts.length > 1 ? leadingInt(parts[1]) : 0;
        if (major > 0) return true;
        return minor >= 109;
    }

    private int leadingInt(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) b.append(c);
            else break;
        }
        if (b.length() == 0) return 0;
        try {
            return Integer.parseInt(b.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean hasRunPermission() {
        return Build.VERSION.SDK_INT < 23 ||
                checkSelfPermission(RUN_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    private void ensureRunPermission() {
        if (!isTermuxInstalled()) {
            status.setText("Termux is not installed.");
            return;
        }
        if (!termuxVersionSupported()) {
            status.setText("Termux 0.109+ is required for verified command results.");
            return;
        }
        if (!hasRunPermission()) {
            requestPermissions(new String[]{RUN_PERMISSION}, REQ_RUN_PERMISSION);
            status.setText("Grant: Run commands in Termux environment.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_RUN_PERMISSION) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                status.setText("RUN_COMMAND permission granted. Press CHECK SYSTEM.");
            } else {
                status.setText("RUN_COMMAND permission denied. V3 cannot control Termux.");
            }
        }
    }

    private void refreshState() {
        boolean installed = isTermuxInstalled();
        boolean permission = hasRunPermission();
        boolean systemOk = prefs.getBoolean("system_ok", false);
        boolean runtimeOk = prefs.getBoolean("runtime_ok", false) || systemOk;
        boolean preflightOk = prefs.getBoolean("preflight_ok", false);
        boolean runningOk = prefs.getBoolean("running_ok", false);

        startButton.setEnabled(installed && termuxVersionSupported() && permission && preflightOk);

        StringBuilder s = new StringBuilder();
        s.append("Termux ").append(installed ? termuxVersion() : "MISSING");
        s.append(" · RUN_COMMAND ").append(permission ? "GRANTED" : "NOT GRANTED");
        s.append(" · Runtime ").append(runtimeOk ? "VERIFIED" : "NOT VERIFIED");
        s.append(" · Preflight ").append(preflightOk ? "PASS" : "NOT PASSED");
        s.append(" · Scheduler ").append(runningOk ? "RUNNING" : "NOT CONFIRMED");
        status.setText(s.toString());

        int seq = prefs.getInt("result_seq", 0);
        if (seq != lastSeenResultSeq) {
            lastSeenResultSeq = seq;
            String type = prefs.getString("last_type", "none");
            int exit = prefs.getInt("last_exit_code", -999);
            String stdout = prefs.getString("last_stdout", "");
            String stderr = prefs.getString("last_stderr", "");
            String errmsg = prefs.getString("last_errmsg", "");

            StringBuilder d = new StringBuilder();
            d.append("Last command: ").append(type).append(" · exit=").append(exit);
            if (!errmsg.isEmpty()) d.append("\nTermux: ").append(errmsg);
            if (!stderr.isEmpty()) d.append("\nError: ").append(lastLines(stderr, 6));
            if (!stdout.isEmpty()) d.append("\n").append(lastLines(stdout, 8));
            details.setText(d.toString());
        }
    }

    private String lastLines(String text, int count) {
        String[] lines = text.replace("\r", "").split("\n");
        int start = Math.max(0, lines.length - count);
        StringBuilder b = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) continue;
            if (b.length() > 0) b.append("\n");
            b.append(lines[i]);
        }
        return b.toString();
    }

    private boolean readyForTermux() {
        if (!isTermuxInstalled()) {
            Toast.makeText(this, "Install Termux first.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!termuxVersionSupported()) {
            Toast.makeText(this, "Termux 0.109 or newer is required.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!hasRunPermission()) {
            ensureRunPermission();
            Toast.makeText(this, "Grant RUN_COMMAND permission first.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void showTermuxSetup() {
        String command =
                "mkdir -p ~/.termux; " +
                "file=~/.termux/termux.properties; touch \"$file\"; " +
                "if grep -q '^allow-external-apps=' \"$file\"; then " +
                "sed -i 's/^allow-external-apps=.*/allow-external-apps=true/' \"$file\"; " +
                "else echo 'allow-external-apps=true' >> \"$file\"; fi; " +
                "chmod 700 ~/.termux; termux-reload-settings";

        new AlertDialog.Builder(this)
                .setTitle("One-time Termux setup")
                .setMessage(
                        "Termux itself requires allow-external-apps=true and the Additional Permission RUN_COMMAND. " +
                        "For interactive OTP windows on Android 10+, Draw Over Apps may also be needed.\n\n" + command)
                .setPositiveButton("COPY & OPEN TERMUX", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("theree v2 setup", command));
                    Intent i = getPackageManager().getLaunchIntentForPackage(TERMUX_PACKAGE);
                    if (i != null) startActivity(i);
                    else Toast.makeText(this, "Termux is not installed.", Toast.LENGTH_LONG).show();
                })
                .setNeutralButton("APP INFO", (d, w) -> openTermuxAppInfo())
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void openTermuxAppInfo() {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + TERMUX_PACKAGE));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open Termux app info.", Toast.LENGTH_LONG).show();
        }
    }

    private void openTermuxOverlaySettings() {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + TERMUX_PACKAGE));
            startActivity(i);
        } catch (Exception e) {
            openTermuxAppInfo();
        }
    }

    private String readTextAsset(String name) throws Exception {
        InputStream in = getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8)
                .replace("\n", "").replace("\r", "").trim();
    }

    private void checkSystem() {
        if (!readyForTermux()) return;
        prefs.edit().putBoolean("system_ok", false).putBoolean("preflight_ok", false).apply();

        String command =
                "set -euo pipefail; " +
                "grep -Eq '^[[:space:]]*allow-external-apps[[:space:]]*=[[:space:]]*true[[:space:]]*$' " +
                "\"$HOME/.termux/termux.properties\"; " +
                "command -v proot-distro >/dev/null; " +
                "test -d \"$HOME/theree_ibaru_source\"; " +
                "cd \"$HOME/theree_ibaru_source\"; " +
                "bash scripts/verify_install.sh; " +
                "echo SYSTEM_OK";

        runTermuxWithResult("system", "V3 system check", command, null, true, false);
    }

    private void installRuntime() {
        if (!readyForTermux()) return;
        prefs.edit()
                .putBoolean("runtime_ok", false)
                .putBoolean("system_ok", false)
                .putBoolean("preflight_ok", false)
                .apply();

        try {
            String payload = readTextAsset("runtime_v09.b64");
            String command =
                    "set -euo pipefail; " +
                    "command -v base64 >/dev/null; " +
                    "command -v unzip >/dev/null 2>&1 || pkg install -y unzip; " +
                    "rm -rf \"$HOME/.theree_apk_stage\"; " +
                    "mkdir -p \"$HOME/.theree_apk_stage/unpack\"; " +
                    "base64 -d > \"$HOME/.theree_apk_stage/runtime.zip\"; " +
                    "unzip -t \"$HOME/.theree_apk_stage/runtime.zip\" >/dev/null; " +
                    "unzip -q \"$HOME/.theree_apk_stage/runtime.zip\" -d \"$HOME/.theree_apk_stage/unpack\"; " +
                    "test -f \"$HOME/.theree_apk_stage/unpack/theree_ibaru_source/install.sh\"; " +
                    "rm -rf \"$HOME/theree_ibaru_source\"; " +
                    "cp -a \"$HOME/.theree_apk_stage/unpack/theree_ibaru_source\" \"$HOME/theree_ibaru_source\"; " +
                    "cd \"$HOME/theree_ibaru_source\"; " +
                    "bash install.sh; " +
                    "bash scripts/verify_install.sh; " +
                    "echo V3_INSTALL_OK";

            status.setText("Installing Runtime. This downloads Debian/Python/Bun dependencies once.");
            runTermuxWithResult("install", "Install theree ibaru V3 Runtime", command, payload, true, false);
        } catch (Exception e) {
            showError("Runtime asset error", e);
        }
    }

    private String ensurePanelShell() {
        return
                "cd \"$HOME/theree_ibaru_source\" 2>/dev/null || { echo 'Runtime not installed'; exit 2; }; " +
                "mkdir -p data/logs; " +
                "if ! curl -fsS --max-time 2 http://127.0.0.1:8765/api/status >/dev/null 2>&1; then " +
                "nohup bash scripts/start_panel.sh >> data/logs/v3-panel.log 2>&1 </dev/null & " +
                "for i in $(seq 1 30); do " +
                "curl -fsS --max-time 2 http://127.0.0.1:8765/api/status >/dev/null 2>&1 && break; " +
                "sleep 1; done; fi; " +
                "curl -fsS --max-time 3 http://127.0.0.1:8765/api/status >/dev/null; ";
    }

    private void runPreflight() {
        if (!readyForTermux()) return;
        prefs.edit().putBoolean("preflight_ok", false).putBoolean("running_ok", false).apply();

        String command =
                "set -euo pipefail; " +
                "cd \"$HOME/theree_ibaru_source\" 2>/dev/null || { echo 'Runtime not installed'; exit 2; }; " +
                "bash scripts/verify_install.sh; " +
                "bash scripts/start_eitaabun.sh >/tmp/theree_v3_eitaa.log 2>&1 || true; " +
                ensurePanelShell() +
                "curl -fsS -X POST http://127.0.0.1:8765/preflight >/dev/null; " +
                "sleep 1; " +
                "STATUS_JSON=$(curl -fsS --max-time 5 http://127.0.0.1:8765/api/status); " +
                "echo \"$STATUS_JSON\"; " +
                "echo \"$STATUS_JSON\" | grep -Eq '\"last_preflight_ok\"[[:space:]]*:[[:space:]]*true'; " +
                "echo V3_PREFLIGHT_OK";

        status.setText("Running live account Preflight...");
        runTermuxWithResult("preflight", "V3 live Preflight", command, null, true, false);
    }

    private void startScheduler() {
        if (!readyForTermux()) return;
        if (!prefs.getBoolean("preflight_ok", false)) {
            Toast.makeText(this, "PREFLIGHT must PASS first.", Toast.LENGTH_LONG).show();
            return;
        }

        String command =
                "set -euo pipefail; " +
                ensurePanelShell() +
                "curl -fsS -X POST http://127.0.0.1:8765/start >/dev/null; " +
                "sleep 1; " +
                "STATUS_JSON=$(curl -fsS --max-time 5 http://127.0.0.1:8765/api/status); " +
                "echo \"$STATUS_JSON\"; " +
                "echo \"$STATUS_JSON\" | grep -Eq '\"last_preflight_ok\"[[:space:]]*:[[:space:]]*true'; " +
                "echo \"$STATUS_JSON\" | grep -Eq '\"running\"[[:space:]]*:[[:space:]]*true'; " +
                "echo V3_START_OK";

        prefs.edit().putBoolean("running_ok", false).apply();
        runTermuxWithResult("start", "Start theree ibaru scheduler", command, null, true, false);
        handler.postDelayed(this::loadPanel, 1600);
    }

    private void stopScheduler() {
        if (!readyForTermux()) return;

        String command =
                "set -euo pipefail; " +
                "if curl -fsS --max-time 2 http://127.0.0.1:8765/api/status >/dev/null 2>&1; then " +
                "curl -fsS -X POST http://127.0.0.1:8765/stop >/dev/null; sleep 1; " +
                "STATUS_JSON=$(curl -fsS --max-time 5 http://127.0.0.1:8765/api/status); " +
                "echo \"$STATUS_JSON\"; " +
                "echo \"$STATUS_JSON\" | grep -Eq '\"running\"[[:space:]]*:[[:space:]]*false'; " +
                "fi; echo V3_STOP_OK";

        runTermuxWithResult("stop", "Stop theree ibaru scheduler", command, null, true, false);
    }

    private void askPhoneAndLogin(String service) {
        if (!readyForTermux()) return;

        EditText input = new EditText(this);
        input.setHint("+98912...");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle(service.toUpperCase() + " login")
                .setMessage("Enter the account phone. OTP/password stays in the Termux interactive session. After login, return and run PREFLIGHT.")
                .setView(input)
                .setPositiveButton("LOGIN", (d, w) -> {
                    String phone = input.getText().toString().trim();
                    if (!phone.matches("^\\+?[0-9]{7,15}$")) {
                        Toast.makeText(this, "Invalid phone number.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String script;
                    if ("bale".equals(service)) script = "scripts/login_bale.sh";
                    else if ("eitaa".equals(service)) script = "scripts/eitaa_login.sh";
                    else script = "scripts/login_rubika.sh";

                    String command =
                            "cd \"$HOME/theree_ibaru_source\" 2>/dev/null || { echo 'Runtime not installed'; read -r _; exit 2; }; " +
                            "bash " + script + " --phone " + shellQuote(phone) +
                            "; echo; echo 'Login finished. Return to theree ibaru V3 and run PREFLIGHT.'; read -r _";

                    prefs.edit().putBoolean("preflight_ok", false).apply();
                    runTermuxNoResult(service + " login", command, false, true);
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private String shellQuote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    private void runTermuxWithResult(
            String type,
            String label,
            String command,
            String stdin,
            boolean background,
            boolean openSession) {

        if (!readyForTermux()) return;

        int executionId = TermuxResultService.nextExecutionId();

        Intent resultIntent = new Intent(this, TermuxResultService.class);
        resultIntent.putExtra(TermuxResultService.EXTRA_EXECUTION_ID, executionId);
        resultIntent.putExtra(TermuxResultService.EXTRA_COMMAND_TYPE, type);

        int flags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getService(
                this, executionId, resultIntent, flags);

        Intent i = baseTermuxIntent(label, command, stdin, background, openSession);
        i.putExtra("pendingIntent", pendingIntent);

        try {
            startService(i);
            details.setText("Sent " + type + " command to Termux. Waiting for verified result...");
        } catch (SecurityException e) {
            prefs.edit().putBoolean("preflight_ok", false).apply();
            showTermuxSetup();
        } catch (Exception e) {
            showError("Termux command failed", e);
        }
    }

    private void runTermuxNoResult(
            String label,
            String command,
            boolean background,
            boolean openSession) {

        if (!readyForTermux()) return;
        Intent i = baseTermuxIntent(label, command, null, background, openSession);

        try {
            startService(i);
        } catch (SecurityException e) {
            showTermuxSetup();
        } catch (Exception e) {
            showError("Termux command failed", e);
        }
    }

    private Intent baseTermuxIntent(
            String label,
            String command,
            String stdin,
            boolean background,
            boolean openSession) {

        Intent i = new Intent();
        i.setClassName(TERMUX_PACKAGE, "com.termux.app.RunCommandService");
        i.setAction("com.termux.RUN_COMMAND");
        i.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
        i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-lc", command});
        i.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        i.putExtra("com.termux.RUN_COMMAND_BACKGROUND", background);
        i.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", openSession ? "0" : "2");
        i.putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", label);
        i.putExtra("com.termux.RUN_COMMAND_COMMAND_DESCRIPTION", label);
        if (stdin != null) i.putExtra("com.termux.RUN_COMMAND_STDIN", stdin);
        return i;
    }

    private void loadPanel() {
        if (webView != null) webView.loadUrl(PANEL_URL);
    }

    private void showError(String title, Exception e) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(e.getClass().getSimpleName() + ": " + e.getMessage())
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            if (fileCallback != null) {
                Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                fileCallback.onReceiveValue(result);
                fileCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
