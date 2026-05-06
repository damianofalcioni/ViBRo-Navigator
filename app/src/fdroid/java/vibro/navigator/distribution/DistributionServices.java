package vibro.navigator.distribution;

import android.content.Context;
import android.location.LocationListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.location.FusedLocationUpdateClient;
import vibro.navigator.poi.search.PoiSearchClient;

public final class DistributionServices {
    private DistributionServices() {
    }

    public static boolean supportsFusedLocation() {
        return false;
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

    @Nullable
    public static PoiSearchClient createGooglePoiSearchClient() {
        return null;
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
        public void removeUpdates() {
        }

        @NonNull
        @Override
        public String describeAvailability() {
            return "fusedLocation=false";
        }
    }
}
