package com.telegram.electric;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    private CoreController coreController;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        coreController = new CoreController();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        status = new TextView(this);
        status.setText("Core Offline");

        Button start = new Button(this);
        start.setText("START CORE");
        start.setOnClickListener(v -> {
            coreController.startCore();
            status.setText("Core Running");
        });

        layout.addView(status);
        layout.addView(start);
        setContentView(layout);
    }
}
