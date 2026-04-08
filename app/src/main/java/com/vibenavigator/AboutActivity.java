package com.vibenavigator;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.vibenavigator.util.AppLogger;

public class AboutActivity extends Activity {

    private static final int DEVELOPER_TAP_COUNT = 5;
    private static final long DEVELOPER_TAP_WINDOW_MS = 2000L;
    private static final long SENSOR_STATUS_REFRESH_INTERVAL_MS = 1000L;

    private final long[] developerTapTimes = new long[DEVELOPER_TAP_COUNT];
    private final Handler sensorStatusHandler = new Handler(Looper.getMainLooper());
    private final Runnable sensorStatusRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            renderDeveloperSection();
            sensorStatusHandler.postDelayed(this, SENSOR_STATUS_REFRESH_INTERVAL_MS);
        }
    };
    private AboutSensorStatusFormatter sensorStatusFormatter;
    private TextView sensorStatusTitle;
    private TextView sensorStatusBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        View root = findViewById(R.id.aboutRoot);
        View content = findViewById(R.id.aboutContent);
        TextView title = findViewById(R.id.aboutTitle);
        TextView version = findViewById(R.id.aboutVersion);
        TextView body = findViewById(R.id.aboutBody);
        sensorStatusTitle = findViewById(R.id.aboutSensorStatusTitle);
        sensorStatusBody = findViewById(R.id.aboutSensorStatusBody);
        sensorStatusFormatter = new AboutSensorStatusFormatter(this);

        version.setText(getString(R.string.format_version, BuildConfig.VERSION_NAME));
        renderDeveloperSection();

        View.OnClickListener developerModeTapListener = v -> registerDeveloperTap();
        root.setOnClickListener(developerModeTapListener);
        content.setOnClickListener(developerModeTapListener);
        title.setOnClickListener(developerModeTapListener);
        version.setOnClickListener(developerModeTapListener);
        body.setOnClickListener(developerModeTapListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderDeveloperSection();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (AppLogger.isDeveloperModeEnabled(this)) {
            sensorStatusFormatter.start();
        }
        sensorStatusHandler.post(sensorStatusRefreshRunnable);
    }

    @Override
    protected void onStop() {
        sensorStatusHandler.removeCallbacks(sensorStatusRefreshRunnable);
        sensorStatusFormatter.stop();
        super.onStop();
    }

    private void registerDeveloperTap() {
        System.arraycopy(developerTapTimes, 1, developerTapTimes, 0, developerTapTimes.length - 1);
        developerTapTimes[developerTapTimes.length - 1] = SystemClock.uptimeMillis();

        long now = developerTapTimes[developerTapTimes.length - 1];
        if (developerTapTimes[0] == 0L || developerTapTimes[0] < now - DEVELOPER_TAP_WINDOW_MS) {
            return;
        }

        if (AppLogger.enableDeveloperMode(this)) {
            Toast.makeText(this, R.string.msg_developer_mode_enabled, Toast.LENGTH_SHORT).show();
            AppLogger.i("AboutActivity", "Developer mode unlocked from About page");
        } else {
            Toast.makeText(this, R.string.msg_developer_mode_already_enabled, Toast.LENGTH_SHORT).show();
        }
        renderDeveloperSection();
        clearDeveloperTapWindow();
    }

    private void renderDeveloperSection() {
        boolean developerModeEnabled = AppLogger.isDeveloperModeEnabled(this);
        int visibility = developerModeEnabled ? View.VISIBLE : View.GONE;
        sensorStatusTitle.setVisibility(visibility);
        sensorStatusBody.setVisibility(visibility);
        if (developerModeEnabled) {
            sensorStatusFormatter.start();
            sensorStatusBody.setText(sensorStatusFormatter.build(this));
        } else {
            sensorStatusFormatter.stop();
            sensorStatusBody.setText(null);
        }
    }

    private void clearDeveloperTapWindow() {
        for (int i = 0; i < developerTapTimes.length; i++) {
            developerTapTimes[i] = 0L;
        }
    }
}
