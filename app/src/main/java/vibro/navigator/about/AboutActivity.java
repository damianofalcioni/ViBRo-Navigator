package vibro.navigator.about;


import vibro.navigator.BuildConfig;
import vibro.navigator.R;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.nav.foreground.NavigationNotificationDebugHelper;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppDataBackup;
import vibro.navigator.settings.AppSettings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AboutActivity extends Activity {

    private static final long SENSOR_STATUS_REFRESH_INTERVAL_MS = 1000L;
    private static final int REQ_EXPORT_DATABASE = 4001;
    private static final int REQ_IMPORT_DATABASE = 4002;

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
    private View googlePoiApiKeyContainer;
    private TextView googlePoiApiKeyEdit;
    private Button googlePoiApiKeySaveButton;
    private Button exportDatabaseButton;
    private Button importDatabaseButton;
    private Button symbolTestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView version = findViewById(R.id.aboutVersion);
        logEnabledSwitch = findViewById(R.id.aboutLogEnabledSwitch);
        fusedLocationSwitch = findViewById(R.id.aboutFusedLocationSwitch);
        imperialUnitsSwitch = findViewById(R.id.aboutImperialUnitsSwitch);
        googlePoiApiKeyContainer = findViewById(R.id.aboutGooglePoiApiKeyContainer);
        googlePoiApiKeyEdit = findViewById(R.id.aboutGooglePoiApiKeyEdit);
        googlePoiApiKeySaveButton = findViewById(R.id.aboutGooglePoiApiKeySaveButton);
        exportDatabaseButton = findViewById(R.id.aboutExportDatabaseButton);
        importDatabaseButton = findViewById(R.id.aboutImportDatabaseButton);
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
        configureGooglePoiApiKeySetting();
        exportDatabaseButton.setOnClickListener(v -> openExportDatabasePicker());
        importDatabaseButton.setOnClickListener(v -> openImportDatabasePicker());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        if (requestCode == REQ_EXPORT_DATABASE) {
            exportDatabase(data.getData());
        } else if (requestCode == REQ_IMPORT_DATABASE) {
            importDatabase(data.getData());
        }
    }

    private void renderDiagnosticSection() {
        logEnabledSwitch.setChecked(AppLogger.isLoggingEnabled(this));
        fusedLocationSwitch.setChecked(DistributionServices.supportsFusedLocation()
                && AppSettings.isFusedLocationEnabled(this));
        imperialUnitsSwitch.setChecked(AppSettings.isImperialUnitsEnabled(this));
        sensorStatusTitle.setVisibility(View.VISIBLE);
        sensorStatusBody.setVisibility(View.VISIBLE);
        symbolTestButton.setVisibility(View.VISIBLE);
        sensorStatusBody.setText(sensorStatusFormatter.build(this));
    }

    private void openExportDatabasePicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(AppDataBackup.MIME_TYPE)
                .putExtra(Intent.EXTRA_TITLE, AppDataBackup.DEFAULT_FILE_NAME);
        startActivityForResult(intent, REQ_EXPORT_DATABASE);
    }

    private void openImportDatabasePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(AppDataBackup.MIME_TYPE);
        startActivityForResult(intent, REQ_IMPORT_DATABASE);
    }

    private void exportDatabase(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) {
                throw new IOException("No output stream for backup document");
            }
            out.write(AppDataBackup.exportJson(this).getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, R.string.msg_database_exported, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            AppLogger.w("AboutActivity", "Failed to export database", e);
            Toast.makeText(this, R.string.msg_database_export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabase(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("No input stream for backup document");
            }
            AppDataBackup.importJson(this, readUtf8(in));
            renderDiagnosticSection();
            renderGooglePoiApiKeySetting();
            Toast.makeText(this, R.string.msg_database_imported, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            AppLogger.w("AboutActivity", "Failed to import database", e);
            Toast.makeText(this, R.string.msg_database_import_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private static String readUtf8(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
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

    private void configureGooglePoiApiKeySetting() {
        boolean supported = DistributionServices.supportsUserGooglePoiApiKey();
        googlePoiApiKeyContainer.setVisibility(supported ? View.VISIBLE : View.GONE);
        googlePoiApiKeyEdit.setEnabled(supported);
        googlePoiApiKeySaveButton.setEnabled(supported);
        if (!supported) {
            return;
        }
        renderGooglePoiApiKeySetting();
        googlePoiApiKeySaveButton.setOnClickListener(v -> saveGooglePoiApiKey());
    }

    private void renderGooglePoiApiKeySetting() {
        if (!DistributionServices.supportsUserGooglePoiApiKey()) {
            return;
        }
        googlePoiApiKeyEdit.setText(AppSettings.getGooglePoiApiKey(this));
    }

    private void saveGooglePoiApiKey() {
        if (!DistributionServices.supportsUserGooglePoiApiKey()) {
            return;
        }
        AppSettings.setGooglePoiApiKey(this, googlePoiApiKeyEdit.getText().toString());
        String savedKey = AppSettings.getGooglePoiApiKey(this);
        googlePoiApiKeyEdit.setText(savedKey);
        Toast.makeText(
                this,
                savedKey.isEmpty()
                        ? R.string.msg_google_poi_api_key_cleared
                        : R.string.msg_google_poi_api_key_saved,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void configureImperialUnitsSwitch() {
        imperialUnitsSwitch.setChecked(AppSettings.isImperialUnitsEnabled(this));
        imperialUnitsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSettings.setImperialUnitsEnabled(this, isChecked);
            renderDiagnosticSection();
        });
    }
}
