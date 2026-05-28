package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.compass.CompassRadiusTransition;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

import java.util.List;

final class CompassDisplayMemory {
    private static final long NO_COMPASS_RADIUS_UPDATE_TIME_MS = -1L;
    private static final double TURN_TARGET_LOOKBACK_METERS = 20.0;
    private static final double MIN_TURN_TARGET_BASELINE_METERS = 3.0;

    @Nullable
    private CompassRouteGeometry routeGeometry;
    @Nullable
    private Float lastVisibleRadiusMeters;
    @Nullable
    private Float lastReliableMovingVisibleRadiusMeters;
    @Nullable
    private CompassOrientationCue activeTurnManeuverCue;
    @Nullable
    private Integer activeTurnManeuverDegrees;
    @Nullable
    private Integer activeTurnManeuverTrackIndex;
    private float lastSmoothedAccuracyMeters = Float.NaN;
    private long lastRadiusUpdateTimeMs = NO_COMPASS_RADIUS_UPDATE_TIME_MS;
    @NonNull
    private final CompassRadiusTransition radiusTransition = new CompassRadiusTransition(1_000L);

    void reset() {
        routeGeometry = null;
        lastVisibleRadiusMeters = null;
        lastReliableMovingVisibleRadiusMeters = null;
        clearTurnManeuverCue();
        lastSmoothedAccuracyMeters = Float.NaN;
        lastRadiusUpdateTimeMs = NO_COMPASS_RADIUS_UPDATE_TIME_MS;
        radiusTransition.reset();
    }

    void onRouteApplied(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<LatLon> intermediateStops
    ) {
        routeGeometry = NavStateComposer.buildCompassRouteGeometry(route, polylineIndex, intermediateStops);
        lastVisibleRadiusMeters = null;
        clearTurnManeuverCue();
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

    @Nullable
    CompassOrientationCue resolveOrientationCue(
            @Nullable CompassOrientationCue stationaryOrientationCue,
            @Nullable Integer turnManeuverDegrees,
            @Nullable Integer turnManeuverTrackIndex,
            @NonNull PolylineIndex polylineIndex,
            @Nullable Double headingDegrees
    ) {
        if (turnManeuverDegrees == null) {
            clearTurnManeuverCue();
            return stationaryOrientationCue;
        }
        if (isActiveTurnManeuverCue(turnManeuverDegrees, turnManeuverTrackIndex)) {
            return activeTurnManeuverCue;
        }
        activeTurnManeuverDegrees = turnManeuverDegrees;
        activeTurnManeuverTrackIndex = turnManeuverTrackIndex;
        activeTurnManeuverCue = resolveTurnManeuverCue(
                turnManeuverDegrees,
                turnManeuverTrackIndex,
                polylineIndex,
                headingDegrees
        );
        return activeTurnManeuverCue;
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
            @Nullable NavigationLocation lastFiltered,
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

    private boolean isActiveTurnManeuverCue(
            @NonNull Integer turnManeuverDegrees,
            @Nullable Integer turnManeuverTrackIndex
    ) {
        return activeTurnManeuverCue != null
                && turnManeuverDegrees.equals(activeTurnManeuverDegrees)
                && sameTrackIndex(turnManeuverTrackIndex, activeTurnManeuverTrackIndex);
    }

    private static boolean sameTrackIndex(@Nullable Integer first, @Nullable Integer second) {
        return first == null ? second == null : first.equals(second);
    }

    private void clearTurnManeuverCue() {
        activeTurnManeuverCue = null;
        activeTurnManeuverDegrees = null;
        activeTurnManeuverTrackIndex = null;
    }

    @NonNull
    private static CompassOrientationCue resolveTurnManeuverCue(
            @NonNull Integer turnManeuverDegrees,
            @Nullable Integer turnManeuverTrackIndex,
            @NonNull PolylineIndex polylineIndex,
            @Nullable Double fallbackHeadingDegrees
    ) {
        Double incomingBearingDegrees = resolveIncomingTurnBearingDegrees(turnManeuverTrackIndex, polylineIndex);
        return CompassOrientationCue.fromRelativeTurn(
                incomingBearingDegrees != null
                        ? incomingBearingDegrees.floatValue()
                        : normalizeHeading(fallbackHeadingDegrees),
                turnManeuverDegrees
        );
    }

    @Nullable
    private static Double resolveIncomingTurnBearingDegrees(
            @Nullable Integer turnManeuverTrackIndex,
            @NonNull PolylineIndex polylineIndex
    ) {
        if (turnManeuverTrackIndex == null || turnManeuverTrackIndex <= 0) {
            return null;
        }
        double turnAlongTrackMeters = polylineIndex.distanceAtPointIndex(turnManeuverTrackIndex);
        LatLon turnPoint = polylineIndex.pointAtDistance(turnAlongTrackMeters);
        LatLon beforeTurnPoint = polylineIndex.pointAtDistance(
                Math.max(0.0, turnAlongTrackMeters - TURN_TARGET_LOOKBACK_METERS)
        );
        if (turnPoint == null
                || beforeTurnPoint == null
                || GeoMath.distanceMeters(
                        beforeTurnPoint.lat,
                        beforeTurnPoint.lon,
                        turnPoint.lat,
                        turnPoint.lon
                ) < MIN_TURN_TARGET_BASELINE_METERS) {
            return null;
        }
        return GeoMath.bearingDegrees(beforeTurnPoint.lat, beforeTurnPoint.lon, turnPoint.lat, turnPoint.lon);
    }

    private static float normalizeHeading(@Nullable Double headingDegrees) {
        if (headingDegrees == null || !Double.isFinite(headingDegrees)) {
            return 0f;
        }
        double normalized = headingDegrees % 360.0;
        if (normalized < 0.0) {
            normalized += 360.0;
        }
        return (float) normalized;
    }
}
