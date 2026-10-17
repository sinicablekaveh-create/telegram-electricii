package com.telegram.electric;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class GroupSelectionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView view = new TextView(this);
        view.setText("TDLib is not connected. No Telegram groups were loaded.");
        setContentView(view);
    }
}
