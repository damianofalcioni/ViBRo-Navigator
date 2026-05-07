package vibro.navigator.about;


import vibro.navigator.BuildConfig;
import vibro.navigator.R;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.nav.foreground.NavigationNotificationDebugHelper;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppSettings;

public class AboutActivity extends Activity {

    private static final long SENSOR_STATUS_REFRESH_INTERVAL_MS = 1000L;

    private final Handler sensorStatusHandler = new Handler(Looper.getMainLooper());
    private final Runnable sensorStatusRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            renderDiagnosticSection();
            sensorStatusHandler.postDelayed(this, SENSOR_STATUS_REFRESH_INTERVAL_MS);
        }
    };
    private AboutSensorStatusFormatter sensorStatusFormatter;
    private TextView sensorStatusTitle;
    private TextView sensorStatusBody;
    private Switch logEnabledSwitch;
    private Switch fusedLocationSwitch;
    private Switch imperialUnitsSwitch;
    private Button symbolTestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView version = findViewById(R.id.aboutVersion);
        logEnabledSwitch = findViewById(R.id.aboutLogEnabledSwitch);
        fusedLocationSwitch = findViewById(R.id.aboutFusedLocationSwitch);
        imperialUnitsSwitch = findViewById(R.id.aboutImperialUnitsSwitch);
        sensorStatusTitle = findViewById(R.id.aboutSensorStatusTitle);
        sensorStatusBody = findViewById(R.id.aboutSensorStatusBody);
        symbolTestButton = findViewById(R.id.aboutSymbolTestButton);
        sensorStatusFormatter = new AboutSensorStatusFormatter(this);
        logEnabledSwitch.setChecked(AppLogger.isLoggingEnabled(this));
        logEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppLogger.setLoggingEnabled(this, isChecked);
            renderDiagnosticSection();
        });
        configureFusedLocationSwitch();
        configureImperialUnitsSwitch();
        symbolTestButton.setOnClickListener(v -> sendSymbolTestNotification());

        version.setText(getString(R.string.format_version, BuildConfig.VERSION_NAME));
        renderDiagnosticSection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderDiagnosticSection();
    }

    @Override
    protected void onStart() {
        super.onStart();
        sensorStatusFormatter.start();
        sensorStatusHandler.post(sensorStatusRefreshRunnable);
    }

    @Override
    protected void onStop() {
        sensorStatusHandler.removeCallbacks(sensorStatusRefreshRunnable);
        sensorStatusFormatter.stop();
        super.onStop();
    }

    private void renderDiagnosticSection() {
        logEnabledSwitch.setChecked(AppLogger.isLoggingEnabled(this));
        fusedLocationSwitch.setChecked(DistributionServices.supportsFusedLocation()
                && AppSettings.isFusedLocationEnabled(this));
        imperialUnitsSwitch.setChecked(AppSettings.isImperialUnitsEnabled(this));
        sensorStatusTitle.setVisibility(android.view.View.VISIBLE);
        sensorStatusBody.setVisibility(android.view.View.VISIBLE);
        symbolTestButton.setVisibility(android.view.View.VISIBLE);
        sensorStatusBody.setText(sensorStatusFormatter.build(this));
    }

    private void sendSymbolTestNotification() {
        NavigationNotificationDebugHelper.postSymbolTestNotification(this);
        Toast.makeText(this, R.string.msg_symbol_test_notification_sent, Toast.LENGTH_SHORT).show();
    }

    private void configureFusedLocationSwitch() {
        boolean supported = DistributionServices.supportsFusedLocation();
        fusedLocationSwitch.setEnabled(supported);
        fusedLocationSwitch.setChecked(supported && AppSettings.isFusedLocationEnabled(this));
        fusedLocationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!DistributionServices.supportsFusedLocation()) {
                buttonView.setChecked(false);
                return;
            }
            AppSettings.setFusedLocationEnabled(this, isChecked);
            renderDiagnosticSection();
        });
    }

    private void configureImperialUnitsSwitch() {
        imperialUnitsSwitch.setChecked(AppSettings.isImperialUnitsEnabled(this));
        imperialUnitsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSettings.setImperialUnitsEnabled(this, isChecked);
            renderDiagnosticSection();
        });
    }
}
