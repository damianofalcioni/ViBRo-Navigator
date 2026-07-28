package vibro.navigator.auto;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;
import vibro.navigator.settings.AppNavigationCustomButtonTargetState;

final class ViBRoAutoCustomButtonController {
    interface Host {
        @Nullable
        NavigationServiceBinder currentBinder();

        void openPhoneSettings();

        void refreshSurfaceTheme();

        void refreshSurface();

        void showToast(int messageResId);
    }

    @NonNull
    private final Context context;
    @NonNull
    private final Host host;

    ViBRoAutoCustomButtonController(@NonNull Context context, @NonNull Host host) {
        this.context = context;
        this.host = host;
    }

    void toggleSelectedSetting() {
        if (!AppNavigationCustomButtonSettings.isEnabled(context)) {
            return;
        }
        Target target = AppNavigationCustomButtonSettings.getTarget(context);
        boolean enabled = !AppNavigationCustomButtonTargetState.isEnabled(context, target);
        if (needsPhonePermission(target, enabled)) {
            host.showToast(R.string.msg_compass_surrounding_streets_storage_permission_required);
            host.openPhoneSettings();
            return;
        }
        AppNavigationCustomButtonTargetState.setEnabled(context, target, enabled);
        if (!applyLiveEffect(target)) {
            host.refreshSurface();
        }
    }

    private boolean needsPhonePermission(@NonNull Target target, boolean enabled) {
        return target == Target.SURROUNDING_STREETS
                && enabled
                && AndroidLegacyExternalStorageAccess.shouldRequestReadPermission(context);
    }

    private boolean applyLiveEffect(@NonNull Target target) {
        if (target == Target.DYNAMIC_GPS_INTERVAL) {
            refreshLocationUpdates();
            return false;
        }
        if (target == Target.LIGHT_THEME) {
            host.refreshSurfaceTheme();
            return true;
        }
        return false;
    }

    private void refreshLocationUpdates() {
        NavigationServiceBinder binder = host.currentBinder();
        if (binder != null) {
            binder.refreshLocationUpdateSettings();
        }
    }
}
