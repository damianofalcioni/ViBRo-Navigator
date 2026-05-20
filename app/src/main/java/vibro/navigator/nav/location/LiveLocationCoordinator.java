package vibro.navigator.nav.location;

import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class LiveLocationCoordinator {
    public static final String FUSED_PROVIDER = "fused";

    @Nullable
    private Location latestGpsLocation;
    @Nullable
    private Location latestNetworkLocation;
    @Nullable
    private Location latestFusedLocation;
    @Nullable
    private LiveLocationFix latestGpsFix;
    @Nullable
    private LiveLocationFix latestNetworkFix;
    @Nullable
    private LiveLocationFix latestFusedFix;
    @Nullable
    private LiveLocationFix lastDispatchedRawFix;

    public void reset() {
        latestGpsLocation = null;
        latestNetworkLocation = null;
        latestFusedLocation = null;
        latestGpsFix = null;
        latestNetworkFix = null;
        latestFusedFix = null;
        lastDispatchedRawFix = null;
    }

    public void remember(@NonNull Location location) {
        Location copy = new Location(location);
        LiveLocationFix fix = toFix(copy);
        if (LocationManager.GPS_PROVIDER.equals(fix.provider)) {
            latestGpsLocation = copy;
            latestGpsFix = fix;
        } else if (LocationManager.NETWORK_PROVIDER.equals(fix.provider)) {
            latestNetworkLocation = copy;
            latestNetworkFix = fix;
        } else {
            latestFusedLocation = copy;
            latestFusedFix = fix;
        }
    }

    void remember(@NonNull LiveLocationFix fix) {
        if (LocationManager.GPS_PROVIDER.equals(fix.provider)) {
            latestGpsLocation = null;
            latestGpsFix = fix;
        } else if (LocationManager.NETWORK_PROVIDER.equals(fix.provider)) {
            latestNetworkLocation = null;
            latestNetworkFix = fix;
        } else {
            latestFusedLocation = null;
            latestFusedFix = fix;
        }
    }

    public void clearProvider(@NonNull String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            latestGpsLocation = null;
            latestGpsFix = null;
        } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
            latestNetworkLocation = null;
            latestNetworkFix = null;
        } else if (FUSED_PROVIDER.equals(provider)) {
            latestFusedLocation = null;
            latestFusedFix = null;
        }
    }

    @Nullable
    public Location selectBestLiveLocation() {
        long nowMs = System.currentTimeMillis();
        LiveLocationFix selected = selectBestLiveFix(nowMs);
        return copyOf(locationFor(selected));
    }

    @Nullable
    LiveLocationFix selectBestLiveFix(long nowMs) {
        return LiveLocationPolicy.selectBestFix(latestGpsFix, latestNetworkFix, latestFusedFix, nowMs);
    }

    @Nullable
    private static Location copyOf(@Nullable Location location) {
        return location == null ? null : new Location(location);
    }

    public boolean shouldDispatch(@NonNull Location candidate) {
        return shouldDispatch(toFix(candidate));
    }

    boolean shouldDispatch(@NonNull LiveLocationFix candidate) {
        return LiveLocationPolicy.shouldDispatch(lastDispatchedRawFix, candidate);
    }

    public void markDispatched(@NonNull Location location) {
        lastDispatchedRawFix = toFix(location);
    }

    void markDispatched(@NonNull LiveLocationFix fix) {
        lastDispatchedRawFix = fix;
    }

    @Nullable
    private Location locationFor(@Nullable LiveLocationFix selected) {
        if (selected == null) {
            return null;
        }
        Location gps = locationIfSame(selected, latestGpsFix, latestGpsLocation);
        if (gps != null) {
            return gps;
        }
        Location network = locationIfSame(selected, latestNetworkFix, latestNetworkLocation);
        return network != null ? network : locationIfSame(selected, latestFusedFix, latestFusedLocation);
    }

    @Nullable
    private static Location locationIfSame(
            @NonNull LiveLocationFix selected,
            @Nullable LiveLocationFix candidateFix,
            @Nullable Location candidateLocation
    ) {
        return candidateFix != null && LiveLocationPolicy.sameFix(selected, candidateFix)
                ? candidateLocation
                : null;
    }

    @Nullable
    private static LiveLocationFix toFix(@Nullable Location location) {
        if (location == null) {
            return null;
        }
        return new LiveLocationFix(
                location.getProvider(),
                location.getTime(),
                location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE,
                location.getLatitude(),
                location.getLongitude()
        );
    }
}
