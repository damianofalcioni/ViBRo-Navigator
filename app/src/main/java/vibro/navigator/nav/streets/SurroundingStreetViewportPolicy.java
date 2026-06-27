package vibro.navigator.nav.streets;

import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.NavCompassState;

final class SurroundingStreetViewportPolicy {
    boolean shouldShow(@Nullable NavCompassState compassState) {
        return compassState != null
                && compassState.displayMode.movingScaleActive
                && isValidVisibleRadius(compassState.radiusState.visibleRadiusMeters);
    }

    private boolean isValidVisibleRadius(float visibleRadiusMeters) {
        return Float.isFinite(visibleRadiusMeters)
                && visibleRadiusMeters > 0f;
    }
}
