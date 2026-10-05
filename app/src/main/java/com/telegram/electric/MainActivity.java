package com.telegram.electric;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;

import com.telegram.electric.core.CoreEngine;

public class MainActivity extends Activity {
    private CoreEngine engine;
    private TextView status;
    private TextView target;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        engine = new CoreEngine();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        status = new TextView(this);
        status.setText("Engine Offline");

        target = new TextView(this);
        target.setText("Target Group: Not Selected");

        Button selectGroup = new Button(this);
        selectGroup.setText("SELECT TARGET GROUP");
        selectGroup.setOnClickListener(v -> {
            Intent intent = new Intent(this, GroupSelectionActivity.class);
            startActivity(intent);
        });

        Button start = new Button(this);
        start.setText("START ENGINE");
        start.setOnClickListener(v -> {
            engine.start();
            status.setText("Engine Running");
        });

        Button stop = new Button(this);
        stop.setText("STOP ENGINE");
        stop.setOnClickListener(v -> {
            engine.stop();
            status.setText("Engine Offline");
        });

        layout.addView(status);
        layout.addView(target);
        layout.addView(selectGroup);
        layout.addView(start);
        layout.addView(stop);
        setContentView(layout);
    }
}
