package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

/** Chronological and cadence gates share their decisions with diagnostic logging. */
final class LiveLocationDispatchPolicy {
    private static final long LOCATION_TIME_TOLERANCE_MS = 1_000L;
    private static final float LOCATION_ACCURACY_BIAS_METERS = 15f;
    private static final float LOCATION_ACCURACY_IMPROVEMENT_METERS = 5f;
    private static final long CROSS_PROVIDER_INTERVAL_TOLERANCE_MS = 500L;

    private LiveLocationDispatchPolicy() { }

    static boolean shouldDispatch(
            @Nullable NavigationLocationFix lastDispatched,
            @NonNull NavigationLocationFix candidate
    ) {
        return rejectionReason(lastDispatched, candidate) == null;
    }

    @Nullable
    private static String rejectionReason(@Nullable NavigationLocationFix lastDispatched,
            @NonNull NavigationLocationFix candidate) {
        if (!LiveLocationPolicy.isValidFix(candidate)) {
            return "invalid_fix";
        }
        if (lastDispatched == null) {
            return null;
        }
        long candidateTime = candidate.timeMs;
        long lastTime = lastDispatched.timeMs;
        if (candidateTime > lastTime + LOCATION_TIME_TOLERANCE_MS) {
            return null;
        }
        if (candidateTime < lastTime) {
            return "older_than_last_accepted";
        }

        return accuracyRejectionReason(lastDispatched, candidate);
    }

    @Nullable
    private static String accuracyRejectionReason(NavigationLocationFix lastDispatched, NavigationLocationFix candidate) {
        float candidateAccuracy = candidate.accuracyMeters;
        float lastAccuracy = lastDispatched.accuracyMeters;
        if (candidateAccuracy + LOCATION_ACCURACY_IMPROVEMENT_METERS < lastAccuracy) {
            return null;
        }
        if (candidate.timeMs == lastDispatched.timeMs) {
            return "same_measurement_time_without_accuracy_improvement";
        }
        return candidateAccuracy <= lastAccuracy + LOCATION_ACCURACY_BIAS_METERS
                ? null : "accuracy_worse_within_time_tolerance";
    }

    static boolean shouldDispatchForRequestedInterval(
            @Nullable NavigationLocationFix lastDispatched,
            @NonNull NavigationLocationFix candidate,
            long elapsedSinceLastDispatchMs,
            long expectedUpdateIntervalMs
    ) {
        return dispatchRejectionReason(lastDispatched, candidate, elapsedSinceLastDispatchMs,
                expectedUpdateIntervalMs) == null;
    }

    @Nullable
    static String dispatchRejectionReason(@Nullable NavigationLocationFix lastDispatched,
            @NonNull NavigationLocationFix candidate, long elapsedSinceLastDispatchMs,
            long expectedUpdateIntervalMs) {
        String reason = rejectionReason(lastDispatched, candidate);
        if (reason != null) {
            return reason;
        }
        if (lastDispatched == null) {
            return null;
        }
        if (Objects.toString(lastDispatched.provider, "unknown")
                .equals(Objects.toString(candidate.provider, "unknown"))) {
            return null;
        }
        boolean arrivedInsideRequestedInterval = elapsedSinceLastDispatchMs >= 0L
                && elapsedSinceLastDispatchMs + CROSS_PROVIDER_INTERVAL_TOLERANCE_MS
                < expectedUpdateIntervalMs;
        boolean permitted = !arrivedInsideRequestedInterval
                || candidate.accuracyMeters + LOCATION_ACCURACY_IMPROVEMENT_METERS
                < lastDispatched.accuracyMeters;
        return permitted ? null : "cross_provider_requested_interval_not_elapsed";
    }

}
