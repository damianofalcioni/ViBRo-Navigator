package vibro.navigator.nav.orientation;

import androidx.annotation.Nullable;

final class HeadingAccuracyPolicy {

    static final long MAX_LEGACY_ORIENTATION_ACCURACY_AGE_MS = 5_000L;

    private static final double LOW_HEADING_ACCURACY_DEGREES = 35.0;
    private static final double MEDIUM_HEADING_ACCURACY_DEGREES = 20.0;
    private static final double HIGH_HEADING_ACCURACY_DEGREES = 10.0;
    private static final double UNRELIABLE_LEGACY_HEADING_ACCURACY_DEGREES = 90.0;

    private HeadingAccuracyPolicy() {
    }

    static boolean isAccuracyHighEnough(
            int selectedAccuracy,
            @Nullable Integer legacyOrientationAccuracy,
            long legacyOrientationAccuracyElapsedRealtimeMs,
            long nowElapsedRealtimeMs
    ) {
        Integer freshLegacyAccuracy = freshLegacyOrientationAccuracy(
                legacyOrientationAccuracy,
                legacyOrientationAccuracyElapsedRealtimeMs,
                nowElapsedRealtimeMs
        );
        return selectedAccuracy >= HeadingAccuracyStatus.HIGH
                && (freshLegacyAccuracy == null
                || freshLegacyAccuracy >= HeadingAccuracyStatus.MEDIUM);
    }

    @Nullable
    static Double effectiveHeadingAccuracyDegrees(
            int selectedAccuracy,
            @Nullable Double selectedHeadingAccuracyDegrees,
            @Nullable Integer legacyOrientationAccuracy,
            long legacyOrientationAccuracyElapsedRealtimeMs,
            long nowElapsedRealtimeMs
    ) {
        Double selectedAccuracyDegrees = selectedHeadingAccuracyDegrees(
                selectedAccuracy,
                selectedHeadingAccuracyDegrees
        );
        Double legacyAccuracyDegrees = legacyOrientationHeadingAccuracyDegrees(
                legacyOrientationAccuracy,
                legacyOrientationAccuracyElapsedRealtimeMs,
                nowElapsedRealtimeMs
        );
        return maxNullable(selectedAccuracyDegrees, legacyAccuracyDegrees);
    }

    @Nullable
    static Integer freshLegacyOrientationAccuracy(
            @Nullable Integer legacyOrientationAccuracy,
            long legacyOrientationAccuracyElapsedRealtimeMs,
            long nowElapsedRealtimeMs
    ) {
        if (legacyOrientationAccuracy == null || legacyOrientationAccuracyElapsedRealtimeMs < 0L) {
            return null;
        }
        long ageMs = nowElapsedRealtimeMs - legacyOrientationAccuracyElapsedRealtimeMs;
        return ageMs >= 0L && ageMs <= MAX_LEGACY_ORIENTATION_ACCURACY_AGE_MS
                ? legacyOrientationAccuracy
                : null;
    }

    @Nullable
    private static Double selectedHeadingAccuracyDegrees(
            int selectedAccuracy,
            @Nullable Double selectedHeadingAccuracyDegrees
    ) {
        return selectedHeadingAccuracyDegrees != null
                ? selectedHeadingAccuracyDegrees
                : statusHeadingAccuracyDegrees(selectedAccuracy, false);
    }

    @Nullable
    private static Double legacyOrientationHeadingAccuracyDegrees(
            @Nullable Integer legacyOrientationAccuracy,
            long legacyOrientationAccuracyElapsedRealtimeMs,
            long nowElapsedRealtimeMs
    ) {
        Integer freshLegacyAccuracy = freshLegacyOrientationAccuracy(
                legacyOrientationAccuracy,
                legacyOrientationAccuracyElapsedRealtimeMs,
                nowElapsedRealtimeMs
        );
        return freshLegacyAccuracy == null
                ? null
                : statusHeadingAccuracyDegrees(freshLegacyAccuracy, true);
    }

    @Nullable
    private static Double maxNullable(@Nullable Double first, @Nullable Double second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return Math.max(first, second);
    }

    @Nullable
    private static Double statusHeadingAccuracyDegrees(int accuracy, boolean unreliableIsExplicitPoor) {
        switch (accuracy) {
            case HeadingAccuracyStatus.HIGH:
                return unreliableIsExplicitPoor ? null : HIGH_HEADING_ACCURACY_DEGREES;
            case HeadingAccuracyStatus.MEDIUM:
                return MEDIUM_HEADING_ACCURACY_DEGREES;
            case HeadingAccuracyStatus.LOW:
                return LOW_HEADING_ACCURACY_DEGREES;
            case HeadingAccuracyStatus.UNRELIABLE:
            default:
                return unreliableIsExplicitPoor ? UNRELIABLE_LEGACY_HEADING_ACCURACY_DEGREES : null;
        }
    }
}
