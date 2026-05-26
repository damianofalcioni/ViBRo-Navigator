package vibro.navigator.distribution;

import android.content.Context;
import android.location.LocationListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.location.FusedLocationDiagnosticClient;
import vibro.navigator.nav.location.FusedLocationUpdateClient;
import vibro.navigator.poi.search.PoiSearchClient;

public final class DistributionServices {
    private DistributionServices() {
    }

    public static boolean supportsFusedLocation() {
        return false;
    }

    public static boolean supportsUserGooglePoiApiKey() {
        return false;
    }

    public static boolean supportsAndroidAutoIntegration() {
        return false;
    }

    public static void configureAndroidAutoIntegration(@NonNull Context context, boolean enabled) {
    }

    @NonNull
    public static GooglePoiApiKeyValidationResult validateUserGooglePoiApiKey(@NonNull String apiKey) {
        return GooglePoiApiKeyValidationResult.ERROR;
    }

    public static boolean isFusedLocationRuntimeAvailable(@NonNull Context context) {
        return false;
    }

    @NonNull
    public static FusedLocationUpdateClient createFusedLocationUpdateClient(
            @NonNull Context context,
            @NonNull LocationListener listener
    ) {
        return new NoOpFusedLocationUpdateClient();
    }

    @NonNull
    public static FusedLocationDiagnosticClient createFusedLocationDiagnosticClient(@NonNull Context context) {
        return new NoOpFusedLocationDiagnosticClient();
    }

    @Nullable
    public static PoiSearchClient createGooglePoiSearchClient(@NonNull Context context) {
        return null;
    }

    private static final class NoOpFusedLocationDiagnosticClient implements FusedLocationDiagnosticClient {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void requestLastKnownLocation(@NonNull Callback callback) {
            callback.onFailure("unavailable");
        }
    }

    private static final class NoOpFusedLocationUpdateClient implements FusedLocationUpdateClient {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public boolean requestUpdates(long minTimeMs, boolean fineGranted, boolean coarseGranted) {
            return false;
        }

        @Override
        public void requestCurrentLocationSeed(boolean fineGranted, boolean coarseGranted) {
        }

        @Override
        public void removeUpdates() {
        }

        @NonNull
        @Override
        public String describeAvailability() {
            return "fusedLocation=false";
        }
    }
}
