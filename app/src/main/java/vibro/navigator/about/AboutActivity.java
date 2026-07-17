package vibro.navigator.about;


import vibro.navigator.BuildConfig;
import vibro.navigator.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;

import vibro.navigator.android.theme.AndroidAppTheme;

public class AboutActivity extends Activity {

    public static final String EXTRA_SCROLL_TO_SETTINGS = "vibro.navigator.about.SCROLL_TO_SETTINGS";

    private final AboutDiagnosticRenderScheduler diagnosticRenderScheduler =
            AboutDiagnosticRenderScheduler.mainThread(this::renderDiagnosticSectionNow);

    private AboutSettingsControllers settingsControllers;
    private AboutDiagnosticSection diagnosticSection;
    private AboutSettingsSwitches settingsSwitches;
    private AboutDatabaseBackupActions databaseBackupActions;

    @NonNull
    public static Intent settingsIntent(@NonNull Context context) {
        return new Intent(context, AboutActivity.class)
                .putExtra(EXTRA_SCROLL_TO_SETTINGS, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidAppTheme.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView version = findViewById(R.id.aboutVersion);
        Switch logEnabledSwitch = findViewById(R.id.aboutLogEnabledSwitch);
        Switch autoSaveGpxSwitch = findViewById(R.id.aboutAutoSaveGpxSwitch);
        Switch fusedLocationSwitch = findViewById(R.id.aboutFusedLocationSwitch);
        Switch dynamicGpsFixIntervalSwitch = findViewById(R.id.aboutDynamicGpsFixIntervalSwitch);
        Switch imperialUnitsSwitch = findViewById(R.id.aboutImperialUnitsSwitch);
        Switch lightThemeSwitch = findViewById(R.id.aboutLightThemeSwitch);
        Switch surroundingStreetsSwitch = findViewById(R.id.aboutCompassSurroundingStreetsSwitch);
        Switch navigationNotificationsSwitch = findViewById(R.id.aboutNavigationNotificationsSwitch);
        Switch singleInstructionModeSwitch = findViewById(R.id.aboutSingleInstructionModeSwitch);
        View exportDatabaseButton = findViewById(R.id.aboutExportDatabaseButton);
        View importDatabaseButton = findViewById(R.id.aboutImportDatabaseButton);
        diagnosticSection = new AboutDiagnosticSection(this);
        settingsSwitches = new AboutSettingsSwitches(
                this,
                logEnabledSwitch,
                autoSaveGpxSwitch,
                fusedLocationSwitch,
                dynamicGpsFixIntervalSwitch,
                imperialUnitsSwitch,
                lightThemeSwitch,
                surroundingStreetsSwitch,
                navigationNotificationsSwitch,
                singleInstructionModeSwitch,
                this::scheduleDiagnosticSectionRender
        );
        settingsSwitches.configure();
        settingsControllers = new AboutSettingsControllers(this);
        settingsControllers.configure();
        databaseBackupActions = new AboutDatabaseBackupActions(
                this,
                this::flushDeferredSettings,
                this::renderAfterDatabaseImport
        );
        databaseBackupActions.configure(exportDatabaseButton, importDatabaseButton);
        new AboutSettingInfoButtons(this).configure();
        AboutProjectLinks.configure(this);

        version.setText(getString(R.string.format_version, BuildConfig.VERSION_NAME));
        scrollToSettingsIfRequested();
    }

    @Override
    protected void onResume() {
        super.onResume();
        scheduleDiagnosticSectionRender();
    }

    @Override
    protected void onStart() {
        super.onStart();
        diagnosticRenderScheduler.start();
    }

    @Override
    protected void onStop() {
        flushDeferredSettings();
        diagnosticRenderScheduler.stop();
        diagnosticSection.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (settingsControllers != null) {
            settingsControllers.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        databaseBackupActions.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        settingsSwitches.onRequestPermissionsResult(requestCode, grantResults);
    }

    private void scheduleDiagnosticSectionRender() {
        renderSettingsControls();
        diagnosticRenderScheduler.schedule();
    }

    private void renderDiagnosticSectionNow() {
        renderSettingsControls();
        if (diagnosticRenderScheduler.isStarted()) {
            diagnosticSection.start();
        }
        diagnosticSection.render();
    }

    private void renderSettingsControls() {
        settingsSwitches.render();
    }

    private void scrollToSettingsIfRequested() {
        if (!getIntent().getBooleanExtra(EXTRA_SCROLL_TO_SETTINGS, false)) {
            return;
        }
        ScrollView root = findViewById(R.id.aboutRoot);
        View settingsTitle = findViewById(R.id.aboutSettingsTitle);
        root.post(() -> root.scrollTo(0, settingsTitle.getTop()));
    }

    private void flushDeferredSettings() {
        settingsSwitches.flush();
        if (settingsControllers != null) {
            settingsControllers.flush();
        }
    }

    private void renderAfterDatabaseImport() {
        diagnosticRenderScheduler.renderNow();
        if (settingsControllers != null) {
            settingsControllers.refreshAfterDatabaseImport();
        }
    }

}
