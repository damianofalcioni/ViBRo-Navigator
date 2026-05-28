package vibro.navigator.nav.startup;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.location.NavigationLocationFix;

public final class NavigationStartupLocationSelector {

    private static final long MAX_AGE_MS = 15_000L;
    private static final float MAX_ACCURACY_METERS = 50f;
    private static final float MAX_ROUTE_START_ACCURACY_METERS = 25f;

    private NavigationStartupLocationSelector() {
    }

    @Nullable
    public static NavigationLocation selectBest(@Nullable NavigationLocation gps, @Nullable NavigationLocation network, long nowMs) {
        NavigationLocationFix gpsFix = NavigationLocationFix.from(gps);
        NavigationLocationFix networkFix = NavigationLocationFix.from(network);
        CandidateSource source = selectBestSource(gpsFix, networkFix, nowMs);
        if (source == CandidateSource.GPS) {
            return new NavigationLocation(gps);
        }
        if (source == CandidateSource.NETWORK) {
            return new NavigationLocation(network);
        }
        return null;
    }

    public static boolean isUsable(@Nullable NavigationLocation location, long nowMs) {
        return isUsableFix(NavigationLocationFix.from(location), nowMs);
    }

    public static boolean isUsableForRouteStart(@Nullable NavigationLocation location, long nowMs) {
        return isUsableForRouteStartFix(NavigationLocationFix.from(location), nowMs);
    }

    @Nullable
    static NavigationLocationFix selectBestFix(
            @Nullable NavigationLocationFix gps,
            @Nullable NavigationLocationFix network,
            long nowMs
    ) {
        CandidateSource source = selectBestSource(gps, network, nowMs);
        if (source == CandidateSource.GPS) {
            return gps;
        }
        if (source == CandidateSource.NETWORK) {
            return network;
        }
        return null;
    }

    @Nullable
    private static CandidateSource selectBestSource(
            @Nullable NavigationLocationFix gps,
            @Nullable NavigationLocationFix network,
            long nowMs
    ) {
        NavigationLocationFix best = null;
        CandidateSource source = null;
        if (isUsableFix(gps, nowMs)) {
            best = gps;
            source = CandidateSource.GPS;
        }
        if (isUsableFix(network, nowMs) && isBetterThan(network, best)) {
            source = CandidateSource.NETWORK;
        }
        return source;
    }

    static boolean isUsableFix(@Nullable NavigationLocationFix fix, long nowMs) {
        if (fix == null) {
            return false;
        }
        long ageMs = Math.max(0L, nowMs - fix.timeMs);
        return ageMs <= MAX_AGE_MS
                && fix.accuracyMeters <= MAX_ACCURACY_METERS;
    }

    static boolean isUsableForRouteStartFix(@Nullable NavigationLocationFix fix, long nowMs) {
        if (fix == null) {
            return false;
        }
        long ageMs = Math.max(0L, nowMs - fix.timeMs);
        return ageMs <= MAX_AGE_MS
                && fix.accuracyMeters <= MAX_ROUTE_START_ACCURACY_METERS;
    }

    private static boolean isBetterThan(
            @NonNull NavigationLocationFix candidate,
            @Nullable NavigationLocationFix currentBest
    ) {
        return currentBest == null
                || candidate.timeMs > currentBest.timeMs
                || (candidate.timeMs == currentBest.timeMs
                && candidate.accuracyMeters < currentBest.accuracyMeters);
    }

    private enum CandidateSource {
        GPS,
        NETWORK
    }
}
