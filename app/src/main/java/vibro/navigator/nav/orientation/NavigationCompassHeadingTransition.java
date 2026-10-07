package vibro.navigator.nav.orientation;

import androidx.annotation.NonNull;

import vibro.navigator.nav.compass.NavCompassHeadingRefresh;
import vibro.navigator.nav.compass.NavCompassState;

/** Animates the display handoff from direct guidance to road guidance. */
final class NavigationCompassHeadingTransition {
    private static final long DURATION_MS = 1_000L;
    private boolean previousBeeline;
    private boolean active;
    private float displayedHeading;
    private float startHeading;
    private float previousTargetHeading;
    private float rotationDegrees;
    private long startElapsedMs;

    @NonNull
    NavCompassState resolve(@NonNull NavCompassState state, long nowElapsedMs) {
        // Direct-guidance projections cover route-start, native and synthetic beelines.
        boolean beeline = state.routeStartApproachProjection != null;
        if (previousBeeline && !beeline && !state.displayMode.straightLineMode) {
            startHeading = displayedHeading;
            previousTargetHeading = state.displayMode.headingDegrees;
            rotationDegrees = shortestDelta(startHeading, previousTargetHeading);
            startElapsedMs = nowElapsedMs;
            active = true;
        }
        previousBeeline = beeline;
        if (beeline || state.displayMode.straightLineMode) {
            active = false;
        }
        float heading = state.displayMode.headingDegrees;
        if (!active) {
            displayedHeading = heading;
            return state;
        }
        // Keep the chosen rotation direction if updated road headings cross the opposite bearing.
        rotationDegrees += shortestDelta(previousTargetHeading, heading);
        previousTargetHeading = heading;
        float fraction = Math.min(1f, Math.max(0L, nowElapsedMs - startElapsedMs) / (float) DURATION_MS);
        float progress = fraction * fraction * (3f - 2f * fraction);
        heading = (startHeading + rotationDegrees * progress + 720f) % 360f;
        active = fraction < 1f;
        displayedHeading = heading;
        return NavCompassHeadingRefresh.apply(state, (double) heading, state.displayMode.headingAccuracyDegrees);
    }

    boolean isActive() {
        return active;
    }

    private static float shortestDelta(float from, float to) {
        return (to - from + 540f) % 360f - 180f;
    }

    void reset() {
        previousBeeline = false;
        active = false;
    }
}
