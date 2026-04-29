package vibro.navigator.nav;

import android.content.Context;
import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

final class NavCompassStateFactory {
    private static final float COMPASS_MOVING_LOOKAHEAD_SECONDS = 60f;
    private static final float COMPASS_MIN_VISIBLE_RADIUS_METERS = 90f;
    private static final long COMPASS_RADIUS_SMOOTHING_TIME_CONSTANT_MS = 450L;

    private NavCompassStateFactory() {
    }

    @NonNull
    static CompassRouteGeometry buildCompassRouteGeometry(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index
    ) {
        return CompassRouteGeometryFactory.build(route, index);
    }

    @NonNull
    static String buildGpsStatusLine(
            float speedMps,
            @Nullable Location currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @NonNull Context context
    ) {
        return NavigationTextFormatter.formatGpsStatus(
                context,
                speedMps,
                elevationMeters(currentLocation),
                accuracyMeters,
                bearingDegrees(currentLocation),
                bearingAccuracyDegrees(currentLocation),
                fixedSatelliteCount
        );
    }

    @Nullable
    private static Double elevationMeters(@Nullable Location currentLocation) {
        return currentLocation != null && currentLocation.hasAltitude()
                ? currentLocation.getAltitude()
                : null;
    }

    @Nullable
    private static Float bearingDegrees(@Nullable Location currentLocation) {
        return currentLocation != null && currentLocation.hasBearing()
                ? currentLocation.getBearing()
                : null;
    }

    @Nullable
    private static Float bearingAccuracyDegrees(@Nullable Location currentLocation) {
        return currentLocation != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && currentLocation.hasBearingAccuracy()
                ? currentLocation.getBearingAccuracyDegrees()
                : null;
    }

    @Nullable
    static NavCompassState buildCompassState(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            @NonNull Location currentLocation,
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
        double furthestDistanceMeters = Math.max(
                resolveFurthestRouteSampleDistanceMeters(routeGeometry, currentLat, currentLon),
                destinationDistanceMeters
        );
        CompassRadiusState radiusState = resolveCompassRadiusState(
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
        float sixtySecondReferenceSpeedMps = resolveMovingLegendReferenceSpeedMps(
                radiusState.sixtySecondVisibleRadiusMeters
        );
        float referenceSpeedMps = radiusState.usingMovingScale
                ? resolveMovingLegendReferenceSpeedMps(radiusState.visibleRadiusMeters)
                : fullRouteReferenceSpeedMps;
        float routeThresholdMeters =
                (float) RouteDeviationPolicy.resolveOffTrackThresholdMeters(compassAccuracyMeters);
        float resolvedHeading = normalizeHeading(headingDegrees == null ? 0.0 : headingDegrees);
        return new NavCompassState(
                resolvedHeading,
                sanitizeHeadingAccuracyDegrees(headingAccuracyDegrees),
                referenceSpeedMps,
                fullRouteReferenceSpeedMps,
                sixtySecondReferenceSpeedMps,
                radiusState.visibleRadiusMeters,
                radiusState.fullRouteVisibleRadiusMeters,
                radiusState.sixtySecondVisibleRadiusMeters,
                sanitizeAccuracyMeters(compassAccuracyMeters),
                radiusState.usingMovingScale,
                routeThresholdMeters,
                routeGeometry,
                currentLat,
                currentLon,
                routeGeometry.passedRoutePointCount(alongTrackMeters),
                destinationEastMeters,
                destinationNorthMeters,
                destinationDistanceMeters <= radiusState.visibleRadiusMeters
        );
    }

    static boolean hasReliableMovingSpeed(
            @NonNull Location currentLocation,
            boolean likelyStationary
    ) {
        return !likelyStationary
                && currentLocation.hasSpeed()
                && Float.isFinite(currentLocation.getSpeed())
                && currentLocation.getSpeed() > 0f;
    }

    static float smoothVisibleRadiusMeters(
            float targetVisibleRadiusMeters,
            @Nullable Float previousVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs
    ) {
        if (previousVisibleRadiusMeters == null
                || !Float.isFinite(previousVisibleRadiusMeters)
                || previousVisibleRadiusMeters <= 0f
                || compassRadiusUpdateDeltaMs <= 0L) {
            return targetVisibleRadiusMeters;
        }
        long boundedDeltaMs = Math.min(compassRadiusUpdateDeltaMs, 5_000L);
        double alpha = 1.0 - Math.exp(-boundedDeltaMs / (double) COMPASS_RADIUS_SMOOTHING_TIME_CONSTANT_MS);
        return (float) (previousVisibleRadiusMeters
                + (targetVisibleRadiusMeters - previousVisibleRadiusMeters) * alpha);
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

    @NonNull
    private static CompassRadiusState resolveCompassRadiusState(
            double furthestDistanceMeters,
            @NonNull Location currentLocation,
            float speedMps,
            boolean likelyStationary,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            @Nullable CompassRadiusTransition compassRadiusTransition,
            long nowMs
    ) {
        float fullRouteVisibleRadiusMeters = (float) Math.max(
                COMPASS_MIN_VISIBLE_RADIUS_METERS,
                furthestDistanceMeters * 1.15
        );
        boolean reliableMovingSpeed = hasReliableMovingSpeed(currentLocation, likelyStationary);
        boolean hasReusableMovingRadius = isReusableMovingRadius(previousReliableMovingCompassVisibleRadiusMeters);
        float targetVisibleRadiusMeters = resolveTargetVisibleRadiusMeters(
                fullRouteVisibleRadiusMeters,
                speedMps,
                likelyStationary,
                reliableMovingSpeed,
                previousReliableMovingCompassVisibleRadiusMeters,
                hasReusableMovingRadius
        );
        boolean fullRouteOverview = likelyStationary
                || (!reliableMovingSpeed && !hasReusableMovingRadius);
        float visibleRadiusMeters = resolveVisibleRadiusMeters(
                fullRouteVisibleRadiusMeters,
                targetVisibleRadiusMeters,
                reliableMovingSpeed,
                hasReusableMovingRadius,
                previousCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                compassRadiusTransition,
                fullRouteOverview,
                nowMs
        );
        return new CompassRadiusState(
                fullRouteVisibleRadiusMeters,
                visibleRadiusMeters,
                resolveSixtySecondVisibleRadiusMeters(
                        fullRouteVisibleRadiusMeters,
                        speedMps,
                        previousReliableMovingCompassVisibleRadiusMeters,
                        hasReusableMovingRadius
                ),
                !likelyStationary && (reliableMovingSpeed || hasReusableMovingRadius)
        );
    }

    private static boolean isReusableMovingRadius(@Nullable Float radiusMeters) {
        return radiusMeters != null && Float.isFinite(radiusMeters) && radiusMeters > 0f;
    }

    private static float resolveTargetVisibleRadiusMeters(
            float fullRouteVisibleRadiusMeters,
            float speedMps,
            boolean likelyStationary,
            boolean reliableMovingSpeed,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            boolean hasReusableMovingRadius
    ) {
        if (likelyStationary) {
            return fullRouteVisibleRadiusMeters;
        }
        if (reliableMovingSpeed) {
            return Math.min(fullRouteVisibleRadiusMeters, resolveMovingVisibleRadiusMeters(speedMps));
        }
        if (hasReusableMovingRadius) {
            return Math.min(fullRouteVisibleRadiusMeters, previousReliableMovingCompassVisibleRadiusMeters);
        }
        return fullRouteVisibleRadiusMeters;
    }

    private static float resolveSixtySecondVisibleRadiusMeters(
            float fullRouteVisibleRadiusMeters,
            float speedMps,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            boolean hasReusableMovingRadius
    ) {
        float movingRadiusMeters = hasReusableMovingRadius
                ? previousReliableMovingCompassVisibleRadiusMeters
                : resolveMovingVisibleRadiusMeters(speedMps);
        return Math.min(fullRouteVisibleRadiusMeters, movingRadiusMeters);
    }

    private static float resolveVisibleRadiusMeters(
            float fullRouteVisibleRadiusMeters,
            float targetVisibleRadiusMeters,
            boolean reliableMovingSpeed,
            boolean hasReusableMovingRadius,
            @Nullable Float previousCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            @Nullable CompassRadiusTransition compassRadiusTransition,
            boolean fullRouteOverview,
            long nowMs
    ) {
        if (fullRouteOverview) {
            return resolveFullRouteOverviewRadius(
                    fullRouteVisibleRadiusMeters,
                    previousCompassVisibleRadiusMeters,
                    compassRadiusTransition,
                    nowMs
            );
        }
        if (compassRadiusTransition != null) {
            compassRadiusTransition.reset();
        }
        if (hasReusableMovingRadius && !reliableMovingSpeed) {
            return targetVisibleRadiusMeters;
        }
        return smoothVisibleRadiusMeters(
                targetVisibleRadiusMeters,
                previousCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs
        );
    }

    private static float resolveFullRouteOverviewRadius(
            float fullRouteVisibleRadiusMeters,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable CompassRadiusTransition compassRadiusTransition,
            long nowMs
    ) {
        if (compassRadiusTransition == null) {
            return fullRouteVisibleRadiusMeters;
        }
        return compassRadiusTransition.resolve(
                previousCompassVisibleRadiusMeters != null
                        ? previousCompassVisibleRadiusMeters
                        : fullRouteVisibleRadiusMeters,
                fullRouteVisibleRadiusMeters,
                isReusableMovingRadius(previousCompassVisibleRadiusMeters),
                nowMs
        );
    }

    private static float resolveMovingVisibleRadiusMeters(float speedMps) {
        float safeSpeedMps = Float.isFinite(speedMps) && speedMps > 0f ? speedMps : 0f;
        float targetRadiusMeters = safeSpeedMps * COMPASS_MOVING_LOOKAHEAD_SECONDS;
        return Math.max(COMPASS_MIN_VISIBLE_RADIUS_METERS, targetRadiusMeters);
    }

    private static float resolveMovingLegendReferenceSpeedMps(float visibleRadiusMeters) {
        float safeRadiusMeters = Float.isFinite(visibleRadiusMeters) && visibleRadiusMeters > 0f
                ? visibleRadiusMeters
                : COMPASS_MIN_VISIBLE_RADIUS_METERS;
        return Math.max(1f, safeRadiusMeters / COMPASS_MOVING_LOOKAHEAD_SECONDS);
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

    private static final class CompassRadiusState {
        final float fullRouteVisibleRadiusMeters;
        final float visibleRadiusMeters;
        final float sixtySecondVisibleRadiusMeters;
        final boolean usingMovingScale;

        CompassRadiusState(
                float fullRouteVisibleRadiusMeters,
                float visibleRadiusMeters,
                float sixtySecondVisibleRadiusMeters,
                boolean usingMovingScale
        ) {
            this.fullRouteVisibleRadiusMeters = fullRouteVisibleRadiusMeters;
            this.visibleRadiusMeters = visibleRadiusMeters;
            this.sixtySecondVisibleRadiusMeters = sixtySecondVisibleRadiusMeters;
            this.usingMovingScale = usingMovingScale;
        }
    }
}
