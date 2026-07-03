package vibro.navigator.distribution;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.location.GplayFusedLocationClient;
import vibro.navigator.auto.ViBRoCarAppService;
import vibro.navigator.nav.location.FusedLocationDiagnosticClient;
import vibro.navigator.nav.location.FusedLocationUpdateClient;
import vibro.navigator.nav.location.NavigationLocationListener;
import vibro.navigator.poi.search.GoogleGeocodeClient;
import vibro.navigator.poi.search.PoiReverseGeocodingClient;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.settings.AppSettings;

public final class DistributionServices {
    private DistributionServices() {
    }

    public static boolean supportsFusedLocation() {
        return true;
    }

    public static boolean supportsUserGooglePoiApiKey() {
        return true;
    }

    public static boolean supportsAndroidAutoIntegration() {
        return true;
    }

    public static void configureAndroidAutoIntegration(@NonNull Context context, boolean enabled) {
        int state = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        ComponentName componentName = new ComponentName(context, ViBRoCarAppService.class);
        context.getPackageManager().setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP
        );
    }

    @NonNull
    public static GooglePoiApiKeyValidationResult validateUserGooglePoiApiKey(@NonNull String apiKey) {
        return GoogleGeocodeClient.validateApiKey(apiKey);
    }

    public static boolean isFusedLocationRuntimeAvailable(@NonNull Context context) {
        return GplayFusedLocationClient.isRuntimeAvailable(context);
    }

    @NonNull
    public static FusedLocationUpdateClient createFusedLocationUpdateClient(
            @NonNull Context context,
            @NonNull NavigationLocationListener listener
    ) {
        return new GplayFusedLocationClient(context, listener);
    }

    @NonNull
    public static FusedLocationDiagnosticClient createFusedLocationDiagnosticClient(@NonNull Context context) {
        return GplayFusedLocationClient.diagnosticClient(context);
    }

    @Nullable
    public static PoiSearchClient createGooglePoiSearchClient(@NonNull Context context) {
        return createGoogleGeocodeClient(context);
    }

    @Nullable
    public static PoiReverseGeocodingClient createGooglePoiReverseGeocodingClient(@NonNull Context context) {
        return createGoogleGeocodeClient(context);
    }

    @Nullable
    private static GoogleGeocodeClient createGoogleGeocodeClient(@NonNull Context context) {
        if (!AppSettings.isGooglePoiSearchEnabled(context)) {
            return null;
        }
        String key = AppSettings.getGooglePoiApiKey(context);
        if (key.trim().isEmpty()) {
            return null;
        }
        return new GoogleGeocodeClient(key);
    }
}
