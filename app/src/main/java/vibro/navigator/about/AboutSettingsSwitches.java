package vibro.navigator.about;

import android.app.Activity;
import android.widget.Switch;

import androidx.annotation.NonNull;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppSettings;
import vibro.navigator.settings.AppThemeSettings;

final class AboutSettingsSwitches {

    @NonNull
    private final Activity activity;
    @NonNull
    private final Switch logEnabledSwitch;
    @NonNull
    private final Switch fusedLocationSwitch;
    @NonNull
    private final Switch imperialUnitsSwitch;
    @NonNull
    private final Switch lightThemeSwitch;
    @NonNull
    private final Runnable afterSettingApplied;
    @NonNull
    private final TaskScheduler settingsChangeScheduler = AndroidTaskScheduler.main();

    private AboutDeferredBooleanSetting logEnabledSetting;
    private AboutDeferredBooleanSetting fusedLocationSetting;
    private AboutDeferredBooleanSetting imperialUnitsSetting;
    private AboutDeferredBooleanSetting lightThemeSetting;

    AboutSettingsSwitches(
            @NonNull Activity activity,
            @NonNull Switch logEnabledSwitch,
            @NonNull Switch fusedLocationSwitch,
            @NonNull Switch imperialUnitsSwitch,
            @NonNull Switch lightThemeSwitch,
            @NonNull Runnable afterSettingApplied
    ) {
        this.activity = activity;
        this.logEnabledSwitch = logEnabledSwitch;
        this.fusedLocationSwitch = fusedLocationSwitch;
        this.imperialUnitsSwitch = imperialUnitsSwitch;
        this.lightThemeSwitch = lightThemeSwitch;
        this.afterSettingApplied = afterSettingApplied;
    }

    void configure() {
        configureLogEnabledSwitch();
        configureFusedLocationSwitch();
        configureImperialUnitsSwitch();
        configureLightThemeSwitch();
    }

    void render() {
        logEnabledSetting.render(logEnabledSwitch, AppLogger.isLoggingEnabled(activity));
        fusedLocationSetting.render(
                fusedLocationSwitch,
                DistributionServices.supportsFusedLocation() && AppSettings.isFusedLocationEnabled(activity)
        );
        imperialUnitsSetting.render(imperialUnitsSwitch, AppSettings.isImperialUnitsEnabled(activity));
        lightThemeSetting.render(lightThemeSwitch, AppThemeSettings.isLightThemeEnabled(activity));
    }

    void flush() {
        logEnabledSetting.flush();
        fusedLocationSetting.flush();
        imperialUnitsSetting.flush();
        lightThemeSetting.flush(false);
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

    private void recreateForThemeChange() {
        if (!activity.isFinishing()) {
            activity.recreate();
        }
    }
}
