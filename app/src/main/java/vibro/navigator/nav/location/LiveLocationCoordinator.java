package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class LiveLocationCoordinator {
    public static final String FUSED_PROVIDER = "fused";

    public static final class Selection {
        @NonNull
        public final NavigationLocation location;
        @NonNull
        private final NavigationLocationFix fix;

        private Selection(@NonNull NavigationLocation location, @NonNull NavigationLocationFix fix) {
            this.location = location;
            this.fix = fix;
        }
    }

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
    private long lastDispatchedAtMs = -1L;

    public void reset() {
        latestGpsLocation = null;
        latestNetworkLocation = null;
        latestFusedLocation = null;
        latestGpsFix = null;
        latestNetworkFix = null;
        latestFusedFix = null;
        lastDispatchedRawFix = null;
        lastDispatchedAtMs = -1L;
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
    public Selection selectBestLiveSelection(long nowMs) {
        NavigationLocationFix selectedFix = selectBestLiveFix(nowMs);
        if (selectedFix == null) {
            return null;
        }
        NavigationLocation selectedLocation = copyOf(locationFor(selectedFix));
        return selectedLocation == null ? null : new Selection(selectedLocation, selectedFix);
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

    public boolean shouldDispatch(
            @NonNull NavigationLocation candidate,
            long nowMs,
            long expectedUpdateIntervalMs
    ) {
        return shouldDispatch(NavigationLocationFix.from(candidate), nowMs, expectedUpdateIntervalMs);
    }

    public boolean shouldDispatch(
            @NonNull Selection candidate,
            long nowMs,
            long expectedUpdateIntervalMs
    ) {
        return shouldDispatch(candidate.fix, nowMs, expectedUpdateIntervalMs);
    }

    private boolean shouldDispatch(
            @NonNull NavigationLocationFix candidate,
            long nowMs,
            long expectedUpdateIntervalMs
    ) {
        long elapsedSinceLastDispatchMs = lastDispatchedAtMs < 0L ? -1L : nowMs - lastDispatchedAtMs;
        return LiveLocationPolicy.shouldDispatchForRequestedInterval(
                lastDispatchedRawFix,
                candidate,
                elapsedSinceLastDispatchMs,
                expectedUpdateIntervalMs
        );
    }

    boolean shouldDispatch(@NonNull NavigationLocationFix candidate) {
        return LiveLocationPolicy.shouldDispatch(lastDispatchedRawFix, candidate);
    }

    public void markDispatched(@NonNull NavigationLocation location) {
        lastDispatchedRawFix = NavigationLocationFix.from(location);
    }

    public void markDispatched(@NonNull NavigationLocation location, long nowMs) {
        markDispatched(location);
        lastDispatchedAtMs = nowMs;
    }

    public void markDispatched(@NonNull Selection selection, long nowMs) {
        markDispatched(selection.fix);
        lastDispatchedAtMs = nowMs;
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
