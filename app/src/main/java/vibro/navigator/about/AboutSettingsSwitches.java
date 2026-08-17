package vibro.navigator.about;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsTreeAccessPrompt;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppCompassSettings;
import vibro.navigator.settings.AppGpxSettings;
import vibro.navigator.settings.AppLocationSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNotificationSettings;
import vibro.navigator.settings.AppSettings;
import vibro.navigator.settings.AppThemeSettings;

final class AboutSettingsSwitches {
    static final int REQUEST_SURROUNDING_STREETS_STORAGE = 3001;
    static final int REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE = 3003;
    private static final String TAG = "AboutSettings";

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
    private final Switch compassInstantZoomSwitch;
    @NonNull
    private final Switch compassStationaryFullRouteZoomSwitch;
    @NonNull
    private final Switch compassFullscreenRouteSwitch;
    @NonNull
    private final Switch navigationNotificationsSwitch;
    @NonNull
    private final Switch singleInstructionModeSwitch;
    @NonNull
    private final Switch navigationCustomButtonSwitch;
    @NonNull
    private final Runnable afterSettingApplied;
    @NonNull
    private final TaskScheduler settingsChangeScheduler = AndroidTaskScheduler.main();
    @NonNull
    private final BRouterProfilesRepository profilesRepository =
            AndroidBRouterProfilesRepositoryFactory.create();
    @NonNull
    private final BRouterSegmentsRepository segmentsRepository =
            AndroidBRouterSegmentsRepositoryFactory.create();

    private AboutDeferredBooleanSetting logEnabledSetting;
    private AboutDeferredBooleanSetting autoSaveGpxSetting;
    private AboutDeferredBooleanSetting fusedLocationSetting;
    private AboutDeferredBooleanSetting dynamicGpsFixIntervalSetting;
    private AboutDeferredBooleanSetting imperialUnitsSetting;
    private AboutDeferredBooleanSetting lightThemeSetting;
    private AboutDeferredBooleanSetting surroundingStreetsSetting;
    private AboutDeferredBooleanSetting compassInstantZoomSetting;
    private AboutDeferredBooleanSetting compassStationaryFullRouteZoomSetting;
    private AboutDeferredBooleanSetting compassFullscreenRouteSetting;
    private AboutDeferredBooleanSetting navigationNotificationsSetting;
    private AboutDeferredBooleanSetting singleInstructionModeSetting;
    private AboutDeferredBooleanSetting navigationCustomButtonSetting;
    private boolean waitingForSurroundingStreetSegmentsTree;

    AboutSettingsSwitches(
            @NonNull Activity activity,
            @NonNull Switch logEnabledSwitch,
            @NonNull Switch autoSaveGpxSwitch,
            @NonNull Switch fusedLocationSwitch,
            @NonNull Switch dynamicGpsFixIntervalSwitch,
            @NonNull Switch imperialUnitsSwitch,
            @NonNull Switch lightThemeSwitch,
            @NonNull Switch surroundingStreetsSwitch,
            @NonNull Switch compassInstantZoomSwitch,
            @NonNull Switch compassStationaryFullRouteZoomSwitch,
            @NonNull Switch compassFullscreenRouteSwitch,
            @NonNull Switch navigationNotificationsSwitch,
            @NonNull Switch singleInstructionModeSwitch,
            @NonNull Switch navigationCustomButtonSwitch,
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
        this.compassInstantZoomSwitch = compassInstantZoomSwitch;
        this.compassStationaryFullRouteZoomSwitch = compassStationaryFullRouteZoomSwitch;
        this.compassFullscreenRouteSwitch = compassFullscreenRouteSwitch;
        this.navigationNotificationsSwitch = navigationNotificationsSwitch;
        this.singleInstructionModeSwitch = singleInstructionModeSwitch;
        this.navigationCustomButtonSwitch = navigationCustomButtonSwitch;
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
        configureCompassInstantZoomSwitch();
        configureCompassStationaryFullRouteZoomSwitch();
        configureCompassFullscreenRouteSwitch();
        configureNavigationNotificationsSwitch();
        configureSingleInstructionModeSwitch();
        configureNavigationCustomButtonSwitch();
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
                storedSurroundingStreetsValue()
        );
        compassInstantZoomSetting.render(
                compassInstantZoomSwitch,
                AppCompassSettings.isInstantZoomEnabled(activity)
        );
        compassStationaryFullRouteZoomSetting.render(
                compassStationaryFullRouteZoomSwitch,
                AppCompassSettings.isStationaryFullRouteZoomEnabled(activity)
        );
        compassFullscreenRouteSetting.render(
                compassFullscreenRouteSwitch,
                AppCompassSettings.isFullscreenRouteEnabled(activity)
        );
        navigationNotificationsSetting.render(
                navigationNotificationsSwitch,
                AppNotificationSettings.areNavigationNotificationsEnabled(activity)
        );
        singleInstructionModeSetting.render(
                singleInstructionModeSwitch,
                AppNotificationSettings.isSingleInstructionModeEnabled(activity)
        );
        navigationCustomButtonSetting.render(
                navigationCustomButtonSwitch,
                AppNavigationCustomButtonSettings.isEnabled(activity)
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
        compassInstantZoomSetting.flush(false);
        compassStationaryFullRouteZoomSetting.flush(false);
        compassFullscreenRouteSetting.flush(false);
        navigationNotificationsSetting.flush(false);
        singleInstructionModeSetting.flush(false);
        navigationCustomButtonSetting.flush(false);
    }

    void onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_SURROUNDING_STREETS_STORAGE) {
            return;
        }
        if (AndroidLegacyExternalStorageAccess.isReadPermissionGranted(grantResults)) {
            if (!isBRouterInstalled()) {
                denySurroundingStreetsBRouterMissing();
                return;
            }
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

    boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE) {
            return false;
        }
        if (!waitingForSurroundingStreetSegmentsTree) {
            return true;
        }
        waitingForSurroundingStreetSegmentsTree = false;
        Uri uri = persistSegmentsTreeResultUri(resultCode, data);
        if (uri == null) {
            denySurroundingStreetsStorage();
            return true;
        }
        if (!isBRouterInstalled()) {
            denySurroundingStreetsBRouterMissing();
            return true;
        }
        segmentsRepository.saveSegmentsTreeUri(activity, uri);
        surroundingStreetsSetting.set(true);
        surroundingStreetsSetting.flush();
        render();
        return true;
    }

    @Nullable
    private Uri persistSegmentsTreeResultUri(int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return null;
        }
        Uri uri = data.getData();
        return AndroidDocumentAccess.persistReadPermission(activity, data, uri) ? uri : null;
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
                storedSurroundingStreetsValue()
        );
        surroundingStreetsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isBRouterInstalled()) {
                denySurroundingStreetsBRouterMissing();
                return;
            }
            if (isChecked && AndroidLegacyExternalStorageAccess.shouldRequestReadPermission(activity)) {
                AndroidLegacyExternalStorageAccess.requestReadPermission(
                        activity,
                        REQUEST_SURROUNDING_STREETS_STORAGE
                );
                return;
            }
            if (isChecked && shouldRequestSurroundingStreetSegmentsTree()) {
                startSurroundingStreetSegmentsTreePicker();
                return;
            }
            surroundingStreetsSetting.set(isChecked);
        });
    }

    private boolean storedSurroundingStreetsValue() {
        return AppCompassSettings.isSurroundingStreetsEnabled(activity) && isBRouterInstalled();
    }

    private boolean shouldRequestSurroundingStreetSegmentsTree() {
        return !AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()
                && !segmentsRepository.hasPersistedSegmentsTreeAccess(activity);
    }

    private void startSurroundingStreetSegmentsTreePicker() {
        AndroidBRouterSegmentsTreeAccessPrompt.show(
                activity,
                segmentsRepository,
                REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE,
                TAG,
                () -> waitingForSurroundingStreetSegmentsTree = true,
                this::denySurroundingStreetsStorage
        );
    }

    private void denySurroundingStreetsBRouterMissing() {
        AppCompassSettings.setSurroundingStreetsEnabled(activity, false);
        render();
        Toast.makeText(
                activity,
                R.string.msg_surrounding_streets_brouter_required,
                Toast.LENGTH_SHORT
        ).show();
    }

    private boolean isBRouterInstalled() {
        return profilesRepository.isBRouterInstalled(activity);
    }

    private void denySurroundingStreetsStorage() {
        AppCompassSettings.setSurroundingStreetsEnabled(activity, false);
        render();
        Toast.makeText(
                activity,
                R.string.msg_compass_surrounding_streets_storage_permission_required,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void configureCompassInstantZoomSwitch() {
        compassInstantZoomSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppCompassSettings.setInstantZoomEnabled(activity, enabled),
                afterSettingApplied
        );
        compassInstantZoomSetting.render(
                compassInstantZoomSwitch,
                AppCompassSettings.isInstantZoomEnabled(activity)
        );
        compassInstantZoomSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                compassInstantZoomSetting.set(isChecked));
    }

    private void configureCompassStationaryFullRouteZoomSwitch() {
        compassStationaryFullRouteZoomSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppCompassSettings.setStationaryFullRouteZoomEnabled(activity, enabled),
                afterSettingApplied
        );
        compassStationaryFullRouteZoomSetting.render(
                compassStationaryFullRouteZoomSwitch,
                AppCompassSettings.isStationaryFullRouteZoomEnabled(activity)
        );
        compassStationaryFullRouteZoomSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                compassStationaryFullRouteZoomSetting.set(isChecked));
    }

    private void configureCompassFullscreenRouteSwitch() {
        compassFullscreenRouteSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppCompassSettings.setFullscreenRouteEnabled(activity, enabled),
                afterSettingApplied
        );
        compassFullscreenRouteSetting.render(
                compassFullscreenRouteSwitch,
                AppCompassSettings.isFullscreenRouteEnabled(activity)
        );
        compassFullscreenRouteSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                compassFullscreenRouteSetting.set(isChecked));
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

    private void configureSingleInstructionModeSwitch() {
        singleInstructionModeSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppNotificationSettings.setSingleInstructionModeEnabled(activity, enabled),
                afterSettingApplied
        );
        singleInstructionModeSetting.render(
                singleInstructionModeSwitch,
                AppNotificationSettings.isSingleInstructionModeEnabled(activity)
        );
        singleInstructionModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                singleInstructionModeSetting.set(isChecked));
    }

    private void configureNavigationCustomButtonSwitch() {
        navigationCustomButtonSetting = new AboutDeferredBooleanSetting(
                settingsChangeScheduler,
                enabled -> AppNavigationCustomButtonSettings.setEnabled(activity, enabled),
                afterSettingApplied
        );
        navigationCustomButtonSetting.render(
                navigationCustomButtonSwitch,
                AppNavigationCustomButtonSettings.isEnabled(activity)
        );
        navigationCustomButtonSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                navigationCustomButtonSetting.set(isChecked));
    }

    private void recreateForThemeChange() {
        if (!activity.isFinishing()) {
            activity.recreate();
        }
    }

}
