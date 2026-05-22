package vibro.navigator.nav.orientation;

import android.hardware.SensorManager;

import androidx.annotation.Nullable;

final class LegacyOrientationAccuracy {

    private int accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    private long elapsedRealtimeMs = -1L;
    private boolean hasAccuracy;

    void reset() {
        accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
        elapsedRealtimeMs = -1L;
        hasAccuracy = false;
    }

    void remember(int updatedAccuracy, long nowElapsedRealtimeMs) {
        accuracy = updatedAccuracy;
        elapsedRealtimeMs = nowElapsedRealtimeMs;
        hasAccuracy = true;
    }

    boolean refreshTimestamp(long nowElapsedRealtimeMs) {
        if (!hasAccuracy) {
            return false;
        }
        elapsedRealtimeMs = nowElapsedRealtimeMs;
        return true;
    }

    @Nullable
    Integer freshAccuracy(long nowElapsedRealtimeMs) {
        if (!hasAccuracy || elapsedRealtimeMs < 0L) {
            return null;
        }
        long ageMs = nowElapsedRealtimeMs - elapsedRealtimeMs;
        return ageMs >= 0L && ageMs <= HeadingAccuracyPolicy.MAX_LEGACY_ORIENTATION_ACCURACY_AGE_MS
                ? accuracy
                : null;
    }

    long freshElapsedRealtimeMs(long nowElapsedRealtimeMs) {
        return freshAccuracy(nowElapsedRealtimeMs) == null ? -1L : elapsedRealtimeMs;
    }
}
