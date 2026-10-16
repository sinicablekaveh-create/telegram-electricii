package com.telegram.electric;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class SendActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        EditText message = findViewById(R.id.messageText);
        message.setHint("Sending is unavailable until TDLib is connected");

        Button send = findViewById(R.id.sendButton);
        send.setText("UNAVAILABLE — TDLib NOT CONNECTED");
        send.setEnabled(false);
    }
}
