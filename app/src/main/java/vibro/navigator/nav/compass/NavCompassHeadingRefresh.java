package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavCompassHeadingRefresh {
    private NavCompassHeadingRefresh() {
    }

    @NonNull
    public static NavCompassState apply(
            @NonNull NavCompassState source,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees
    ) {
        float resolvedHeadingDegrees = normalizeHeading(headingDegrees);
        Float resolvedAccuracyDegrees = sanitizeHeadingAccuracy(headingAccuracyDegrees);
        if (Float.compare(source.displayMode.headingDegrees, resolvedHeadingDegrees) == 0
                && sameAccuracy(source.displayMode.headingAccuracyDegrees, resolvedAccuracyDegrees)) {
            return source;
        }
        return new NavCompassState(
                source,
                new CompassDisplayMode(
                        resolvedHeadingDegrees,
                        resolvedAccuracyDegrees,
                        source.displayMode.referenceSpeedMps,
                        source.displayMode.fullRouteReferenceSpeedMps,
                        source.displayMode.movingScaleReferenceSpeedMps,
                        source.displayMode.movingScaleHorizonSeconds,
                        source.displayMode.movingScaleSpeedBucket,
                        source.displayMode.movingScaleActive,
                        source.displayMode.straightLineMode
                ),
                source.streetOverlay
        );
    }

    public static boolean hasPendingRadiusChange(@NonNull NavCompassState state) {
        float targetRadiusMeters = state.radiusState.targetVisibleRadiusMeters(
                state.displayMode.movingScaleActive
        );
        return Math.abs(state.radiusState.visibleRadiusMeters - targetRadiusMeters) > 0.01f;
    }

    private static float normalizeHeading(@Nullable Double headingDegrees) {
        if (headingDegrees == null || !Double.isFinite(headingDegrees)) {
            return 0f;
        }
        double normalized = headingDegrees % 360.0;
        return (float) (normalized < 0.0 ? normalized + 360.0 : normalized);
    }

    @Nullable
    private static Float sanitizeHeadingAccuracy(@Nullable Float headingAccuracyDegrees) {
        return headingAccuracyDegrees != null
                && Float.isFinite(headingAccuracyDegrees)
                && headingAccuracyDegrees > 0f
                ? headingAccuracyDegrees
                : null;
    }

    private static boolean sameAccuracy(@Nullable Float first, @Nullable Float second) {
        return first == null ? second == null : second != null && Float.compare(first, second) == 0;
    }
}
