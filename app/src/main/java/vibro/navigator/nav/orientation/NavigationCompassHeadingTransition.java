package vibro.navigator.nav.orientation;

import androidx.annotation.NonNull;

import vibro.navigator.nav.compass.NavCompassHeadingRefresh;
import vibro.navigator.nav.compass.NavCompassState;

/** Smooths heading-source handoffs without delaying live updates within a source. */
final class NavigationCompassHeadingTransition {
    private static final long DURATION_MS = 500L;
    private NavigationHeadingSource previousSource = NavigationHeadingSource.UNKNOWN;
    private boolean active;
    private float displayedHeading;
    private float startHeading;
    private float previousTargetHeading;
    private float rotationDegrees;
    private long startElapsedMs;

    @NonNull
    NavCompassState resolve(@NonNull NavCompassState state, long nowElapsedMs) {
        NavigationHeadingSource source = state.displayMode.headingSource;
        if (previousSource != NavigationHeadingSource.UNKNOWN && source != NavigationHeadingSource.UNKNOWN
                && source != previousSource) {
            startHeading = displayedHeading;
            previousTargetHeading = state.displayMode.headingDegrees;
            rotationDegrees = shortestDelta(startHeading, previousTargetHeading);
            startElapsedMs = nowElapsedMs;
            active = rotationDegrees != 0f;
        }
        previousSource = source;
        if (source == NavigationHeadingSource.UNKNOWN) {
            active = false;
        }
        float heading = state.displayMode.headingDegrees;
        if (!active) {
            displayedHeading = heading;
            return state;
        }
        // Preserve the chosen rotation direction if target updates cross the opposite bearing.
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
        previousSource = NavigationHeadingSource.UNKNOWN;
        active = false;
    }
}
