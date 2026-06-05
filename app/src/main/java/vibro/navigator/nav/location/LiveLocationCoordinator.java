package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class LiveLocationCoordinator {
    public static final String FUSED_PROVIDER = "fused";

    @Nullable
    private NavigationLocation latestGpsLocation;
    @Nullable
    private NavigationLocation latestNetworkLocation;
    @Nullable
    private NavigationLocation latestFusedLocation;
    @Nullable
    private NavigationLocationFix latestGpsFix;
    @Nullable
    private NavigationLocationFix latestNetworkFix;
    @Nullable
    private NavigationLocationFix latestFusedFix;
    @Nullable
    private NavigationLocationFix lastDispatchedRawFix;

    public void reset() {
        latestGpsLocation = null;
        latestNetworkLocation = null;
        latestFusedLocation = null;
        latestGpsFix = null;
        latestNetworkFix = null;
        latestFusedFix = null;
        lastDispatchedRawFix = null;
    }

    public void remember(@NonNull NavigationLocation location) {
        NavigationLocation copy = new NavigationLocation(location);
        NavigationLocationFix fix = NavigationLocationFix.from(copy);
        if (NavigationLocationProviders.GPS_PROVIDER.equals(fix.provider)) {
            latestGpsLocation = copy;
            latestGpsFix = fix;
        } else if (NavigationLocationProviders.NETWORK_PROVIDER.equals(fix.provider)) {
            latestNetworkLocation = copy;
            latestNetworkFix = fix;
        } else {
            latestFusedLocation = copy;
            latestFusedFix = fix;
        }
    }

    void remember(@NonNull NavigationLocationFix fix) {
        if (NavigationLocationProviders.GPS_PROVIDER.equals(fix.provider)) {
            latestGpsLocation = null;
            latestGpsFix = fix;
        } else if (NavigationLocationProviders.NETWORK_PROVIDER.equals(fix.provider)) {
            latestNetworkLocation = null;
            latestNetworkFix = fix;
        } else {
            latestFusedLocation = null;
            latestFusedFix = fix;
        }
    }

    public void clearProvider(@NonNull String provider) {
        if (NavigationLocationProviders.GPS_PROVIDER.equals(provider)) {
            latestGpsLocation = null;
            latestGpsFix = null;
        } else if (NavigationLocationProviders.NETWORK_PROVIDER.equals(provider)) {
            latestNetworkLocation = null;
            latestNetworkFix = null;
        } else if (FUSED_PROVIDER.equals(provider)) {
            latestFusedLocation = null;
            latestFusedFix = null;
        }
    }

    @Nullable
    public NavigationLocation selectBestLiveLocation(long nowMs) {
        NavigationLocationFix selected = selectBestLiveFix(nowMs);
        return copyOf(locationFor(selected));
    }

    @Nullable
    NavigationLocationFix selectBestLiveFix(long nowMs) {
        return LiveLocationPolicy.selectBestFix(latestGpsFix, latestNetworkFix, latestFusedFix, nowMs);
    }

    @Nullable
    private static NavigationLocation copyOf(@Nullable NavigationLocation location) {
        return location == null ? null : new NavigationLocation(location);
    }

    public boolean shouldDispatch(@NonNull NavigationLocation candidate) {
        return shouldDispatch(NavigationLocationFix.from(candidate));
    }

    boolean shouldDispatch(@NonNull NavigationLocationFix candidate) {
        return LiveLocationPolicy.shouldDispatch(lastDispatchedRawFix, candidate);
    }

    public void markDispatched(@NonNull NavigationLocation location) {
        lastDispatchedRawFix = NavigationLocationFix.from(location);
    }

    void markDispatched(@NonNull NavigationLocationFix fix) {
        lastDispatchedRawFix = fix;
    }

    @Nullable
    private NavigationLocation locationFor(@Nullable NavigationLocationFix selected) {
        if (selected == null) {
            return null;
        }
        NavigationLocation gps = locationIfSame(selected, latestGpsFix, latestGpsLocation);
        if (gps != null) {
            return gps;
        }
        NavigationLocation network = locationIfSame(selected, latestNetworkFix, latestNetworkLocation);
        return network != null ? network : locationIfSame(selected, latestFusedFix, latestFusedLocation);
    }

    @Nullable
    private static NavigationLocation locationIfSame(
            @NonNull NavigationLocationFix selected,
            @Nullable NavigationLocationFix candidateFix,
            @Nullable NavigationLocation candidateLocation
    ) {
        return candidateFix != null && LiveLocationPolicy.sameFix(selected, candidateFix)
                ? candidateLocation
                : null;
    }

}
