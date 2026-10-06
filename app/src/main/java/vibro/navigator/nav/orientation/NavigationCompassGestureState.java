package vibro.navigator.nav.orientation;

import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.CompassPerspectiveScale;
import vibro.navigator.nav.compass.CompassDisplayStateCache;

/** Display-only adjustments relative to the automatic/tap-selected compass viewport. */
public final class NavigationCompassGestureState {
    private static final int MIN_ZOOM_LEVEL = -2;
    private static final int MAX_ZOOM_LEVEL = 2;
    private static final float MIN_INCLINATION = 0.25f;

    private int zoomLevel;
    private float inclination = 1f;
    private boolean zoomEnabled;
    private boolean tiltEnabled;
    private final CompassDisplayStateCache zoomStateCache = new CompassDisplayStateCache();

    @Nullable
    public NavCompassState apply(@Nullable NavCompassState state, boolean perspectiveViewEnabled) {
        zoomEnabled = state != null && state.displayMode.movingScaleActive;
        tiltEnabled = state != null && perspectiveViewEnabled;
        if (state == null) {
            zoomLevel = 0;
            inclination = 1f;
            zoomStateCache.clear();
        }
        if (!zoomEnabled || zoomLevel == 0) {
            return state;
        }
        return zoomStateCache.resolve(state, true,
                state.radiusState.visibleRadiusMeters * (float) Math.pow(2.0, -zoomLevel));
    }

    public boolean isZoomEnabled() {
        return zoomEnabled;
    }

    public boolean isTiltEnabled() {
        return tiltEnabled;
    }

    public boolean zoomBy(int steps) {
        if (!zoomEnabled) {
            return false;
        }
        int nextLevel = (int) Math.max(MIN_ZOOM_LEVEL, Math.min(MAX_ZOOM_LEVEL, (long) zoomLevel + steps));
        boolean changed = nextLevel != zoomLevel;
        zoomLevel = nextLevel;
        return changed;
    }

    public boolean tiltBy(float delta) {
        if (!tiltEnabled || !Float.isFinite(delta)) {
            return false;
        }
        float nextInclination = Math.max(MIN_INCLINATION,
                Math.min(CompassPerspectiveScale.maximumProgress(), inclination + delta));
        boolean changed = inclination != nextInclination;
        inclination = nextInclination;
        return changed;
    }

    public float perspectiveProgress(float transitionProgress) {
        return transitionProgress * inclination;
    }
}
