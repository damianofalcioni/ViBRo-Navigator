package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Deque;
import java.util.Iterator;

final class RouteEtaMotionEstimator {
    private static final long MIN_ETA_SPEED_SAMPLE_AGE_MS = 2_000L;
    private static final double MILLIS_PER_SECOND = 1000.0;
    private static final double MIDPOINT_INTERVAL_MILLIS_PER_SECOND = 2000.0;
    private static final double MIN_ETA_PROGRESS_METERS = 1.0;
    private static final float MIN_TRUSTED_ETA_SPEED_MPS = 0.2f;

    private RouteEtaMotionEstimator() {
    }

    @NonNull
    static RouteMotionEstimate resolve(
            @NonNull Deque<RouteProgressSample> samples,
            long locationTimeMs,
            double alongTrackMeters,
            float accuracyMeters,
            boolean likelyStationary
    ) {
        if (likelyStationary) {
            return RouteMotionEstimate.stationary();
        }
        RouteProgressSample anchor = latestEligibleSample(samples, locationTimeMs, MIN_ETA_SPEED_SAMPLE_AGE_MS);
        if (anchor == null) {
            return RouteMotionEstimate.stationary();
        }
        Float speedMps = resolveTrustedEtaSpeed(anchor, alongTrackMeters, locationTimeMs, accuracyMeters);
        if (speedMps == null) {
            return RouteMotionEstimate.stationary();
        }
        Float accelerationMps2 = resolveEtaAcceleration(samples, anchor, locationTimeMs, accuracyMeters, speedMps);
        return accelerationMps2 == null
                ? RouteMotionEstimate.speedOnly(speedMps)
                : RouteMotionEstimate.withAcceleration(speedMps, accelerationMps2);
    }

    @Nullable
    private static Float resolveTrustedEtaSpeed(
            @NonNull RouteProgressSample anchor,
            double alongTrackMeters,
            long locationTimeMs,
            float accuracyMeters
    ) {
        double alongTrackDeltaMeters = alongTrackMeters - anchor.alongTrackMeters;
        if (alongTrackDeltaMeters < minimumEtaProgressMeters(accuracyMeters)) {
            return null;
        }
        double elapsedSeconds = (locationTimeMs - anchor.timeMs) / MILLIS_PER_SECOND;
        if (elapsedSeconds <= 0.0) {
            return null;
        }
        float speedMps = (float) (alongTrackDeltaMeters / elapsedSeconds);
        return speedMps >= MIN_TRUSTED_ETA_SPEED_MPS ? speedMps : null;
    }

    @Nullable
    private static Float resolveEtaAcceleration(
            @NonNull Deque<RouteProgressSample> samples,
            @NonNull RouteProgressSample currentAnchor,
            long locationTimeMs,
            float accuracyMeters,
            float currentSpeedMps
    ) {
        RouteProgressSample previousAnchor = latestEligibleSample(
                samples,
                currentAnchor.timeMs,
                MIN_ETA_SPEED_SAMPLE_AGE_MS
        );
        if (previousAnchor == null) {
            return null;
        }
        Float previousSpeedMps = resolvePreviousEtaSpeed(previousAnchor, currentAnchor, accuracyMeters);
        if (previousSpeedMps == null) {
            return null;
        }
        double accelerationElapsedSeconds = (locationTimeMs - previousAnchor.timeMs)
                / MIDPOINT_INTERVAL_MILLIS_PER_SECOND;
        if (accelerationElapsedSeconds <= 0.0) {
            return null;
        }
        float accelerationMps2 = (float) ((currentSpeedMps - previousSpeedMps) / accelerationElapsedSeconds);
        return Float.isFinite(accelerationMps2) ? accelerationMps2 : null;
    }

    @Nullable
    private static Float resolvePreviousEtaSpeed(
            @NonNull RouteProgressSample previousAnchor,
            @NonNull RouteProgressSample currentAnchor,
            float accuracyMeters
    ) {
        double alongTrackDeltaMeters = currentAnchor.alongTrackMeters - previousAnchor.alongTrackMeters;
        if (alongTrackDeltaMeters < 0.0) {
            return null;
        }
        if (alongTrackDeltaMeters < minimumEtaProgressMeters(accuracyMeters)) {
            return 0f;
        }
        double elapsedSeconds = (currentAnchor.timeMs - previousAnchor.timeMs) / MILLIS_PER_SECOND;
        if (elapsedSeconds <= 0.0) {
            return null;
        }
        float speedMps = (float) (alongTrackDeltaMeters / elapsedSeconds);
        return speedMps >= MIN_TRUSTED_ETA_SPEED_MPS ? speedMps : 0f;
    }

    @Nullable
    private static RouteProgressSample latestEligibleSample(
            @NonNull Deque<RouteProgressSample> samples,
            long nowMs,
            long minimumAgeMs
    ) {
        Iterator<RouteProgressSample> candidates = samples.descendingIterator();
        while (candidates.hasNext()) {
            RouteProgressSample candidate = candidates.next();
            if (nowMs - candidate.timeMs >= minimumAgeMs) {
                return candidate;
            }
        }
        return null;
    }

    private static double minimumEtaProgressMeters(float accuracyMeters) {
        return Math.max(
                MIN_ETA_PROGRESS_METERS,
                Float.isFinite(accuracyMeters) && accuracyMeters > 0f ? accuracyMeters * 0.25 : 0.0
        );
    }
}
