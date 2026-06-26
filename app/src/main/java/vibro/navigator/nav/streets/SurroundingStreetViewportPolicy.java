package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.NavCompassState;

final class SurroundingStreetViewportPolicy {
    static final float MAX_VISIBLE_RADIUS_METERS = 500f;
    static final float EXTRACTION_PADDING_METERS = 24f;

    boolean shouldShow(@Nullable NavCompassState compassState) {
        return compassState != null
                && compassState.displayMode.movingScaleActive
                && isZoomedIn(compassState.radiusState.visibleRadiusMeters);
    }

    double extractionRadiusMeters(@NonNull NavCompassState compassState) {
        return compassState.radiusState.visibleRadiusMeters + EXTRACTION_PADDING_METERS;
    }

    private boolean isZoomedIn(float visibleRadiusMeters) {
        return Float.isFinite(visibleRadiusMeters)
                && visibleRadiusMeters > 0f
                && visibleRadiusMeters <= MAX_VISIBLE_RADIUS_METERS;
    }
}
