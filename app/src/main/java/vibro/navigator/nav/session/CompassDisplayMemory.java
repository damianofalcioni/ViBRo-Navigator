package vibro.navigator.nav.session;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.CompassRadiusTransition;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavStateComposer;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

final class CompassDisplayMemory {
    private static final long NO_COMPASS_RADIUS_UPDATE_TIME_MS = -1L;

    @Nullable
    private CompassRouteGeometry routeGeometry;
    @Nullable
    private Float lastVisibleRadiusMeters;
    @Nullable
    private Float lastReliableMovingVisibleRadiusMeters;
    private float lastSmoothedAccuracyMeters = Float.NaN;
    private long lastRadiusUpdateTimeMs = NO_COMPASS_RADIUS_UPDATE_TIME_MS;
    @NonNull
    private final CompassRadiusTransition radiusTransition = new CompassRadiusTransition(1_000L);

    void reset() {
        routeGeometry = null;
        lastVisibleRadiusMeters = null;
        lastReliableMovingVisibleRadiusMeters = null;
        lastSmoothedAccuracyMeters = Float.NaN;
        lastRadiusUpdateTimeMs = NO_COMPASS_RADIUS_UPDATE_TIME_MS;
        radiusTransition.reset();
    }

    void onRouteApplied(@NonNull GeoJsonRoute route, @NonNull PolylineIndex polylineIndex) {
        routeGeometry = NavStateComposer.buildCompassRouteGeometry(route, polylineIndex);
        lastVisibleRadiusMeters = null;
        lastSmoothedAccuracyMeters = Float.NaN;
        lastRadiusUpdateTimeMs = NO_COMPASS_RADIUS_UPDATE_TIME_MS;
        radiusTransition.reset();
    }

    void rememberSmoothedAccuracyMeters(float smoothedAccuracyMeters) {
        lastSmoothedAccuracyMeters = smoothedAccuracyMeters;
    }

    float resolveAccuracyMeters(float fallbackAccuracyMeters) {
        if (Float.isFinite(lastSmoothedAccuracyMeters) && lastSmoothedAccuracyMeters > 0f) {
            return lastSmoothedAccuracyMeters;
        }
        return Float.isFinite(fallbackAccuracyMeters) && fallbackAccuracyMeters > 0f
                ? fallbackAccuracyMeters
                : 0f;
    }

    long resolveRadiusUpdateDeltaMs(long nowMs) {
        if (lastRadiusUpdateTimeMs == NO_COMPASS_RADIUS_UPDATE_TIME_MS || nowMs <= lastRadiusUpdateTimeMs) {
            return 0L;
        }
        return nowMs - lastRadiusUpdateTimeMs;
    }

    void rememberCompassState(
            @NonNull NavState state,
            long nowMs,
            @Nullable Location lastFiltered,
            boolean likelyStationary
    ) {
        if (state.routeStatus.compassState == null) {
            return;
        }
        lastVisibleRadiusMeters = state.routeStatus.compassState.radiusState.visibleRadiusMeters;
        lastRadiusUpdateTimeMs = nowMs;
        if (lastFiltered != null && NavCompassStateFactory.hasReliableMovingSpeed(lastFiltered, likelyStationary)) {
            lastReliableMovingVisibleRadiusMeters = state.routeStatus.compassState.radiusState.visibleRadiusMeters;
        }
    }

    @Nullable
    CompassRouteGeometry routeGeometry() {
        return routeGeometry;
    }

    @NonNull
    CompassRadiusTransition radiusTransition() {
        return radiusTransition;
    }

    @Nullable
    Float lastVisibleRadiusMeters() {
        return lastVisibleRadiusMeters;
    }

    @Nullable
    Float lastReliableMovingVisibleRadiusMeters() {
        return lastReliableMovingVisibleRadiusMeters;
    }
}
