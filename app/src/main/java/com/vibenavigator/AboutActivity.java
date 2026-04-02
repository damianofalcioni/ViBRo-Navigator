package com.vibenavigator;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.vibenavigator.util.AppLogger;

public class AboutActivity extends AppCompatActivity {

    private static final int DEVELOPER_TAP_COUNT = 5;
    private static final long DEVELOPER_TAP_WINDOW_MS = 2000L;

    private final long[] developerTapTimes = new long[DEVELOPER_TAP_COUNT];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        View root = findViewById(R.id.aboutRoot);
        View content = findViewById(R.id.aboutContent);
        TextView title = findViewById(R.id.aboutTitle);
        TextView version = findViewById(R.id.aboutVersion);
        TextView body = findViewById(R.id.aboutBody);

        version.setText(getString(R.string.format_version, BuildConfig.VERSION_NAME));

        View.OnClickListener developerModeTapListener = v -> registerDeveloperTap();
        root.setOnClickListener(developerModeTapListener);
        content.setOnClickListener(developerModeTapListener);
        title.setOnClickListener(developerModeTapListener);
        version.setOnClickListener(developerModeTapListener);
        body.setOnClickListener(developerModeTapListener);
    }

    private void registerDeveloperTap() {
        System.arraycopy(developerTapTimes, 1, developerTapTimes, 0, developerTapTimes.length - 1);
        developerTapTimes[developerTapTimes.length - 1] = SystemClock.uptimeMillis();

        long now = developerTapTimes[developerTapTimes.length - 1];
        if (developerTapTimes[0] < now - DEVELOPER_TAP_WINDOW_MS) {
            return;
        }

        if (AppLogger.enableDeveloperMode(this)) {
            Toast.makeText(this, R.string.msg_developer_mode_enabled, Toast.LENGTH_SHORT).show();
            AppLogger.i("AboutActivity", "Developer mode unlocked from About page");
        } else if (AppLogger.isDeveloperModeEnabled(this)) {
            Toast.makeText(this, R.string.msg_developer_mode_restarted, Toast.LENGTH_SHORT).show();
        }
        clearDeveloperTapWindow();
    }

    private void clearDeveloperTapWindow() {
        for (int i = 0; i < developerTapTimes.length; i++) {
            developerTapTimes[i] = 0L;
        }
    }
}
