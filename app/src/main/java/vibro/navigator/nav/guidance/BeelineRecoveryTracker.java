package vibro.navigator.nav.guidance;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;

/** Earlier recovery attempts are harmless only when repetition is bounded by time and movement. */
public final class BeelineRecoveryTracker {
    private static final double MINIMUM_GROWTH_METERS = 20;
    private static final double MAXIMUM_ACCURACY_METERS = 50;
    private static final long CONFIRMATION_MS = 10_000L;
    private static final long MAXIMUM_SAMPLE_GAP_MS = 10_000L;
    private static final long MINIMUM_PROBE_INTERVAL_MS = 3_000L;
    private static final long MOVING_PROBE_INTERVAL_MS = 30_000L;
    private static final long RETRY_INTERVAL_MS = 5_000L;
    private double closestUpperDistance = Double.POSITIVE_INFINITY;
    private long divergenceSinceMs = -1;
    private long movingSinceMs = -1;
    private NavigationLocation movingStartLocation;
    private long lastSampleMs = -1;
    private long lastAttemptMs = -1;
    private NavigationLocation lastAttemptLocation;

    public void reset() {
        resetEvidence();
        lastAttemptMs = -1;
        lastAttemptLocation = null;
    }

    public void resetEvidence() {
        closestUpperDistance = Double.POSITIVE_INFINITY;
        divergenceSinceMs = -1;
        movingSinceMs = -1;
        movingStartLocation = null;
        lastSampleMs = -1;
    }

    public boolean shouldRequest(LatLon target, NavigationLocation fix, boolean stationary,
            long nowMs) {
        if (!hasUsableAccuracy(fix)) {
            resetEvidence();
            return false;
        }
        boolean diverging = observe(target, fix, stationary, nowMs);
        return (diverging || shouldProbe(fix, nowMs)) && canRetry(fix, nowMs);
    }

    public void recordAttempt(NavigationLocation fix, long nowMs) {
        lastAttemptLocation = new NavigationLocation(fix);
        lastAttemptMs = nowMs;
    }

    public static boolean hasUsableAccuracy(NavigationLocation fix) {
        return fix != null && fix.hasAccuracy() && Float.isFinite(fix.getAccuracy())
                && fix.getAccuracy() > 0 && fix.getAccuracy() <= MAXIMUM_ACCURACY_METERS;
    }

    private boolean observe(LatLon target, NavigationLocation fix, boolean stationary, long nowMs) {
        if (stationary) {
            resetEvidence();
            return false;
        }
        if (lastSampleMs >= 0 && (nowMs <= lastSampleMs || nowMs - lastSampleMs > MAXIMUM_SAMPLE_GAP_MS)) {
            resetEvidence();
        }
        lastSampleMs = nowMs;
        if (movingSinceMs < 0) {
            movingSinceMs = nowMs;
            movingStartLocation = new NavigationLocation(fix);
        }
        double distance = GeoMath.distanceMeters(fix.getLatitude(), fix.getLongitude(), target.lat, target.lon);
        closestUpperDistance = Math.min(closestUpperDistance, distance + fix.getAccuracy());
        return confirmGrowth(distance - fix.getAccuracy() - closestUpperDistance, nowMs);
    }

    private boolean confirmGrowth(double growth, long nowMs) {
        if (growth < MINIMUM_GROWTH_METERS) {
            divergenceSinceMs = -1;
            return false;
        }
        if (divergenceSinceMs < 0) {
            divergenceSinceMs = nowMs;
        }
        return nowMs - divergenceSinceMs >= CONFIRMATION_MS;
    }

    private boolean canRetry(NavigationLocation fix, long nowMs) {
        if (lastAttemptLocation == null) {
            return true;
        }
        return nowMs - lastAttemptMs >= RETRY_INTERVAL_MS
                && hasMovedEnough(lastAttemptLocation, fix);
    }

    private boolean shouldProbe(NavigationLocation fix, long nowMs) {
        if (movingStartLocation == null) {
            return false;
        }
        long movingForMs = nowMs - movingSinceMs;
        // Driving reaches a different road access point quickly; walking still has a timed fallback.
        // Candidates must pass road-access validation before replacing current guidance.
        return movingForMs >= MINIMUM_PROBE_INTERVAL_MS
                && (movingForMs >= MOVING_PROBE_INTERVAL_MS || hasMovedEnough(movingStartLocation, fix));
    }

    private static boolean hasMovedEnough(NavigationLocation from, NavigationLocation to) {
        return from.distanceTo(to) >= MINIMUM_GROWTH_METERS + from.getAccuracy() + to.getAccuracy();
    }
}
