package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.policy.NavigationSpeedBucket;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

import java.util.Collections;
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
                input.previousMovingSpeedBucket,
                input.radiusUpdateDeltaMs,
                input.routeGeometry,
                input.radiusTransition,
                input.orientationCue,
                input.routeStartApproachTarget,
                input.blockedAreas,
                input.nowMs,
                false
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
                null,
                compassRadiusUpdateDeltaMs,
                compassRouteGeometry,
                compassRadiusTransition,
                orientationCue,
                routeStartApproachTarget,
                Collections.emptyList(),
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
            @Nullable NavigationSpeedBucket previousMovingSpeedBucket,
            long compassRadiusUpdateDeltaMs,
            @Nullable CompassRouteGeometry compassRouteGeometry,
            @Nullable CompassRadiusTransition compassRadiusTransition,
            @Nullable CompassOrientationCue orientationCue,
            @Nullable LatLon routeStartApproachTarget,
            @NonNull List<CompassBlockedArea> blockedAreas,
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
        float sanitizedCompassAccuracyMeters = sanitizeAccuracyMeters(compassAccuracyMeters);
        float routeThresholdMeters =
                (float) RouteDeviationPolicy.resolveOffTrackThresholdMeters(sanitizedCompassAccuracyMeters);
        CompassDestinationProjection routeStartApproachProjection = CompassRouteStartApproachProjectionFactory.resolve(
                currentLat,
                currentLon,
                routeStartApproachTarget,
                routeThresholdMeters
        );
        double furthestDistanceMeters = Math.max(
                CompassRouteDistanceResolver.furthestSampleDistanceMeters(routeGeometry, currentLat, currentLon),
                destinationDistanceMeters
        );
        furthestDistanceMeters = CompassRouteStartApproachProjectionFactory.extendFurthestDistance(
                furthestDistanceMeters,
                routeStartApproachProjection
        );
        furthestDistanceMeters = extendFurthestDistanceForBlockedAreas(furthestDistanceMeters, blockedAreas);
        CompassRadiusResolver.State radiusState = CompassRadiusResolver.resolve(
                furthestDistanceMeters,
                currentLocation,
                speedMps,
                likelyStationary,
                previousMovingSpeedBucket,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                compassRadiusTransition,
                nowMs
        );
        float fullRouteReferenceSpeedMps = sanitizeReferenceSpeedMps(speedMps);
        float movingScaleReferenceSpeedMps = CompassRadiusResolver.movingLegendReferenceSpeedMps(
                radiusState.movingScaleVisibleRadiusMeters,
                radiusState.movingScaleHorizonSeconds,
                0f
        );
        float referenceSpeedMps = radiusState.usingMovingScale
                ? movingScaleReferenceSpeedMps
                : fullRouteReferenceSpeedMps;
        float resolvedHeading = normalizeHeading(headingDegrees == null ? 0.0 : headingDegrees);
        return NavCompassState.fromRouteGeometry(new NavCompassRouteGeometryInput(
                new CompassDisplayMetrics(
                        resolvedHeading,
                        sanitizeHeadingAccuracyDegrees(headingAccuracyDegrees),
                        referenceSpeedMps,
                        fullRouteReferenceSpeedMps,
                        movingScaleReferenceSpeedMps,
                        radiusState.movingScaleHorizonSeconds,
                        radiusState.movingScaleSpeedBucket,
                        radiusState.usingMovingScale,
                        straightLineMode
                ),
                new CompassRadiusMetrics(
                        radiusState.visibleRadiusMeters,
                        radiusState.fullRouteVisibleRadiusMeters,
                        radiusState.movingScaleVisibleRadiusMeters,
                        sanitizedCompassAccuracyMeters,
                        routeThresholdMeters
                ),
                routeGeometry,
                currentLat,
                currentLon,
                alongTrackMeters,
                new CompassDestinationProjection(
                        destinationEastMeters,
                        destinationNorthMeters,
                        destinationReachedRadiusMeters,
                        destinationDistanceMeters <= radiusState.visibleRadiusMeters
                ),
                blockedAreas,
                routeStartApproachProjection,
                orientationCue
        ));
    }

    public static boolean hasReliableMovingSpeed(
            @NonNull NavigationLocation currentLocation,
            float speedMps,
            boolean likelyStationary
    ) {
        return CompassRadiusResolver.hasReliableMovingSpeed(currentLocation, speedMps, likelyStationary);
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

    private static float sanitizeAccuracyMeters(float accuracyMeters) {
        return CompassAccuracyMeters.sanitize(accuracyMeters);
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

    private static double extendFurthestDistanceForBlockedAreas(
            double furthestDistanceMeters,
            @NonNull List<CompassBlockedArea> blockedAreas
    ) {
        double resolved = furthestDistanceMeters;
        for (CompassBlockedArea area : blockedAreas) {
            if (Float.isFinite(area.radiusMeters)
                    && area.radiusMeters > 0f
                    && Float.isFinite(area.eastMeters)
                    && Float.isFinite(area.northMeters)) {
                resolved = Math.max(
                        resolved,
                        Math.hypot(area.eastMeters, area.northMeters) + area.radiusMeters
                );
            }
        }
        return resolved;
    }

}
