package vibro.navigator.about;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewStub;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;

import vibro.navigator.BuildConfig;
import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.dispatch.TaskScheduler;

public class AboutActivity extends Activity {

    public static final String EXTRA_SCROLL_TO_SETTINGS = "vibro.navigator.about.SCROLL_TO_SETTINGS";
    static final long SETTINGS_INITIALIZATION_DELAY_MS = 75L;

    private final AboutDiagnosticRenderScheduler diagnosticRenderScheduler =
            AboutDiagnosticRenderScheduler.mainThread(this::renderDiagnosticSectionNow);
    @NonNull
    private final TaskScheduler settingsInitializationScheduler = AndroidTaskScheduler.main();
    @NonNull
    private final Runnable initializeSettings = this::initializeSettingsSection;

    private AboutSettingsControllers settingsControllers;
    private AboutDiagnosticSection diagnosticSection;
    private AboutSettingsSwitches settingsSwitches;
    private AboutDatabaseBackupActions databaseBackupActions;
    private boolean settingsInitializationScheduled;
    final AboutOutputSavingSettings outputSaving = new AboutOutputSavingSettings(this, this::scheduleDiagnosticSectionRender);

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
        outputSaving.restoreState(savedInstanceState);

        TextView version = findViewById(R.id.aboutVersion);
        AboutProjectLinks.configure(this);

        version.setText(getString(R.string.format_version, BuildConfig.VERSION_NAME));
        if (shouldScrollToSettings()) {
            initializeSettingsSection();
        } else {
            scheduleSettingsInitialization();
        }
        scrollToSettingsIfRequested();
    }

    private void initializeSettingsSection() {
        settingsInitializationScheduled = false;
        if (settingsSwitches != null) {
            return;
        }
        inflateSettingsSectionIfNeeded();
        Switch logEnabledSwitch = findViewById(R.id.aboutLogEnabledSwitch);
        Switch autoSaveGpxSwitch = findViewById(R.id.aboutAutoSaveGpxSwitch);
        Switch fusedLocationSwitch = findViewById(R.id.aboutFusedLocationSwitch);
        Switch dynamicGpsFixIntervalSwitch = findViewById(R.id.aboutDynamicGpsFixIntervalSwitch);
        Switch imperialUnitsSwitch = findViewById(R.id.aboutImperialUnitsSwitch);
        Switch lightThemeSwitch = findViewById(R.id.aboutLightThemeSwitch);
        Switch surroundingStreetsSwitch = findViewById(R.id.aboutCompassSurroundingStreetsSwitch);
        Switch compassInstantZoomSwitch = findViewById(R.id.aboutCompassInstantZoomSwitch);
        Switch compassStationaryFullRouteZoomSwitch =
                findViewById(R.id.aboutCompassStationaryFullRouteZoomSwitch);
        Switch compassFullscreenRouteSwitch = findViewById(R.id.aboutCompassFullscreenRouteSwitch);
        Switch compassDistanceCirclesSwitch = findViewById(R.id.aboutCompassDistanceCirclesSwitch);
        Switch navigationNotificationsSwitch = findViewById(R.id.aboutNavigationNotificationsSwitch);
        Switch singleInstructionModeSwitch = findViewById(R.id.aboutSingleInstructionModeSwitch);
        Switch navigationCustomButtonSwitch = findViewById(R.id.aboutNavigationCustomButtonSwitch);
        View exportDatabaseButton = findViewById(R.id.aboutExportDatabaseButton);
        View importDatabaseButton = findViewById(R.id.aboutImportDatabaseButton);
        settingsSwitches = new AboutSettingsSwitches(
                this,
                logEnabledSwitch,
                autoSaveGpxSwitch,
                fusedLocationSwitch,
                dynamicGpsFixIntervalSwitch,
                imperialUnitsSwitch,
                lightThemeSwitch,
                surroundingStreetsSwitch,
                compassInstantZoomSwitch,
                compassStationaryFullRouteZoomSwitch,
                compassFullscreenRouteSwitch,
                compassDistanceCirclesSwitch,
                navigationNotificationsSwitch,
                singleInstructionModeSwitch,
                navigationCustomButtonSwitch,
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
        if (!shouldScrollToSettings()) {
            scheduleSettingsInitialization();
        }
    }

    @Override
    protected void onStop() {
        flushDeferredSettings();
        cancelSettingsInitialization();
        diagnosticRenderScheduler.stop();
        if (diagnosticSection != null) {
            diagnosticSection.stop();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        cancelSettingsInitialization();
        if (diagnosticSection != null) {
            diagnosticSection.shutdown();
        }
        if (settingsControllers != null) {
            settingsControllers.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        initializeSettingsSection();
        if (outputSaving.handleActivityResult(requestCode, resultCode, data)
                || settingsControllers.handleActivityResult(requestCode, resultCode, data)) {
            scheduleDiagnosticSectionRender();
            return;
        }
        AboutActivityResultHandlers.handleActivityResult(
                settingsSwitches,
                diagnosticSection,
                databaseBackupActions,
                requestCode,
                resultCode,
                data,
                this::scheduleDiagnosticSectionRender
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        initializeSettingsSection();
        outputSaving.handlePermissionResult(requestCode);
        settingsControllers.handlePermissionResult(requestCode);
        AboutActivityResultHandlers.handlePermissionResult(
                settingsSwitches,
                diagnosticSection,
                requestCode,
                grantResults,
                this::scheduleDiagnosticSectionRender
        );
    }

    private void scheduleDiagnosticSectionRender() {
        diagnosticRenderScheduler.schedule();
    }

    private void renderDiagnosticSectionNow() {
        if (settingsSwitches != null) {
            settingsSwitches.render();
            settingsControllers.renderStorage();
        }
        AboutDiagnosticSection section = diagnosticSection();
        if (diagnosticRenderScheduler.isStarted()) {
            section.start();
        }
        section.render();
    }

    void requestOutputStorageAccess() {
        initializeSettingsSection();
        settingsControllers.requestOutputStorageAccess();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle state) {
        flushDeferredSettings();
        outputSaving.saveState(state);
        super.onSaveInstanceState(state);
    }

    private void scrollToSettingsIfRequested() {
        if (!shouldScrollToSettings()) {
            return;
        }
        ScrollView root = findViewById(R.id.aboutRoot);
        View settingsTitle = findViewById(R.id.aboutSettingsTitle);
        AboutScrollTarget.scrollToOnPreDraw(root, settingsTitle);
    }

    void flushDeferredSettings() {
        if (settingsSwitches != null) {
            settingsSwitches.flush();
            settingsControllers.flush();
        }
        outputSaving.flush();
    }

    private void renderAfterDatabaseImport() {
        initializeSettingsSection();
        diagnosticRenderScheduler.renderNow();
        settingsControllers.refreshAfterDatabaseImport();
    }

    @NonNull
    private AboutDiagnosticSection diagnosticSection() {
        if (diagnosticSection == null) {
            inflateDiagnosticSectionIfNeeded();
            diagnosticSection = new AboutDiagnosticSection(this);
        }
        return diagnosticSection;
    }

    private void inflateDiagnosticSectionIfNeeded() {
        ViewStub diagnosticStub = findViewById(R.id.aboutDiagnosticsStub);
        if (diagnosticStub != null) {
            diagnosticStub.inflate();
        }
    }

    private void inflateSettingsSectionIfNeeded() {
        ViewStub settingsStub = findViewById(R.id.aboutSettingsStub);
        if (settingsStub != null) {
            settingsStub.inflate();
        }
    }

    private void scheduleSettingsInitialization() {
        if (settingsInitializationScheduled || settingsSwitches != null) {
            return;
        }
        settingsInitializationScheduled = true;
        settingsInitializationScheduler.postDelayed(
                initializeSettings,
                SETTINGS_INITIALIZATION_DELAY_MS
        );
    }

    private void cancelSettingsInitialization() {
        settingsInitializationScheduled = false;
        settingsInitializationScheduler.removeCallbacks(initializeSettings);
    }

    private boolean shouldScrollToSettings() {
        return getIntent().getBooleanExtra(EXTRA_SCROLL_TO_SETTINGS, false);
    }

}
