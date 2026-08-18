package vibro.navigator.distribution;

import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;

import vibro.navigator.android.location.GplayFusedLocationClient;
import vibro.navigator.auto.ViBRoCarAppComponent;
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

    public static boolean supportsGooglePlayServicesDiagnostics() {
        return true;
    }

    @NonNull
    public static String googlePlayServicesRuntimeStatus(@NonNull Context context) {
        int status = GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context);
        return googlePlayServicesStatusName(status) + "(" + status + ")";
    }

    @NonNull
    public static String androidAutoIntegrationComponentState(@NonNull Context context) {
        return ViBRoCarAppComponent.state(context);
    }

    public static void configureAndroidAutoIntegration(@NonNull Context context, boolean enabled) {
        ViBRoCarAppComponent.configure(context, enabled);
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

    @Nullable
    public static ComponentName createSpeechRecognitionServiceFallback() {
        return new ComponentName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
        );
    }

    @NonNull
    private static String googlePlayServicesStatusName(int status) {
        switch (status) {
            case ConnectionResult.SUCCESS:
                return "success";
            case ConnectionResult.SERVICE_MISSING:
                return "service_missing";
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                return "service_update_required";
            case ConnectionResult.SERVICE_DISABLED:
                return "service_disabled";
            case ConnectionResult.SERVICE_INVALID:
                return "service_invalid";
            case ConnectionResult.SERVICE_UPDATING:
                return "service_updating";
            default:
                return "unknown";
        }
    }
}
