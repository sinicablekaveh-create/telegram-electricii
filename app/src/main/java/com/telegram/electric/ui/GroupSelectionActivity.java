package com.telegram.electric.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.telegram.electric.core.GroupSelectionManager;

public class GroupSelectionActivity extends Activity {
    private final GroupSelectionManager manager = new GroupSelectionManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("Target Group Selection");

        Button select = new Button(this);
        select.setText("SELECT GROUP");
        select.setOnClickListener(v -> manager.selectTarget(0));

        layout.addView(title);
        layout.addView(select);
        setContentView(layout);
    }
}
