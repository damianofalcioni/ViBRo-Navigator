package vibro.navigator.about;

import android.app.Activity;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppCompassSettings;
import vibro.navigator.settings.AppGpxSettings;
import vibro.navigator.settings.AppLocationSettings;
import vibro.navigator.settings.AppNotificationSettings;
import vibro.navigator.settings.AppSettings;
import vibro.navigator.settings.AppThemeSettings;

final class AboutSettingsSwitches {
    static final int REQUEST_SURROUNDING_STREETS_STORAGE = 3001;

    @NonNull
    private final Activity activity;
    @NonNull
    private final Switch logEnabledSwitch;
    @NonNull
    private final Switch autoSaveGpxSwitch;
    @NonNull
    private final Switch fusedLocationSwitch;
    @NonNull
    private final Switch dynamicGpsFixIntervalSwitch;
    @NonNull
    private final Switch imperialUnitsSwitch;
    @NonNull
    private final Switch lightThemeSwitch;
    @NonNull
    private final Switch surroundingStreetsSwitch;
    @NonNull
    private final Switch navigationNotificationsSwitch;
    @NonNull
    private final Runnable afterSettingApplied;
    @NonNull
    private final TaskScheduler settingsChangeScheduler = AndroidTaskScheduler.main();

    private AboutDeferredBooleanSetting logEnabledSetting;
    private AboutDeferredBooleanSetting autoSaveGpxSetting;
    private AboutDeferredBooleanSetting fusedLocationSetting;
    private AboutDeferredBooleanSetting dynamicGpsFixIntervalSetting;
    private AboutDeferredBooleanSetting imperialUnitsSetting;
    private AboutDeferredBooleanSetting lightThemeSetting;
    private AboutDeferredBooleanSetting surroundingStreetsSetting;
    private AboutDeferredBooleanSetting navigationNotificationsSetting;

    AboutSettingsSwitches(
            @NonNull Activity activity,
            @NonNull Switch logEnabledSwitch,
            @NonNull Switch autoSaveGpxSwitch,
            @NonNull Switch fusedLocationSwitch,
            @NonNull Switch dynamicGpsFixIntervalSwitch,
            @NonNull Switch imperialUnitsSwitch,
            @NonNull Switch lightThemeSwitch,
            @NonNull Switch surroundingStreetsSwitch,
            @NonNull Switch navigationNotificationsSwitch,
            @NonNull Runnable afterSettingApplied
    ) {
        this.activity = activity;
        this.logEnabledSwitch = logEnabledSwitch;
        this.autoSaveGpxSwitch = autoSaveGpxSwitch;
        this.fusedLocationSwitch = fusedLocationSwitch;
        this.dynamicGpsFixIntervalSwitch = dynamicGpsFixIntervalSwitch;
        this.imperialUnitsSwitch = imperialUnitsSwitch;
        this.lightThemeSwitch = lightThemeSwitch;
        this.surroundingStreetsSwitch = surroundingStreetsSwitch;
        this.navigationNotificationsSwitch = navigationNotificationsSwitch;
        this.afterSettingApplied = afterSettingApplied;
    }

    void configure() {
        configureLogEnabledSwitch();
        configureAutoSaveGpxSwitch();
        configureFusedLocationSwitch();
        configureDynamicGpsFixIntervalSwitch();
        configureImperialUnitsSwitch();
        configureLightThemeSwitch();
        configureSurroundingStreetsSwitch();
        configureNavigationNotificationsSwitch();
    }

    void render() {
        logEnabledSetting.render(logEnabledSwitch, AppLogger.isLoggingEnabled(activity));
        autoSaveGpxSetting.render(autoSaveGpxSwitch, AppGpxSettings.isAutoSaveOnStopEnabled(activity));
        fusedLocationSetting.render(
                fusedLocationSwitch,
                DistributionServices.supportsFusedLocation() && AppSettings.isFusedLocationEnabled(activity)
        );
        dynamicGpsFixIntervalSetting.render(
                dynamicGpsFixIntervalSwitch,
                AppLocationSettings.isDynamicGpsFixIntervalEnabled(activity)
        );
        imperialUnitsSetting.render(imperialUnitsSwitch, AppSettings.isImperialUnitsEnabled(activity));
        lightThemeSetting.render(lightThemeSwitch, AppThemeSettings.isLightThemeEnabled(activity));
        surroundingStreetsSetting.render(
                surroundingStreetsSwitch,
                AppCompassSettings.isSurroundingStreetsEnabled(activity)
        );
        navigationNotificationsSetting.render(
                navigationNotificationsSwitch,
                AppNotificationSettings.areNavigationNotificationsEnabled(activity)
        );
    }

    void flush() {
        logEnabledSetting.flush();
        autoSaveGpxSetting.flush();
        fusedLocationSetting.flush();
        dynamicGpsFixIntervalSetting.flush();
        imperialUnitsSetting.flush();
        lightThemeSetting.flush(false);
        surroundingStreetsSetting.flush(false);
        navigationNotificationsSetting.flush(false);
    }

    void onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_SURROUNDING_STREETS_STORAGE) {
            return;
        }
        if (AndroidLegacyExternalStorageAccess.isReadPermissionGranted(grantResults)) {
            surroundingStreetsSetting.set(true);
            surroundingStreetsSetting.flush();
            render();
            return;
        }
        AppCompassSettings.setSurroundingStreetsEnabled(activity, false);
        render();
        Toast.makeText(
                activity,
                R.string.msg_compass_surrounding_streets_storage_permission_required,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void configureLogEnabledSwitch() {
        logEnabledSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppLogger.setLoggingEnabled(activity, enabled),
                afterSettingApplied
        );
        logEnabledSetting.render(logEnabledSwitch, AppLogger.isLoggingEnabled(activity));
        logEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                logEnabledSetting.set(isChecked));
    }

    private void configureAutoSaveGpxSwitch() {
        autoSaveGpxSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppGpxSettings.setAutoSaveOnStopEnabled(activity, enabled),
                afterSettingApplied
        );
        autoSaveGpxSetting.render(autoSaveGpxSwitch, AppGpxSettings.isAutoSaveOnStopEnabled(activity));
        autoSaveGpxSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                autoSaveGpxSetting.set(isChecked));
    }

    private void configureFusedLocationSwitch() {
        boolean supported = DistributionServices.supportsFusedLocation();
        fusedLocationSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppSettings.setFusedLocationEnabled(activity, enabled),
                afterSettingApplied
        );
        fusedLocationSwitch.setEnabled(supported);
        fusedLocationSetting.render(fusedLocationSwitch, supported && AppSettings.isFusedLocationEnabled(activity));
        fusedLocationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!DistributionServices.supportsFusedLocation()) {
                buttonView.setChecked(false);
                return;
            }
            fusedLocationSetting.set(isChecked);
        });
    }

    private void configureDynamicGpsFixIntervalSwitch() {
        dynamicGpsFixIntervalSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppLocationSettings.setDynamicGpsFixIntervalEnabled(activity, enabled),
                afterSettingApplied
        );
        dynamicGpsFixIntervalSetting.render(
                dynamicGpsFixIntervalSwitch,
                AppLocationSettings.isDynamicGpsFixIntervalEnabled(activity)
        );
        dynamicGpsFixIntervalSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                dynamicGpsFixIntervalSetting.set(isChecked));
    }

    private void configureImperialUnitsSwitch() {
        imperialUnitsSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppSettings.setImperialUnitsEnabled(activity, enabled),
                afterSettingApplied
        );
        imperialUnitsSetting.render(imperialUnitsSwitch, AppSettings.isImperialUnitsEnabled(activity));
        imperialUnitsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                imperialUnitsSetting.set(isChecked));
    }

    private void configureLightThemeSwitch() {
        lightThemeSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppThemeSettings.setLightThemeEnabled(activity, enabled),
                this::recreateForThemeChange
        );
        lightThemeSetting.render(lightThemeSwitch, AppThemeSettings.isLightThemeEnabled(activity));
        lightThemeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                lightThemeSetting.set(isChecked));
    }

    private void configureSurroundingStreetsSwitch() {
        surroundingStreetsSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppCompassSettings.setSurroundingStreetsEnabled(activity, enabled),
                afterSettingApplied
        );
        surroundingStreetsSetting.render(
                surroundingStreetsSwitch,
                AppCompassSettings.isSurroundingStreetsEnabled(activity)
        );
        surroundingStreetsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && AndroidLegacyExternalStorageAccess.shouldRequestReadPermission(activity)) {
                AndroidLegacyExternalStorageAccess.requestReadPermission(
                        activity,
                        REQUEST_SURROUNDING_STREETS_STORAGE
                );
                return;
            }
            surroundingStreetsSetting.set(isChecked);
        });
    }

    private void configureNavigationNotificationsSwitch() {
        navigationNotificationsSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppNotificationSettings.setNavigationNotificationsEnabled(activity, enabled),
                afterSettingApplied
        );
        navigationNotificationsSetting.render(
                navigationNotificationsSwitch,
                AppNotificationSettings.areNavigationNotificationsEnabled(activity)
        );
        navigationNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                navigationNotificationsSetting.set(isChecked));
    }

    private void recreateForThemeChange() {
        if (!activity.isFinishing()) {
            activity.recreate();
        }
    }
}
