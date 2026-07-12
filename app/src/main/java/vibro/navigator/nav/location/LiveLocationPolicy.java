package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class LiveLocationPolicy {
    private static final long LOCATION_STALE_MS = 15_000L;
    private static final long LOCATION_FRESHNESS_BIAS_MS = 8_000L;
    private static final long LOCATION_TIME_TOLERANCE_MS = 1_000L;
    private static final float LOCATION_ACCURACY_BIAS_METERS = 15f;
    private static final float LOCATION_ACCURACY_IMPROVEMENT_METERS = 5f;
    private static final long CROSS_PROVIDER_INTERVAL_TOLERANCE_MS = 500L;

    private LiveLocationPolicy() {
    }

    @Nullable
    static NavigationLocationFix selectBestFix(
            @Nullable NavigationLocationFix gps,
            @Nullable NavigationLocationFix network,
            @Nullable NavigationLocationFix fused,
            long nowMs
    ) {
        NavigationLocationFix recentGps = isRecentFix(gps, nowMs) ? gps : null;
        NavigationLocationFix recentNetwork = isRecentFix(network, nowMs) ? network : null;
        NavigationLocationFix recentFused = isRecentFix(fused, nowMs) ? fused : null;
        return resolveBestFix(resolveBestFix(recentGps, recentNetwork, nowMs), recentFused, nowMs);
    }

    static boolean shouldDispatch(
            @Nullable NavigationLocationFix lastDispatched,
            @NonNull NavigationLocationFix candidate
    ) {
        if (lastDispatched == null) {
            return true;
        }
        long candidateTime = candidate.timeMs;
        long lastTime = lastDispatched.timeMs;
        if (candidateTime > lastTime + LOCATION_TIME_TOLERANCE_MS) {
            return true;
        }
        if (candidateTime + LOCATION_TIME_TOLERANCE_MS < lastTime) {
            return false;
        }

        float candidateAccuracy = candidate.accuracyMeters;
        float lastAccuracy = lastDispatched.accuracyMeters;
        if (candidateAccuracy + LOCATION_ACCURACY_IMPROVEMENT_METERS < lastAccuracy) {
            return true;
        }
        if (sameFix(candidate, lastDispatched)) {
            return false;
        }
        return candidateAccuracy <= lastAccuracy + LOCATION_ACCURACY_BIAS_METERS;
    }

    static boolean shouldDispatchForRequestedInterval(
            @Nullable NavigationLocationFix lastDispatched,
            @NonNull NavigationLocationFix candidate,
            long elapsedSinceLastDispatchMs,
            long expectedUpdateIntervalMs
    ) {
        if (lastDispatched == null) {
            return true;
        }
        if (!shouldDispatch(lastDispatched, candidate)) {
            return false;
        }
        if (safeProvider(lastDispatched.provider).equals(safeProvider(candidate.provider))) {
            return true;
        }
        boolean arrivedInsideRequestedInterval = elapsedSinceLastDispatchMs >= 0L
                && elapsedSinceLastDispatchMs + CROSS_PROVIDER_INTERVAL_TOLERANCE_MS
                < expectedUpdateIntervalMs;
        return !arrivedInsideRequestedInterval
                || candidate.accuracyMeters + LOCATION_ACCURACY_IMPROVEMENT_METERS
                < lastDispatched.accuracyMeters;
    }

    static boolean isRecentFix(@Nullable NavigationLocationFix fix, long nowMs) {
        return fix != null && ageMs(fix, nowMs) <= LOCATION_STALE_MS;
    }

    static boolean sameFix(@NonNull NavigationLocationFix first, @NonNull NavigationLocationFix second) {
        return first.timeMs == second.timeMs
                && safeProvider(first.provider).equals(safeProvider(second.provider))
                && Double.compare(first.lat, second.lat) == 0
                && Double.compare(first.lon, second.lon) == 0;
    }

    @Nullable
    private static NavigationLocationFix resolveBestFix(
            @Nullable NavigationLocationFix first,
            @Nullable NavigationLocationFix second,
            long nowMs
    ) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        long firstAgeMs = ageMs(first, nowMs);
        long secondAgeMs = ageMs(second, nowMs);

        if (isFirstPreferred(firstAgeMs, secondAgeMs, first.accuracyMeters, second.accuracyMeters)) {
            return first;
        }
        if (isSecondClearlyBetter(firstAgeMs, secondAgeMs, first.accuracyMeters, second.accuracyMeters)) {
            return second;
        }
        if (isFreshnessDecisive(firstAgeMs, secondAgeMs)) {
            return firstAgeMs < secondAgeMs ? first : second;
        }
        return first;
    }

    private static boolean isFirstPreferred(
            long firstAgeMs,
            long secondAgeMs,
            float firstAccuracy,
            float secondAccuracy
    ) {
        return firstAccuracy <= secondAccuracy + LOCATION_ACCURACY_IMPROVEMENT_METERS
                && firstAgeMs <= secondAgeMs + LOCATION_TIME_TOLERANCE_MS;
    }

    private static boolean isSecondClearlyBetter(
            long firstAgeMs,
            long secondAgeMs,
            float firstAccuracy,
            float secondAccuracy
    ) {
        return secondAccuracy + LOCATION_ACCURACY_BIAS_METERS < firstAccuracy
                && secondAgeMs <= firstAgeMs + LOCATION_TIME_TOLERANCE_MS;
    }

    private static boolean isFreshnessDecisive(long firstAgeMs, long secondAgeMs) {
        return Math.abs(firstAgeMs - secondAgeMs) >= LOCATION_FRESHNESS_BIAS_MS;
    }

    private static long ageMs(@NonNull NavigationLocationFix fix, long nowMs) {
        return Math.max(0L, nowMs - fix.timeMs);
    }

    @NonNull
    private static String safeProvider(@Nullable String provider) {
        return provider == null ? "unknown" : provider;
    }
}
