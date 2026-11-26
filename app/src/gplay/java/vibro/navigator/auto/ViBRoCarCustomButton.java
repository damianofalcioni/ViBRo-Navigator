package vibro.navigator.auto;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;
import vibro.navigator.settings.AppNavigationCustomButtonTargetResources;
import vibro.navigator.settings.AppNavigationCustomButtonTargetState;

final class ViBRoCarCustomButton {
    record State(Target target, boolean enabled) {
        @Nullable
        static State read(Context context) {
            if (!AppNavigationCustomButtonSettings.isEnabled(context)) {
                return null;
            }
            Target target = AppNavigationCustomButtonSettings.getTarget(context);
            return new State(target, AppNavigationCustomButtonTargetState.isEnabled(context, target));
        }

        int icon() {
            return AppNavigationCustomButtonTargetResources.iconResId(target, enabled);
        }

        boolean isMapControl() {
            return target == Target.FULLSCREEN_ROUTE || target == Target.SURROUNDING_STREETS;
        }
    }

    private final CarContext context;
    private final Runnable refreshLocationSettings;

    ViBRoCarCustomButton(CarContext context, Runnable refreshLocationSettings) {
        this.context = context;
        this.refreshLocationSettings = refreshLocationSettings;
    }

    boolean toggle(State presented) {
        if (!presented.equals(State.read(context))) {
            return false;
        }
        Target target = presented.target();
        boolean enable = !presented.enabled();
        if (target == Target.SURROUNDING_STREETS && enable && !canReadStreetSegments()) {
            String message = AndroidAppLanguage.getString(context, R.string.auto_setup_required);
            CarToast.makeText(context, message, CarToast.LENGTH_LONG).show();
            return false;
        }
        AppNavigationCustomButtonTargetState.setEnabled(context, target, enable);
        if (target == Target.DYNAMIC_GPS_INTERVAL) {
            refreshLocationSettings.run();
        }
        return true;
    }

    private boolean canReadStreetSegments() {
        if (!AndroidBRouterProfilesRepositoryFactory.create().isBRouterInstalled(context)) {
            return false;
        }
        if (AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()) {
            return AndroidLegacyExternalStorageAccess.hasReadPermission(context);
        }
        return AndroidBRouterSegmentsRepositoryFactory.create().hasPersistedSegmentsTreeAccess(context);
    }
}
