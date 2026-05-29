package vibro.navigator.nav.orientation;

import androidx.annotation.Nullable;

public final class LegacyOrientationAccuracy {

    private int accuracy = HeadingAccuracyStatus.UNRELIABLE;
    private long elapsedRealtimeMs = -1L;
    private boolean hasAccuracy;

    public void reset() {
        accuracy = HeadingAccuracyStatus.UNRELIABLE;
        elapsedRealtimeMs = -1L;
        hasAccuracy = false;
    }

    public void remember(int updatedAccuracy, long nowElapsedRealtimeMs) {
        accuracy = updatedAccuracy;
        elapsedRealtimeMs = nowElapsedRealtimeMs;
        hasAccuracy = true;
    }

    public boolean refreshTimestamp(long nowElapsedRealtimeMs) {
        if (!hasAccuracy) {
            return false;
        }
        elapsedRealtimeMs = nowElapsedRealtimeMs;
        return true;
    }

    @Nullable
    public Integer freshAccuracy(long nowElapsedRealtimeMs) {
        if (!hasAccuracy || elapsedRealtimeMs < 0L) {
            return null;
        }
        long ageMs = nowElapsedRealtimeMs - elapsedRealtimeMs;
        return ageMs >= 0L && ageMs <= HeadingAccuracyPolicy.MAX_LEGACY_ORIENTATION_ACCURACY_AGE_MS
                ? accuracy
                : null;
    }

    public long freshElapsedRealtimeMs(long nowElapsedRealtimeMs) {
        return freshAccuracy(nowElapsedRealtimeMs) == null ? -1L : elapsedRealtimeMs;
    }
}
