package com.telegram.electric;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.telegram.electric.core.tdlib.TdClientManager;

import java.util.List;

public class MainActivity extends Activity implements TdClientManager.Listener {
    private TdClientManager manager;
    private TextView statusView;
    private LinearLayout connectionPanel;
    private LinearLayout chatPanel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manager = TdClientManager.getInstance(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(28, 28, 28, 28);

        TextView title = new TextView(this);
        title.setText("Telegram Electric");
        title.setTextSize(24);
        content.addView(title);

        statusView = new TextView(this);
        statusView.setTextSize(16);
        statusView.setPadding(0, 16, 0, 16);
        content.addView(statusView);

        connectionPanel = new LinearLayout(this);
        connectionPanel.setOrientation(LinearLayout.VERTICAL);
        content.addView(connectionPanel);

        chatPanel = new LinearLayout(this);
        chatPanel.setOrientation(LinearLayout.VERTICAL);
        chatPanel.setVisibility(View.GONE);
        content.addView(chatPanel);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        manager.setListener(this);
        manager.start();
    }

    @Override
    protected void onPause() {
        manager.setListener(null);
        super.onPause();
    }

    @Override
    public void onStateChanged(TdClientManager.State state, String details) {
        statusView.setText(labelFor(state) + (details == null || details.isEmpty() ? "" : "\n" + details));
        renderControls(state);
    }

    @Override
    public void onChatsChanged(List<TdClientManager.Chat> chats) {
        if (manager.getState() != TdClientManager.State.READY) {
            return;
        }
        chatPanel.setVisibility(View.VISIBLE);
        chatPanel.removeAllViews();
        TextView heading = new TextView(this);
        heading.setText("Groups visible to this Telegram account");
        heading.setTextSize(18);
        chatPanel.addView(heading);

        if (chats.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Waiting for Telegram chat updates. Groups will appear here when TDLib loads them.");
            empty.setPadding(0, 12, 0, 12);
            chatPanel.addView(empty);
        } else {
            for (TdClientManager.Chat chat : chats) {
                TextView row = new TextView(this);
                row.setText("• " + chat.title);
                row.setTextSize(16);
                row.setPadding(0, 10, 0, 10);
                chatPanel.addView(row);
            }
        }

        TextView note = new TextView(this);
        note.setText("Messaging is disabled in this alpha. No message is sent from this screen.");
        note.setPadding(0, 18, 0, 8);
        chatPanel.addView(note);
    }

    private void renderControls(TdClientManager.State state) {
        connectionPanel.removeAllViews();
        chatPanel.setVisibility(state == TdClientManager.State.READY ? View.VISIBLE : View.GONE);

        switch (state) {
            case NEEDS_API_CREDENTIALS:
                addText(connectionPanel, "Use the Telegram application credentials you create at my.telegram.org.");
                addText(connectionPanel, "API ID");
                EditText apiId = addInput("API ID", InputType.TYPE_CLASS_NUMBER);
                connectionPanel.addView(apiId);
                addText(connectionPanel, "API hash");
                EditText apiHash = addInput("API hash", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                connectionPanel.addView(apiHash);
                Button saveCredentials = button("Save credentials and continue");
                saveCredentials.setOnClickListener(view -> {
                    try {
                        int value = Integer.parseInt(apiId.getText().toString().trim());
                        manager.submitApiCredentials(value, apiHash.getText().toString());
                    } catch (NumberFormatException error) {
                        statusView.setText("Enter a numeric Telegram API ID.");
                    }
                });
                connectionPanel.addView(saveCredentials);
                TextView link = addText(connectionPanel, "https://my.telegram.org");
                Linkify.addLinks(link, Linkify.WEB_URLS);
                break;
            case WAIT_PHONE:
                addText(connectionPanel, "Enter the phone number for the Telegram account you want to connect.");
                EditText phone = addInput("International phone number, such as +12025550123", InputType.TYPE_CLASS_PHONE);
                connectionPanel.addView(phone);
                Button sendPhone = button("Continue");
                sendPhone.setOnClickListener(view -> manager.submitPhoneNumber(phone.getText().toString()));
                connectionPanel.addView(sendPhone);
                break;
            case WAIT_CODE:
                addText(connectionPanel, "Enter the code Telegram sent to the app or by SMS.");
                EditText code = addInput("Telegram login code", InputType.TYPE_CLASS_NUMBER);
                connectionPanel.addView(code);
                Button submitCode = button("Verify code");
                submitCode.setOnClickListener(view -> manager.submitCode(code.getText().toString()));
                connectionPanel.addView(submitCode);
                break;
            case WAIT_PASSWORD:
                addText(connectionPanel, "Enter your Telegram 2-step verification password.");
                EditText password = addInput("2-step verification password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                connectionPanel.addView(password);
                Button submitPassword = button("Verify password");
                submitPassword.setOnClickListener(view -> manager.submitPassword(password.getText().toString()));
                connectionPanel.addView(submitPassword);
                break;
            case WAIT_EMAIL_ADDRESS:
                addText(connectionPanel, "Telegram requires an email address to continue.");
                EditText email = addInput("Email address", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                connectionPanel.addView(email);
                Button submitEmail = button("Continue");
                submitEmail.setOnClickListener(view -> manager.submitEmailAddress(email.getText().toString()));
                connectionPanel.addView(submitEmail);
                break;
            case WAIT_EMAIL_CODE:
                addText(connectionPanel, "Enter the verification code sent by Telegram to your email.");
                EditText emailCode = addInput("Email verification code", InputType.TYPE_CLASS_TEXT);
                connectionPanel.addView(emailCode);
                Button submitEmailCode = button("Verify email code");
                submitEmailCode.setOnClickListener(view -> manager.submitEmailCode(emailCode.getText().toString()));
                connectionPanel.addView(submitEmailCode);
                break;
            case READY:
                Button logOut = button("Log out of this Telegram account");
                logOut.setOnClickListener(view -> manager.logOut());
                connectionPanel.addView(logOut);
                break;
            case WAIT_REGISTRATION:
                addText(connectionPanel, "This alpha supports connecting existing accounts. It will not register a new Telegram account.");
                break;
            case WAIT_DEVICE_CONFIRMATION:
                TextView confirm = addText(connectionPanel, "Confirm this authorization in Telegram on another device if prompted.");
                Linkify.addLinks(confirm, Linkify.WEB_URLS);
                break;
            case ERROR:
                addText(connectionPanel, "The connection stopped. Review the error above, then restart the app after fixing the build or device issue.");
                break;
            case CLOSED:
                addText(connectionPanel, "The Telegram session is closed. Restart the app to connect again.");
                break;
            case CONNECTING:
                addText(connectionPanel, "Waiting for TDLib and Telegram authorization updates.");
                break;
            case DISCONNECTED:
            default:
                addText(connectionPanel, "Waiting to start Telegram connection.");
                break;
        }
    }

    private String labelFor(TdClientManager.State state) {
        switch (state) {
            case CONNECTING:
                return "Connecting to Telegram";
            case NEEDS_API_CREDENTIALS:
                return "Telegram app credentials needed";
            case WAIT_PHONE:
                return "Telegram phone number needed";
            case WAIT_CODE:
                return "Telegram login code needed";
            case WAIT_PASSWORD:
                return "2-step verification needed";
            case WAIT_EMAIL_ADDRESS:
                return "Telegram email needed";
            case WAIT_EMAIL_CODE:
                return "Email verification needed";
            case WAIT_DEVICE_CONFIRMATION:
                return "Confirm login on another device";
            case WAIT_REGISTRATION:
                return "Telegram account action needed";
            case READY:
                return "Connected to Telegram";
            case CLOSED:
                return "Telegram session closed";
            case ERROR:
                return "Telegram connection error";
            case DISCONNECTED:
            default:
                return "Telegram is disconnected";
        }
    }

    private TextView addText(LinearLayout parent, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setPadding(0, 8, 0, 8);
        parent.addView(view);
        return view;
    }

    private EditText addInput(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setSingleLine(true);
        return input;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        return button;
    }
}
