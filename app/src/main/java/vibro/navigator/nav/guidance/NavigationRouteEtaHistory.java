package vibro.navigator.nav.guidance;

import java.util.ArrayDeque;
import java.util.Objects;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationMotionState;

/** Live route velocity uses observation time and independently confirmed motion. */
public final class NavigationRouteEtaHistory {
    private static final long MAX_SAMPLE_AGE_MS = 65_000L;
    private final ArrayDeque<RouteProgressSample> samples = new ArrayDeque<>();
    private String provider;

    public void reset() {
        samples.clear();
        provider = null;
    }

    public void recordTrustedProgress(NavigationLocation location, double alongMeters, float accuracy,
            boolean confirmedMoving, long nowMs, boolean trustworthyProgress) {
        if (trustworthyProgress) {
            record(location, alongMeters, accuracy, confirmedMoving, nowMs);
        } else {
            reset();
        }
    }

    public RouteMotionEstimate record(NavigationLocation location, double alongMeters, float accuracy,
            boolean confirmedMoving, long nowMs) {
        if (!isFreshMoving(location, confirmedMoving, nowMs)) {
            reset();
            return RouteMotionEstimate.unavailable();
        }
        if (!Objects.equals(provider, location.getProvider())) {
            reset();
            provider = location.getProvider();
        }
        long measuredMs = location.getElapsedRealtimeOrTimeMs();
        if (reversesLatestProgress(measuredMs, alongMeters)) {
            samples.clear();
        }
        RouteMotionEstimate estimate = resolve(location, alongMeters, accuracy, confirmedMoving, nowMs);
        if (samples.isEmpty() || measuredMs > samples.peekLast().timeMs) {
            samples.addLast(new RouteProgressSample(alongMeters, measuredMs));
        }
        return estimate;
    }

    /** Display reads may expire live velocity, but cannot erase normal sparse-cadence anchors. */
    public RouteMotionEstimate resolve(NavigationLocation location, double alongMeters, float accuracy,
            boolean confirmedMoving, long nowMs) {
        if (!isFreshMoving(location, confirmedMoving, nowMs) || !Objects.equals(provider, location.getProvider())
                || reversesLatestProgress(location.getElapsedRealtimeOrTimeMs(), alongMeters)) {
            return RouteMotionEstimate.unavailable();
        }
        prune(location.getElapsedRealtimeOrTimeMs());
        return RouteEtaMotionEstimator.resolve(samples, location.getElapsedRealtimeOrTimeMs(), alongMeters, accuracy, false);
    }

    private boolean reversesLatestProgress(long measuredMs, double alongMeters) {
        return !samples.isEmpty() && measuredMs > samples.peekLast().timeMs
                && alongMeters < samples.peekLast().alongTrackMeters;
    }

    private void prune(long measuredMs) {
        while (!samples.isEmpty() && measuredMs - samples.peekFirst().timeMs > MAX_SAMPLE_AGE_MS) {
            samples.removeFirst();
        }
    }

    private static boolean isFreshMoving(NavigationLocation location, boolean confirmedMoving, long nowMs) {
        if (location == null || !confirmedMoving) {
            return false;
        }
        long measuredMs = location.getElapsedRealtimeOrTimeMs();
        return measuredMs <= nowMs && nowMs - measuredMs <= NavigationMotionState.MAX_EVIDENCE_AGE_MS;
    }
}
