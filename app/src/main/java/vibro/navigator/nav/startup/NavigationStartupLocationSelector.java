package vibro.navigator.nav.startup;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationStartupLocationSelector {

    private static final long MAX_AGE_MS = 15_000L;
    private static final float MAX_ACCURACY_METERS = 50f;
    private static final float MAX_ROUTE_START_ACCURACY_METERS = 25f;

    private NavigationStartupLocationSelector() {
    }

    @Nullable
    public static Location selectBest(@Nullable Location gps, @Nullable Location network, long nowMs) {
        Fix gpsFix = toFix(gps);
        Fix networkFix = toFix(network);
        CandidateSource source = selectBestSource(gpsFix, networkFix, nowMs);
        if (source == CandidateSource.GPS) {
            return new Location(gps);
        }
        if (source == CandidateSource.NETWORK) {
            return new Location(network);
        }
        return null;
    }

    public static boolean isUsable(@Nullable Location location, long nowMs) {
        return isUsableFix(toFix(location), nowMs);
    }

    public static boolean isUsableForRouteStart(@Nullable Location location, long nowMs) {
        return isUsableForRouteStartFix(toFix(location), nowMs);
    }

    @Nullable
    static Fix selectBestFix(@Nullable Fix gps, @Nullable Fix network, long nowMs) {
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
    private static CandidateSource selectBestSource(@Nullable Fix gps, @Nullable Fix network, long nowMs) {
        Fix best = null;
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

    static boolean isUsableFix(@Nullable Fix fix, long nowMs) {
        if (fix == null) {
            return false;
        }
        long ageMs = Math.max(0L, nowMs - fix.timeMs);
        return ageMs <= MAX_AGE_MS
                && fix.accuracyMeters <= MAX_ACCURACY_METERS;
    }

    static boolean isUsableForRouteStartFix(@Nullable Fix fix, long nowMs) {
        if (fix == null) {
            return false;
        }
        long ageMs = Math.max(0L, nowMs - fix.timeMs);
        return ageMs <= MAX_AGE_MS
                && fix.accuracyMeters <= MAX_ROUTE_START_ACCURACY_METERS;
    }

    private static boolean isBetterThan(@NonNull Fix candidate, @Nullable Fix currentBest) {
        return currentBest == null
                || candidate.timeMs > currentBest.timeMs
                || (candidate.timeMs == currentBest.timeMs
                && candidate.accuracyMeters < currentBest.accuracyMeters);
    }

    @Nullable
    private static Fix toFix(@Nullable Location location) {
        if (location == null) {
            return null;
        }
        float accuracyMeters = location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
        return new Fix(location.getProvider(), location.getTime(), accuracyMeters);
    }

    static final class Fix {
        @Nullable
        final String provider;
        final long timeMs;
        final float accuracyMeters;

        Fix(@Nullable String provider, long timeMs, float accuracyMeters) {
            this.provider = provider;
            this.timeMs = timeMs;
            this.accuracyMeters = accuracyMeters;
        }
    }

    private enum CandidateSource {
        GPS,
        NETWORK
    }
}
