package vibro.navigator.nav.compass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

import java.util.List;

public final class NavCompassStateFactory {
    private NavCompassStateFactory() {
    }

    @NonNull
    public static CompassRouteGeometry buildCompassRouteGeometry(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index
    ) {
        return CompassRouteGeometryFactory.build(route, index);
    }

    @NonNull
    public static CompassRouteGeometry buildCompassRouteGeometry(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<LatLon> intermediateStops
    ) {
        return CompassRouteGeometryFactory.build(route, index, intermediateStops);
    }

    @NonNull
    public static String buildGpsStatusLine(
            float speedMps,
            @Nullable NavigationLocation currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount,
            @NonNull Context context
    ) {
        return buildGpsStatusLine(
                speedMps,
                currentLocation,
                accuracyMeters,
                fixedSatelliteCount,
                acquiredFixCount,
                new AndroidNavigationTextResources(context)
        );
    }

    @NonNull
    public static String buildGpsStatusLine(
            float speedMps,
            @Nullable NavigationLocation currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount,
            @NonNull NavigationTextResources textResources
    ) {
        return NavigationTextFormatter.formatGpsStatus(
                textResources,
                speedMps,
                elevationMeters(currentLocation),
                accuracyMeters,
                bearingDegrees(currentLocation),
                bearingAccuracyDegrees(currentLocation),
                fixedSatelliteCount,
                acquiredFixCount
        );
    }

    @Nullable
    private static Double elevationMeters(@Nullable NavigationLocation currentLocation) {
        return currentLocation != null && currentLocation.hasAltitude()
                ? currentLocation.getAltitude()
                : null;
    }

    @Nullable
    private static Float bearingDegrees(@Nullable NavigationLocation currentLocation) {
        return currentLocation != null && currentLocation.hasBearing()
                ? currentLocation.getBearing()
                : null;
    }

    @Nullable
    private static Float bearingAccuracyDegrees(@Nullable NavigationLocation currentLocation) {
        return currentLocation != null
                && currentLocation.hasBearingAccuracy()
                ? currentLocation.getBearingAccuracyDegrees()
                : null;
    }

    @Nullable
    public static NavCompassState buildCompassState(@NonNull NavCompassStateInput input) {
        return buildCompassState(
                input.route,
                input.index,
                input.alongTrackMeters,
                input.currentLocation,
                input.speedMps,
                input.likelyStationary,
                input.accuracyMeters,
                input.destinationReachedRadiusMeters,
                input.headingDegrees,
                input.headingAccuracyDegrees,
                input.previousVisibleRadiusMeters,
                input.previousReliableMovingVisibleRadiusMeters,
                input.radiusUpdateDeltaMs,
                input.routeGeometry,
                input.radiusTransition,
                input.orientationCue,
                input.routeStartApproachTarget,
                input.nowMs
        );
    }

    @Nullable
    public static NavCompassState buildCompassState(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            @NonNull NavigationLocation currentLocation,
            float speedMps,
            boolean likelyStationary,
            float compassAccuracyMeters,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            @Nullable CompassRouteGeometry compassRouteGeometry,
            @Nullable CompassRadiusTransition compassRadiusTransition,
            long nowMs
    ) {
        return buildCompassState(
                route,
                index,
                alongTrackMeters,
                currentLocation,
                speedMps,
                likelyStationary,
                compassAccuracyMeters,
                (float) NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(compassAccuracyMeters),
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                compassRouteGeometry,
                compassRadiusTransition,
                null,
                null,
                nowMs
        );
    }

    @Nullable
    public static NavCompassState buildCompassState(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            @NonNull NavigationLocation currentLocation,
            float speedMps,
            boolean likelyStationary,
            float compassAccuracyMeters,
            float destinationReachedRadiusMeters,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            @Nullable CompassRouteGeometry compassRouteGeometry,
            @Nullable CompassRadiusTransition compassRadiusTransition,
            @Nullable CompassOrientationCue orientationCue,
            @Nullable LatLon routeStartApproachTarget,
            long nowMs
    ) {
        return buildCompassState(
                route,
                index,
                alongTrackMeters,
                currentLocation,
                speedMps,
                likelyStationary,
                compassAccuracyMeters,
                destinationReachedRadiusMeters,
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                compassRouteGeometry,
                compassRadiusTransition,
                orientationCue,
                routeStartApproachTarget,
                nowMs,
                false
        );
    }

    @Nullable
    static NavCompassState buildCompassState(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            @NonNull NavigationLocation currentLocation,
            float speedMps,
            boolean likelyStationary,
            float compassAccuracyMeters,
            float destinationReachedRadiusMeters,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            @Nullable CompassRouteGeometry compassRouteGeometry,
            @Nullable CompassRadiusTransition compassRadiusTransition,
            @Nullable CompassOrientationCue orientationCue,
            @Nullable LatLon routeStartApproachTarget,
            long nowMs,
            boolean straightLineMode
    ) {
        if (route.track.isEmpty()) {
            return null;
        }

        double currentLat = currentLocation.getLatitude();
        double currentLon = currentLocation.getLongitude();
        CompassRouteGeometry routeGeometry = compassRouteGeometry != null
                ? compassRouteGeometry
                : CompassRouteGeometryFactory.build(route, index);

        LatLon routeEndPoint = route.track.get(route.track.size() - 1);
        float destinationEastMeters = (float) GeoMath.eastMeters(currentLat, currentLon, routeEndPoint.lat, routeEndPoint.lon);
        float destinationNorthMeters = (float) GeoMath.northMeters(currentLat, routeEndPoint.lat);
        double destinationDistanceMeters = Math.hypot(destinationEastMeters, destinationNorthMeters);
        float routeThresholdMeters =
                (float) RouteDeviationPolicy.resolveOffTrackThresholdMeters(compassAccuracyMeters);
        CompassDestinationProjection routeStartApproachProjection = CompassRouteStartApproachProjectionFactory.resolve(
                currentLat,
                currentLon,
                routeStartApproachTarget,
                routeThresholdMeters
        );
        double furthestDistanceMeters = Math.max(
                resolveFurthestRouteSampleDistanceMeters(routeGeometry, currentLat, currentLon),
                destinationDistanceMeters
        );
        furthestDistanceMeters = CompassRouteStartApproachProjectionFactory.extendFurthestDistance(
                furthestDistanceMeters,
                routeStartApproachProjection
        );
        CompassRadiusResolver.State radiusState = CompassRadiusResolver.resolve(
                furthestDistanceMeters,
                currentLocation,
                speedMps,
                likelyStationary,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                compassRadiusTransition,
                nowMs
        );
        float fullRouteReferenceSpeedMps = sanitizeReferenceSpeedMps(speedMps);
        float sixtySecondReferenceSpeedMps = CompassRadiusResolver.movingLegendReferenceSpeedMps(
                radiusState.sixtySecondVisibleRadiusMeters
        );
        float referenceSpeedMps = radiusState.usingMovingScale
                ? CompassRadiusResolver.movingLegendReferenceSpeedMps(radiusState.visibleRadiusMeters)
                : fullRouteReferenceSpeedMps;
        float resolvedHeading = normalizeHeading(headingDegrees == null ? 0.0 : headingDegrees);
        return NavCompassState.fromRouteGeometry(new NavCompassRouteGeometryInput(
                new CompassDisplayMetrics(
                        resolvedHeading,
                        sanitizeHeadingAccuracyDegrees(headingAccuracyDegrees),
                        referenceSpeedMps,
                        fullRouteReferenceSpeedMps,
                        sixtySecondReferenceSpeedMps,
                        radiusState.usingMovingScale,
                        straightLineMode
                ),
                new CompassRadiusMetrics(
                        radiusState.visibleRadiusMeters,
                        radiusState.fullRouteVisibleRadiusMeters,
                        radiusState.sixtySecondVisibleRadiusMeters,
                        sanitizeAccuracyMeters(compassAccuracyMeters),
                        routeThresholdMeters
                ),
                routeGeometry,
                currentLat,
                currentLon,
                routeGeometry.passedRoutePointCount(alongTrackMeters),
                new CompassDestinationProjection(
                        destinationEastMeters,
                        destinationNorthMeters,
                        destinationReachedRadiusMeters,
                        destinationDistanceMeters <= radiusState.visibleRadiusMeters
                ),
                routeStartApproachProjection,
                orientationCue
        ));
    }

    public static boolean hasReliableMovingSpeed(
            @NonNull NavigationLocation currentLocation,
            boolean likelyStationary
    ) {
        return CompassRadiusResolver.hasReliableMovingSpeed(currentLocation, likelyStationary);
    }

    public static float smoothVisibleRadiusMeters(
            float targetVisibleRadiusMeters,
            @Nullable Float previousVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs
    ) {
        return CompassRadiusResolver.smoothVisibleRadiusMeters(
                targetVisibleRadiusMeters,
                previousVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs
        );
    }

    private static double resolveFurthestRouteSampleDistanceMeters(
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLat,
            double currentLon
    ) {
        double furthestDistanceMeters = 0.0;
        for (int i = 0; i < routeGeometry.routeSamplePointCount(); i++) {
            LatLon point = routeGeometry.routeSamplePointAt(i);
            if (point == null) {
                continue;
            }
            double eastMeters = GeoMath.eastMeters(currentLat, currentLon, point.lat, point.lon);
            double northMeters = GeoMath.northMeters(currentLat, point.lat);
            furthestDistanceMeters = Math.max(furthestDistanceMeters, Math.hypot(eastMeters, northMeters));
        }
        return furthestDistanceMeters;
    }

    private static float sanitizeAccuracyMeters(float accuracyMeters) {
        return Float.isFinite(accuracyMeters) && accuracyMeters > 0f ? accuracyMeters : 0f;
    }

    private static float sanitizeReferenceSpeedMps(float speedMps) {
        return Float.isFinite(speedMps) && speedMps > 0f ? Math.max(1f, speedMps) : 1f;
    }

    @Nullable
    private static Float sanitizeHeadingAccuracyDegrees(@Nullable Float headingAccuracyDegrees) {
        return headingAccuracyDegrees != null
                && Float.isFinite(headingAccuracyDegrees)
                && headingAccuracyDegrees > 0f
                ? headingAccuracyDegrees
                : null;
    }

    private static float normalizeHeading(double headingDegrees) {
        double normalized = headingDegrees % 360.0;
        if (normalized < 0.0) {
            normalized += 360.0;
        }
        return (float) normalized;
    }

}
