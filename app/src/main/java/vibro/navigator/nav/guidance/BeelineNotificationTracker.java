package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

/** Tracks periodic command-16 reminders independently from reroute evidence. */
public final class BeelineNotificationTracker {
    private static final long CHECK_INTERVAL_MS = 10_000L;

    @Nullable
    private LatLon target;
    private long lastCheckMs = -1L;
    private double lastNotifiedDistanceMeters = Double.NaN;

    public void reset() {
        target = null;
        lastCheckMs = -1L;
        lastNotifiedDistanceMeters = Double.NaN;
    }

    public boolean shouldNotify(
            @NonNull LatLon activeTarget,
            double distanceMeters,
            long nowMs
    ) {
        if (!isSameTarget(activeTarget) || lastCheckMs < 0L || nowMs < lastCheckMs) {
            startLeg(activeTarget, distanceMeters, nowMs);
            return false;
        }
        if (nowMs - lastCheckMs < CHECK_INTERVAL_MS) {
            return false;
        }
        lastCheckMs = nowMs;
        if (distanceMeters <= lastNotifiedDistanceMeters) {
            return false;
        }
        lastNotifiedDistanceMeters = distanceMeters;
        return true;
    }

    public static long limitSuggestedUpdateInterval(long suggestedUpdateIntervalMs) {
        return suggestedUpdateIntervalMs <= 0L
                ? CHECK_INTERVAL_MS
                : Math.min(suggestedUpdateIntervalMs, CHECK_INTERVAL_MS);
    }

    private void startLeg(@NonNull LatLon activeTarget, double distanceMeters, long nowMs) {
        target = new LatLon(activeTarget.lat, activeTarget.lon);
        lastCheckMs = nowMs;
        lastNotifiedDistanceMeters = distanceMeters;
    }

    private boolean isSameTarget(@NonNull LatLon activeTarget) {
        return target != null
                && target.lat == activeTarget.lat
                && target.lon == activeTarget.lon;
    }
}
