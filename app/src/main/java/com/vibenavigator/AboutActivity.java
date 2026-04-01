package com.vibenavigator;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView version = findViewById(R.id.aboutVersion);
        version.setText(getString(R.string.format_version, BuildConfig.VERSION_NAME));
    }
}
