package com.sinicablekaveh.thereeibaru;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
    private WebView webView;
    private TextView status;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        ensureRunPermission();
        handler.postDelayed(this::loadPanel, 700);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(this::loadPanel, 500);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 247, 250));

        TextView title = new TextView(this);
        title.setText("theree ibaru 1.1");
        title.setTextSize(22f);
        title.setTextColor(Color.rgb(22, 48, 82));
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(16), dp(12), dp(16), dp(3));
        root.addView(title);

        status = new TextView(this);
        status.setText("Checking Termux...");
        status.setTextSize(12f);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(dp(16), 0, dp(16), dp(8));
        root.addView(status);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(8), dp(3), dp(8), dp(7));

        bar.addView(makeButton("SETUP TERMUX", v -> showTermuxSetup()));
        bar.addView(makeButton("INSTALL RUNTIME", v -> installRuntime()));
        bar.addView(makeButton("START", v -> startRuntime()));
        bar.addView(makeButton("PREFLIGHT", v -> runPreflight()));
        bar.addView(makeButton("BALE LOGIN", v -> askPhoneAndLogin("bale")));
        bar.addView(makeButton("EITAA LOGIN", v -> askPhoneAndLogin("eitaa")));
        bar.addView(makeButton("RUBIKA LOGIN", v -> askPhoneAndLogin("rubika")));
        bar.addView(makeButton("STOP", v -> stopRuntime()));
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
                status.setText("Panel: " + url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    status.setText("Panel offline. Install/start Runtime, then RELOAD.");
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

    private void ensureRunPermission() {
        if (!isTermuxInstalled()) {
            status.setText("Termux is not installed.");
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(RUN_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{RUN_PERMISSION}, REQ_RUN_PERMISSION);
            status.setText("Grant: Run commands in Termux environment.");
        } else {
            status.setText("Termux command permission is ready.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_RUN_PERMISSION) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                status.setText("Termux RUN_COMMAND permission granted.");
            } else {
                status.setText("RUN_COMMAND permission denied.");
            }
        }
    }

    private void showTermuxSetup() {
        String command =
                "mkdir -p ~/.termux; " +
                "value=true; key=allow-external-apps; file=~/.termux/termux.properties; touch \"$file\"; " +
                "if grep -q '^allow-external-apps=' \"$file\"; then " +
                "sed -i 's/^allow-external-apps=.*/allow-external-apps=true/' \"$file\"; " +
                "else echo 'allow-external-apps=true' >> \"$file\"; fi; " +
                "chmod 700 ~/.termux; termux-reload-settings";

        new AlertDialog.Builder(this)
                .setTitle("One-time Termux setup")
                .setMessage("This is required by Termux itself. Copy the command, run it once in Termux, then return and grant the Additional Permission: Run commands in Termux.\n\n" + command)
                .setPositiveButton("COPY & OPEN TERMUX", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("theree termux setup", command));
                    Intent i = getPackageManager().getLaunchIntentForPackage(TERMUX_PACKAGE);
                    if (i != null) startActivity(i);
                })
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private String readTextAsset(String name) throws Exception {
        InputStream in = getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8).replace("\n", "").replace("\r", "").trim();
    }

    private boolean readyForTermux() {
        if (!isTermuxInstalled()) {
            Toast.makeText(this, "Install Termux first.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(RUN_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            ensureRunPermission();
            Toast.makeText(this, "Grant RUN_COMMAND permission first.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void installRuntime() {
        if (!readyForTermux()) return;
        try {
            String payload = readTextAsset("runtime_v09.b64");
            String command =
                    "set -e; " +
                    "command -v base64 >/dev/null; " +
                    "command -v unzip >/dev/null 2>&1 || pkg install -y unzip; " +
                    "rm -rf \"$HOME/.theree_apk_stage\"; mkdir -p \"$HOME/.theree_apk_stage/unpack\"; " +
                    "base64 -d > \"$HOME/.theree_apk_stage/runtime.zip\"; " +
                    "unzip -q \"$HOME/.theree_apk_stage/runtime.zip\" -d \"$HOME/.theree_apk_stage/unpack\"; " +
                    "rm -rf \"$HOME/theree_ibaru_source\"; " +
                    "cp -a \"$HOME/.theree_apk_stage/unpack/theree_ibaru_source\" \"$HOME/theree_ibaru_source\"; " +
                    "cd \"$HOME/theree_ibaru_source\"; exec bash install.sh";
            status.setText("Runtime installer sent to Termux.");
            runTermux("Install theree ibaru Runtime", command, payload, false, true);
        } catch (Exception e) {
            showError("Runtime asset error", e);
        }
    }

    private void startRuntime() {
        if (!readyForTermux()) return;
        String command =
                "cd \"$HOME/theree_ibaru_source\" 2>/dev/null || { echo 'Runtime not installed'; exit 2; }; " +
                "exec bash scripts/start_all.sh";
        status.setText("Starting Runtime...");
        runTermux("Start theree ibaru", command, null, true, false);
        handler.postDelayed(this::loadPanel, 1800);
        handler.postDelayed(this::loadPanel, 4500);
    }

    private void stopRuntime() {
        if (!readyForTermux()) return;
        String command =
                "pkill -f 'main.py --serve' 2>/dev/null || true; " +
                "pkill -f 'EitaaBun' 2>/dev/null || true; " +
                "pkill -f 'bun.*index' 2>/dev/null || true; " +
                "termux-wake-unlock >/dev/null 2>&1 || true; echo stopped";
        status.setText("Stopping Runtime...");
        runTermux("Stop theree ibaru", command, null, true, false);
    }

    private void runPreflight() {
        if (!readyForTermux()) return;
        String command =
                "cd \"$HOME/theree_ibaru_source\" 2>/dev/null || { echo 'Runtime not installed'; read -r _; exit 2; }; " +
                "bash scripts/diagnose.sh; echo; echo 'Return to theree ibaru when finished.'; read -r _";
        runTermux("theree ibaru Preflight", command, null, false, true);
    }

    private void askPhoneAndLogin(String service) {
        if (!readyForTermux()) return;
        EditText input = new EditText(this);
        input.setHint("+98912...");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle(service.toUpperCase() + " login")
                .setMessage("Enter the account phone. OTP/password is entered only in the Termux login session.")
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
                            "; echo; echo 'Login flow finished. Return to theree ibaru.'; read -r _";
                    runTermux(service + " login", command, null, false, true);
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private String shellQuote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    private void runTermux(String label, String command, String stdin, boolean background, boolean openSession) {
        try {
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
            startService(i);
        } catch (SecurityException e) {
            status.setText("Termux blocked RUN_COMMAND.");
            showTermuxSetup();
        } catch (Exception e) {
            showError("Termux command failed", e);
        }
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
