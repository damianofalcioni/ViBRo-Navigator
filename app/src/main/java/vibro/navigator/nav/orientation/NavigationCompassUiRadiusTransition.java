package vibro.navigator.nav.orientation;

import androidx.annotation.NonNull;

import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassStateFactory;

/** Smooths UI tap overrides independently of the automatic compass radius policy. */
final class NavigationCompassUiRadiusTransition {
    private static final long NO_UPDATE_TIME = -1L;
    private static final float TARGET_TOLERANCE_RATIO = 0.002f;
    private static final float TARGET_TOLERANCE_METERS = 0.5f;

    private boolean active;
    private Float lastResolvedVisibleRadiusMeters;
    private long lastUpdateElapsedMs = NO_UPDATE_TIME;

    void start(long nowElapsedMs, boolean animate) {
        active = animate;
        lastUpdateElapsedMs = nowElapsedMs;
    }

    @NonNull
    NavCompassState resolve(
            @NonNull NavCompassState automaticState,
            @NonNull NavCompassState targetState,
            long nowElapsedMs,
            boolean animate
    ) {
        if (!animate || !active) {
            active = false;
            remember(targetState.radiusState.visibleRadiusMeters, nowElapsedMs);
            return targetState;
        }
        float previousRadiusMeters = lastResolvedVisibleRadiusMeters != null
                ? lastResolvedVisibleRadiusMeters
                : automaticState.radiusState.visibleRadiusMeters;
        long deltaMs = lastUpdateElapsedMs == NO_UPDATE_TIME
                ? 0L
                : Math.max(0L, nowElapsedMs - lastUpdateElapsedMs);
        float resolvedRadiusMeters = deltaMs <= 0L
                ? previousRadiusMeters
                : NavCompassStateFactory.smoothVisibleRadiusMeters(
                        targetState.radiusState.visibleRadiusMeters,
                        previousRadiusMeters,
                        deltaMs
                );
        if (isAtTarget(resolvedRadiusMeters, targetState.radiusState.visibleRadiusMeters)) {
            active = false;
            remember(targetState.radiusState.visibleRadiusMeters, nowElapsedMs);
            return targetState;
        }
        remember(resolvedRadiusMeters, nowElapsedMs);
        return targetState.withDisplayMode(targetState.displayMode.movingScaleActive, resolvedRadiusMeters);
    }

    boolean isActive() {
        return active;
    }

    void reset() {
        active = false;
        lastResolvedVisibleRadiusMeters = null;
        lastUpdateElapsedMs = NO_UPDATE_TIME;
    }

    private void remember(float radiusMeters, long nowElapsedMs) {
        lastResolvedVisibleRadiusMeters = radiusMeters;
        lastUpdateElapsedMs = nowElapsedMs;
    }

    private static boolean isAtTarget(float resolvedRadiusMeters, float targetRadiusMeters) {
        float toleranceMeters = Math.max(TARGET_TOLERANCE_METERS,
                Math.abs(targetRadiusMeters) * TARGET_TOLERANCE_RATIO);
        return Math.abs(resolvedRadiusMeters - targetRadiusMeters) <= toleranceMeters;
    }
}
