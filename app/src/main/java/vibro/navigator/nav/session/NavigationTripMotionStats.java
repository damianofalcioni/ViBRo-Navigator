package vibro.navigator.nav.session;

import java.util.Objects;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationMotionState;

/** Motion totals retain observation expiry separately from explicit hardware-sleep credit. */
final class NavigationTripMotionStats {
    private NavigationLocation lastLocation;
    private long lastReceiptMs = -1;
    private long expiresAtMs = -1;
    private boolean moving;
    private boolean stationary;
    private double distanceMeters;
    private long movingMs;
    private long stationaryMs;

    void reset() {
        lastLocation = null;
        lastReceiptMs = -1;
        expiresAtMs = -1;
        moving = false;
        stationary = false;
        distanceMeters = 0;
        movingMs = 0;
        stationaryMs = 0;
    }

    void record(NavigationLocation location, long nowMs, boolean stopped, boolean newSegment,
            long sleepEndedMs, boolean known) {
        long sleepMs = sleepDurationMs(sleepEndedMs, nowMs);
        stationaryMs += sleepMs;
        long measurementMs = location.getElapsedRealtimeMs() >= 0 ? location.getElapsedRealtimeMs() : nowMs;
        boolean freshKnown = known && isFresh(measurementMs, nowMs);
        settleTime(stopped, newSegment, nowMs, sleepMs, freshKnown);
        if (freshKnown) {
            creditDistance(location, stopped, newSegment, nowMs, sleepMs);
        }
        lastLocation = freshKnown ? new NavigationLocation(location) : null;
        lastReceiptMs = nowMs;
        expiresAtMs = measurementMs + NavigationMotionState.MAX_EVIDENCE_AGE_MS;
        moving = freshKnown && !stopped;
        stationary = freshKnown && stopped;
    }

    private void creditDistance(NavigationLocation location, boolean stopped, boolean newSegment,
            long nowMs, long sleepMs) {
        if (!stopped && !newSegment && sleepMs == 0 && nowMs <= expiresAtMs && lastLocation != null) {
            distanceMeters += connectingDistanceMeters(location);
        }
    }

    private void settleTime(boolean stopped, boolean newSegment, long nowMs, long sleepMs, boolean freshKnown) {
        boolean continuousEvidence = freshKnown && !newSegment;
        long intervalMs = intervalMs(nowMs, sleepMs, continuousEvidence);
        // Unsupported new evidence cannot erase or relabel the earlier supported prefix.
        boolean stoppedForInterval = continuousEvidence && (nowMs <= expiresAtMs || sleepMs > 0)
                ? stopped : stationary;
        if (stoppedForInterval) {
            stationaryMs += intervalMs;
        } else {
            movingMs += intervalMs;
        }
    }

    private double connectingDistanceMeters(NavigationLocation location) {
        if (!Objects.equals(lastLocation.getProvider(), location.getProvider())) {
            return 0;
        }
        double distance = lastLocation.distanceTo(location);
        return Double.isFinite(distance) ? Math.max(0, distance) : 0;
    }

    private long intervalMs(long nowMs, long sleepMs, boolean continuousEvidence) {
        if (!(moving || stationary)) {
            return 0;
        }
        long endMs = continuousEvidence && sleepMs > 0 ? nowMs : Math.min(nowMs, expiresAtMs);
        return Math.max(0L, endMs - lastReceiptMs - sleepMs);
    }

    private long sleepDurationMs(long endedMs, long nowMs) {
        return lastReceiptMs < 0 ? 0 : Math.max(0L, Math.min(endedMs, nowMs) - lastReceiptMs);
    }

    private static boolean isFresh(long measuredMs, long nowMs) {
        return measuredMs <= nowMs && nowMs - measuredMs <= NavigationMotionState.MAX_EVIDENCE_AGE_MS;
    }

    double distanceMeters() { return distanceMeters; }
    long movingMs() { return movingMs; }
    long stationaryMs() { return stationaryMs; }
    boolean moving() { return moving; }
    boolean stationary() { return stationary; }
    long expiresAtMs() { return expiresAtMs; }
}
