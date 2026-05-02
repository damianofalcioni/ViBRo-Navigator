package vibro.navigator.nav.location;

import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class LiveLocationCoordinator {

    private static final long LOCATION_STALE_MS = 15_000L;
    private static final long LOCATION_FRESHNESS_BIAS_MS = 8_000L;
    private static final long LOCATION_TIME_TOLERANCE_MS = 1_000L;
    private static final float LOCATION_ACCURACY_BIAS_METERS = 15f;
    private static final float LOCATION_ACCURACY_IMPROVEMENT_METERS = 5f;

    @Nullable
    private Location latestGpsLocation;
    @Nullable
    private Location latestNetworkLocation;
    @Nullable
    private Location lastDispatchedRawLocation;

    public void reset() {
        latestGpsLocation = null;
        latestNetworkLocation = null;
        lastDispatchedRawLocation = null;
    }

    public void remember(@NonNull Location location) {
        Location copy = new Location(location);
        if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
            latestGpsLocation = copy;
        } else if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider())) {
            latestNetworkLocation = copy;
        }
    }

    public void clearProvider(@NonNull String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            latestGpsLocation = null;
        } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
            latestNetworkLocation = null;
        }
    }

    @Nullable
    public Location selectBestLiveLocation() {
        Location gps = isRecentLocation(latestGpsLocation) ? latestGpsLocation : null;
        Location network = isRecentLocation(latestNetworkLocation) ? latestNetworkLocation : null;
        return copyOf(resolveBestLiveLocation(gps, network));
    }

    @Nullable
    private Location resolveBestLiveLocation(@Nullable Location gps, @Nullable Location network) {
        if (gps == null) {
            return network;
        }
        if (network == null) {
            return gps;
        }
        long gpsAgeMs = ageMs(gps);
        long networkAgeMs = ageMs(network);
        float gpsAccuracy = accuracyMeters(gps);
        float networkAccuracy = accuracyMeters(network);

        if (isGpsPreferred(gpsAgeMs, networkAgeMs, gpsAccuracy, networkAccuracy)) {
            return new Location(gps);
        }
        if (isNetworkClearlyBetter(gpsAgeMs, networkAgeMs, gpsAccuracy, networkAccuracy)) {
            return new Location(network);
        }
        if (isFreshnessDecisive(gpsAgeMs, networkAgeMs)) {
            return gpsAgeMs < networkAgeMs ? new Location(gps) : new Location(network);
        }
        return new Location(gps);
    }

    private static boolean isGpsPreferred(
            long gpsAgeMs,
            long networkAgeMs,
            float gpsAccuracy,
            float networkAccuracy
    ) {
        return gpsAccuracy <= networkAccuracy + LOCATION_ACCURACY_IMPROVEMENT_METERS
                && gpsAgeMs <= networkAgeMs + LOCATION_TIME_TOLERANCE_MS;
    }

    private static boolean isNetworkClearlyBetter(
            long gpsAgeMs,
            long networkAgeMs,
            float gpsAccuracy,
            float networkAccuracy
    ) {
        return networkAccuracy + LOCATION_ACCURACY_BIAS_METERS < gpsAccuracy
                && networkAgeMs <= gpsAgeMs + LOCATION_TIME_TOLERANCE_MS;
    }

    private static boolean isFreshnessDecisive(long gpsAgeMs, long networkAgeMs) {
        return Math.abs(gpsAgeMs - networkAgeMs) >= LOCATION_FRESHNESS_BIAS_MS;
    }

    @Nullable
    private static Location copyOf(@Nullable Location location) {
        return location == null ? null : new Location(location);
    }

    public boolean shouldDispatch(@NonNull Location candidate) {
        if (lastDispatchedRawLocation == null) {
            return true;
        }
        long candidateTime = candidate.getTime();
        long lastTime = lastDispatchedRawLocation.getTime();
        if (candidateTime > lastTime + LOCATION_TIME_TOLERANCE_MS) {
            return true;
        }
        if (candidateTime + LOCATION_TIME_TOLERANCE_MS < lastTime) {
            return false;
        }

        float candidateAccuracy = accuracyMeters(candidate);
        float lastAccuracy = accuracyMeters(lastDispatchedRawLocation);
        if (candidateAccuracy + LOCATION_ACCURACY_IMPROVEMENT_METERS < lastAccuracy) {
            return true;
        }
        if (sameFix(candidate, lastDispatchedRawLocation)) {
            return false;
        }
        return candidateAccuracy <= lastAccuracy + LOCATION_ACCURACY_BIAS_METERS;
    }

    public void markDispatched(@NonNull Location location) {
        lastDispatchedRawLocation = new Location(location);
    }

    private boolean isRecentLocation(@Nullable Location location) {
        return location != null && ageMs(location) <= LOCATION_STALE_MS;
    }

    private long ageMs(@NonNull Location location) {
        return Math.max(0L, System.currentTimeMillis() - location.getTime());
    }

    private float accuracyMeters(@NonNull Location location) {
        return location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }

    private boolean sameFix(@NonNull Location first, @NonNull Location second) {
        return first.getTime() == second.getTime()
                && safeProvider(first).equals(safeProvider(second))
                && Double.compare(first.getLatitude(), second.getLatitude()) == 0
                && Double.compare(first.getLongitude(), second.getLongitude()) == 0;
    }

    @NonNull
    private static String safeProvider(@NonNull Location location) {
        String provider = location.getProvider();
        return provider == null ? "unknown" : provider;
    }
}
