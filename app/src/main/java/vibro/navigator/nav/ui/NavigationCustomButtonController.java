package vibro.navigator.nav.ui;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;
import vibro.navigator.settings.AppNavigationCustomButtonTargetState;

final class NavigationCustomButtonController {
    static final int REQUEST_SURROUNDING_STREETS_STORAGE = 3002;

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
    private final Runnable recreateForThemeChange = this::recreateForThemeChangeIfActive;
    private boolean waitingForSurroundingStreetPermission;

    NavigationCustomButtonController(
            @NonNull Activity activity,
            @NonNull TaskScheduler uiScheduler,
            @NonNull Host host
    ) {
        this.activity = activity;
        this.uiScheduler = uiScheduler;
        this.host = host;
    }

    void toggleSelectedSetting() {
        if (!AppNavigationCustomButtonSettings.isEnabled(activity)) {
            return;
        }
        Target target = AppNavigationCustomButtonSettings.getTarget(activity);
        boolean enabled = !AppNavigationCustomButtonTargetState.isEnabled(activity, target);
        if (shouldRequestSurroundingStreetPermission(target, enabled)) {
            waitingForSurroundingStreetPermission = true;
            AndroidLegacyExternalStorageAccess.requestReadPermission(activity, REQUEST_SURROUNDING_STREETS_STORAGE);
            return;
        }
        applySelectedSetting(target, enabled);
    }

    boolean onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_SURROUNDING_STREETS_STORAGE || !waitingForSurroundingStreetPermission) {
            return false;
        }
        waitingForSurroundingStreetPermission = false;
        if (AndroidLegacyExternalStorageAccess.isReadPermissionGranted(grantResults)) {
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

    private boolean shouldRequestSurroundingStreetPermission(@NonNull Target target, boolean enabled) {
        return target == Target.SURROUNDING_STREETS
                && enabled
                && AndroidLegacyExternalStorageAccess.shouldRequestReadPermission(activity);
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
}
