package vibro.navigator.nav.location;

import java.util.Objects;

/** A single precise speed reading cannot establish motion; preserve normal sparse cadence. */
final class NavigationNativeSpeedEvidence {
    private static final long MIN_SPACING_MS = 2_000L;
    private static final long MAX_GAP_MS = 65_000L;
    private long anchorTimeMs = -1;
    private long lastTimeMs = -1;
    private String provider;
    private boolean confirmed;

    void reset() {
        anchorTimeMs = -1;
        lastTimeMs = -1;
        provider = null;
        confirmed = false;
    }

    boolean record(NavigationLocation location) {
        if (!hasConfidentMovingSpeed(location)) {
            reset();
            return false;
        }
        long timeMs = location.getElapsedRealtimeOrTimeMs();
        if (anchorTimeMs < 0 || !Objects.equals(provider, location.getProvider()) || timeMs - lastTimeMs > MAX_GAP_MS) {
            reset();
            anchorTimeMs = timeMs;
            provider = location.getProvider();
        }
        lastTimeMs = timeMs;
        if (timeMs - anchorTimeMs >= MIN_SPACING_MS) {
            confirmed = true;
            anchorTimeMs = timeMs;
        }
        return confirmed;
    }

    static boolean hasConfidentMovingSpeed(NavigationLocation location) {
        return location.getSpeed() > 0.35f && hasConfidentPositiveSpeed(location);
    }

    static boolean hasConfidentPositiveSpeed(NavigationLocation location) {
        if (!location.hasSpeed() || !location.hasSpeedAccuracy()) {
            return false;
        }
        float speed = location.getSpeed();
        float uncertainty = location.getSpeedAccuracyMetersPerSecond();
        // Corroborated position progress may establish slow walking below the native-only
        // movement threshold. Its measured speed must still exclude zero.
        return Float.isFinite(speed) && Float.isFinite(uncertainty) && uncertainty >= 0
                && speed - 2.0 * uncertainty > 0.0;
    }
}
