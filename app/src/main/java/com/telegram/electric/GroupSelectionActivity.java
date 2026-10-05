package com.telegram.electric;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class GroupSelectionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView view = new TextView(this);
        view.setText("Select Target Group\nGroups will load from TDLib");
        setContentView(view);
    }
}
