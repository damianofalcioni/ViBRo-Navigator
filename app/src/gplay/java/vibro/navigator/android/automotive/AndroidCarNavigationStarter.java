package vibro.navigator.android.automotive;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.Collections;

import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.android.location.AndroidLocationDiagnostics;
import vibro.navigator.android.location.AndroidLocationPermissions;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.geo.LatLon;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.poi.Poi;
import vibro.navigator.settings.AppAndroidAutoSettings;
import vibro.navigator.settings.AppMainUiSettings;

public final class AndroidCarNavigationStarter {
    private AndroidCarNavigationStarter() { }

    /** Run profile/package/storage inspection on a worker, before starting the foreground service. */
    @Nullable
    public static NavigationRequest prepare(Context context, Poi destination) {
        if (!AppMainUiSettings.isWelcomeCompleted(context)) {
            return null;
        }
        BRouterProfilesRepository profiles = AndroidBRouterProfilesRepositoryFactory.create();
        String key = profiles.getSelectedProfileKey(context);
        if (!hasSelectedProfile(key) || !profiles.isBRouterInstalled(context)) {
            return null;
        }
        boolean custom = "__custom__".equals(key);
        String profile = custom ? profiles.getCustomProfileName(context) : key;
        if (!hasUsableProfile(context, profiles, profile, custom)) {
            return null;
        }
        return new NavigationRequest(NavigationRoutingMode.BROUTER, profile, custom,
                profiles.getProfileParameterOverridesExtraParams(context, profile), destination.displayLabel(),
                new LatLon(destination.lat, destination.lon), Collections.emptyList());
    }

    private static boolean hasSelectedProfile(@Nullable String key) {
        return key != null && !"__unselected__".equals(key);
    }

    private static boolean hasUsableProfile(Context context, BRouterProfilesRepository profiles,
            @Nullable String profile, boolean custom) {
        return profile != null && (!custom || profiles.getCustomProfileUri(context) != null);
    }

    public static boolean canStart(Context context) {
        return AppAndroidAutoSettings.isIntegrationEnabled(context)
                && AndroidLocationPermissions.hasAnyLocationPermission(context)
                && AndroidLocationDiagnostics.isAnyProviderEnabled(context)
                && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static boolean start(Context context, NavigationRequest request) {
        if (!canStart(context)) {
            return false;
        }
        Intent intent = new Intent(context, NavigationService.class).setAction(NavigationService.ACTION_START);
        AndroidNavigationRequestIntentContract.putInto(intent, request);
        try {
            ContextCompat.startForegroundService(context, intent);
            return true;
        } catch (IllegalStateException | SecurityException e) {
            AppLogger.w("CarNavigation", "Unable to start car navigation; phone setup required", e);
            return false;
        }
    }
}
