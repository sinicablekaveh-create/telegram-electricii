package com.telegram.electric;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);

        TextView status = new TextView(this);
        status.setText("TDLib is not connected. This build cannot log in or send messages.");

        TextView target = new TextView(this);
        target.setText("Group list unavailable until TDLib chat sync is implemented.");

        Button selectGroup = new Button(this);
        selectGroup.setText("GROUP SELECTION UNAVAILABLE");
        selectGroup.setEnabled(false);

        Button start = new Button(this);
        start.setText("ENGINE UNAVAILABLE");
        start.setEnabled(false);

        layout.addView(status);
        layout.addView(target);
        layout.addView(selectGroup);
        layout.addView(start);
        setContentView(layout);
    }
}
