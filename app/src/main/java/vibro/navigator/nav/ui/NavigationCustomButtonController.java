package vibro.navigator.nav.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterFolderAccess;
import vibro.navigator.android.brouter.AndroidBRouterPickerRequest;
import vibro.navigator.android.brouter.AndroidBRouterPickerRequest.Completion;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsTreeAccessPrompt;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;
import vibro.navigator.settings.AppNavigationCustomButtonTargetState;

final class NavigationCustomButtonController {
    static final int REQUEST_SURROUNDING_STREETS_STORAGE = 3002;
    static final int REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE = 3004;

    private static final String TAG = "NavCustomButton";
    private static final long THEME_RECREATE_DELAY_MS = 100L;

    interface Host {
        @Nullable
        NavigationServiceBinder currentBinder();

        void refreshNavigationUiSettings();
    }

    @NonNull
    private final Activity activity;
    @NonNull
    private final TaskScheduler uiScheduler;
    @NonNull
    private final Host host;
    @NonNull
    private final BRouterProfilesRepository profilesRepository =
            AndroidBRouterProfilesRepositoryFactory.create();
    @NonNull
    private final BRouterSegmentsRepository segmentsRepository =
            AndroidBRouterSegmentsRepositoryFactory.create();
    @NonNull
    private final Runnable recreateForThemeChange = this::recreateForThemeChangeIfActive;
    private boolean waitingForSurroundingStreetPermission;
    private final StreetPicker streetPicker;

    NavigationCustomButtonController(
            @NonNull Activity activity,
            @NonNull TaskScheduler uiScheduler,
            @NonNull Host host
    ) {
        this.activity = activity;
        this.uiScheduler = uiScheduler;
        this.host = host;
        streetPicker = new StreetPicker();
    }

    void toggleSelectedSetting() {
        if (!AppNavigationCustomButtonSettings.isEnabled(activity)) {
            return;
        }
        Target target = AppNavigationCustomButtonSettings.getTarget(activity);
        boolean enabled = !AppNavigationCustomButtonTargetState.isEnabled(activity, target);
        if (target != Target.SURROUNDING_STREETS || streetSettingReady(enabled)) {
            applySelectedSetting(target, enabled);
        }
    }

    private boolean streetSettingReady(boolean enabled) {
        return !streetPicker.request.isPending() && (!enabled || canEnableStreetSetting());
    }

    private boolean canEnableStreetSetting() {
        if (!isBRouterInstalled()) {
            denySurroundingStreetsBRouterMissing();
            return false;
        }
        if (AndroidLegacyExternalStorageAccess.shouldRequestReadPermission(activity)) {
            waitingForSurroundingStreetPermission = true;
            AndroidLegacyExternalStorageAccess.requestReadPermission(activity, REQUEST_SURROUNDING_STREETS_STORAGE);
            return false;
        }
        if (shouldRequestSurroundingStreetSegmentsTree()) {
            startSurroundingStreetSegmentsTreePicker();
            return false;
        }
        return true;
    }

    boolean onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_SURROUNDING_STREETS_STORAGE || !waitingForSurroundingStreetPermission) {
            return false;
        }
        waitingForSurroundingStreetPermission = false;
        if (AndroidLegacyExternalStorageAccess.isReadPermissionGranted(grantResults)) {
            if (!isBRouterInstalled()) {
                denySurroundingStreetsBRouterMissing();
                return true;
            }
            applySelectedSetting(Target.SURROUNDING_STREETS, true);
            return true;
        }
        AppNavigationCustomButtonTargetState.setEnabled(activity, Target.SURROUNDING_STREETS, false);
        host.refreshNavigationUiSettings();
        Toast.makeText(
                activity,
                R.string.msg_compass_surrounding_streets_storage_permission_required,
                Toast.LENGTH_SHORT
        ).show();
        return true;
    }

    boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        return streetPicker.accept(requestCode, resultCode, data);
    }

    private void onStreetFolderSaved(boolean saved) {
        if (!isBRouterInstalled()) {
            denySurroundingStreetsBRouterMissing();
        } else if (!saved || !segmentsRepository.hasPersistedSegmentsTreeAccess(activity)) {
            denySurroundingStreetStorage();
        } else {
            applySelectedSetting(Target.SURROUNDING_STREETS, true);
        }
    }

    private boolean shouldRequestSurroundingStreetSegmentsTree() {
        return !AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()
                && !segmentsRepository.hasPersistedSegmentsTreeAccess(activity);
    }

    private void startSurroundingStreetSegmentsTreePicker() {
        AndroidBRouterSegmentsTreeAccessPrompt.show(
                activity,
                REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE,
                TAG,
                () -> streetPicker.request.begin(REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE),
                this::denySurroundingStreetStorage,
                () -> onStreetFolderSaved(true)
        );
    }

    void restoreState(@Nullable Bundle state) {
        streetPicker.restore(state);
    }

    void saveState(Bundle state) {
        streetPicker.request.save(state);
    }

    void dispose() {
        streetPicker.request.stopWatching();
    }

    private void denySurroundingStreetsBRouterMissing() {
        AppNavigationCustomButtonTargetState.setEnabled(activity, Target.SURROUNDING_STREETS, false);
        host.refreshNavigationUiSettings();
        Toast.makeText(
                activity,
                R.string.msg_surrounding_streets_brouter_required,
                Toast.LENGTH_SHORT
        ).show();
    }

    private boolean isBRouterInstalled() {
        return profilesRepository.isBRouterInstalled(activity);
    }

    private void denySurroundingStreetStorage() {
        AppNavigationCustomButtonTargetState.setEnabled(activity, Target.SURROUNDING_STREETS, false);
        host.refreshNavigationUiSettings();
        Toast.makeText(
                activity,
                R.string.msg_compass_surrounding_streets_storage_permission_required,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void applySelectedSetting(@NonNull Target target, boolean enabled) {
        AppNavigationCustomButtonTargetState.setEnabled(activity, target, enabled);
        applyLiveEffect(target);
        host.refreshNavigationUiSettings();
    }

    private void applyLiveEffect(@NonNull Target target) {
        if (target == Target.DYNAMIC_GPS_INTERVAL) {
            refreshLocationUpdates();
            return;
        }
        if (target == Target.LIGHT_THEME) {
            uiScheduler.removeCallbacks(recreateForThemeChange);
            uiScheduler.postDelayed(recreateForThemeChange, THEME_RECREATE_DELAY_MS);
        }
    }

    private void refreshLocationUpdates() {
        NavigationServiceBinder binder = host.currentBinder();
        if (binder != null) {
            binder.refreshLocationUpdateSettings();
        }
    }

    private void recreateForThemeChangeIfActive() {
        if (!activity.isFinishing() && !activity.isDestroyed()) {
            activity.recreate();
        }
    }

    private final class StreetPicker {
        final AndroidBRouterPickerRequest request = new AndroidBRouterPickerRequest(activity, "navigation_custom_street_picker");

        void restore(@Nullable Bundle state) {
            Completion completion = request.restore(state);
            if (completion != null) {
                onStreetFolderSaved(completion == Completion.ADVANCE);
            }
            request.watch(result -> onStreetFolderSaved(result == Completion.ADVANCE));
        }

        boolean accept(int code, int resultCode, @Nullable Intent data) {
            if (code != REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE) {
                return false;
            }
            if (request.matches(code)) {
                if (resultCode == Activity.RESULT_OK) {
                    request.beginValidation();
                    AndroidBRouterFolderAccess.saveSelection(activity, data, this::finish);
                } else {
                    finish(false);
                }
            }
            return true;
        }

        private void finish(boolean saved) {
            request.complete(saved ? Completion.ADVANCE : Completion.CANCELED);
            if (!activity.isDestroyed()) {
                onStreetFolderSaved(saved);
            }
        }
    }

}
