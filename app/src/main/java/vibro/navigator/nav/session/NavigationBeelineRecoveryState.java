package vibro.navigator.nav.session;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.BeelineRecoveryTracker;
import vibro.navigator.nav.location.NavigationLocation;

/** The context changes when an approach/leg ends, changes target, is paused, or receives a new route. */
final class NavigationBeelineRecoveryState {
    private static final long MAX_RESULT_FIX_AGE_MS = 10_000L;
    private final BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
    private boolean enabled;
    private LatLon target;
    private long context;
    private long lastObservationMs = -1;

    void reset() {
        onRouteApplied(false);
        tracker.reset();
    }

    void onRouteApplied(boolean allowed) {
        enabled = allowed;
        target = null;
        clearEvidence();
    }

    void clearEvidence() {
        context++;
        lastObservationMs = -1;
        tracker.resetEvidence();
    }

    void setTarget(LatLon value) {
        if (!samePoint(target, value)) {
            target = value;
            clearEvidence();
        }
    }

    boolean shouldRequest(LatLon activeTarget, NavigationLocation fix,
            boolean stationary, long nowMs) {
        setTarget(activeTarget);
        lastObservationMs = nowMs;
        return enabled && target != null
                && tracker.shouldRequest(target, fix, stationary, nowMs);
    }

    long context() {
        return enabled && target != null ? context : -1;
    }

    boolean isCurrent(long capturedContext, long nowMs) {
        return capturedContext >= 0 && capturedContext == context()
                && lastObservationMs >= 0 && nowMs >= lastObservationMs
                && nowMs - lastObservationMs <= MAX_RESULT_FIX_AGE_MS;
    }

    void recordAttempt(NavigationLocation location, long nowMs) {
        tracker.recordAttempt(location, nowMs);
    }

    private static boolean samePoint(LatLon first, LatLon second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.lat == second.lat && first.lon == second.lon;
    }
}
