package com.telegram.electric;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.telegram.electric.core.MessageSender;

public class SendActivity extends Activity {
    private MessageSender sender = new MessageSender();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        EditText message = findViewById(R.id.messageText);
        Button send = findViewById(R.id.sendButton);

        send.setOnClickListener(v -> {
            sender.sendMessage(0L, message.getText().toString());
        });
    }
}
