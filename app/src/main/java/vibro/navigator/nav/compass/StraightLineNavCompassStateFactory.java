package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.policy.NavigationSpeedBucket;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

public final class StraightLineNavCompassStateFactory {
    private StraightLineNavCompassStateFactory() {
    }

    @Nullable
    public static NavCompassState buildTargetCompassState(
            @NonNull NavigationLocation currentLocation,
            float speedMps,
            boolean likelyStationary,
            float compassAccuracyMeters,
            @NonNull LatLon target,
            @NonNull List<LatLon> remainingTargetsAfterNext,
            @NonNull List<LatLon> intermediateMarkers,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            long nowMs
    ) {
        return buildTargetCompassState(
                currentLocation,
                speedMps,
                likelyStationary,
                compassAccuracyMeters,
                target,
                remainingTargetsAfterNext,
                intermediateMarkers,
                headingDegrees,
                headingAccuracyDegrees,
                null,
                null,
                null,
                0L,
                nowMs
        );
    }

    @Nullable
    public static NavCompassState buildTargetCompassState(
            @NonNull NavigationLocation currentLocation,
            float speedMps,
            boolean likelyStationary,
            float compassAccuracyMeters,
            @NonNull LatLon target,
            @NonNull List<LatLon> remainingTargetsAfterNext,
            @NonNull List<LatLon> intermediateMarkers,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousVisibleRadiusMeters,
            @Nullable Float previousReliableMovingVisibleRadiusMeters,
            @Nullable NavigationSpeedBucket previousMovingSpeedBucket,
            long radiusUpdateDeltaMs,
            long nowMs
    ) {
        List<LatLon> track = buildTrack(currentLocation, target, remainingTargetsAfterNext);
        GeoJsonRoute route = new GeoJsonRoute(track, Collections.emptyList(), 0.0, 0.0);
        PolylineIndex index = new PolylineIndex(track);
        return NavCompassStateFactory.buildCompassState(
                route,
                index,
                0.0,
                currentLocation,
                speedMps,
                likelyStationary,
                compassAccuracyMeters,
                (float) NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(compassAccuracyMeters),
                headingDegrees,
                headingAccuracyDegrees,
                previousVisibleRadiusMeters,
                previousReliableMovingVisibleRadiusMeters,
                previousMovingSpeedBucket,
                radiusUpdateDeltaMs,
                buildGeometry(track, index, intermediateMarkers),
                null,
                orientationCue(currentLocation, target),
                null,
                Collections.emptyList(),
                nowMs,
                true
        );
    }

    @NonNull
    private static List<LatLon> buildTrack(
            @NonNull NavigationLocation currentLocation,
            @NonNull LatLon target,
            @NonNull List<LatLon> remainingTargetsAfterNext
    ) {
        List<LatLon> track = new ArrayList<>();
        track.add(new LatLon(currentLocation.getLatitude(), currentLocation.getLongitude()));
        track.add(target);
        track.addAll(remainingTargetsAfterNext);
        return track;
    }

    @NonNull
    private static CompassRouteGeometry buildGeometry(
            @NonNull List<LatLon> track,
            @NonNull PolylineIndex index,
            @NonNull List<LatLon> intermediateMarkers
    ) {
        return new CompassRouteGeometry(
                buildSamplePoints(track, index),
                Collections.emptyList(),
                intermediateMarkers
        );
    }

    @NonNull
    private static List<CompassRouteGeometry.SamplePoint> buildSamplePoints(
            @NonNull List<LatLon> track,
            @NonNull PolylineIndex index
    ) {
        List<CompassRouteGeometry.SamplePoint> samplePoints = new ArrayList<>();
        for (int i = 0; i < track.size(); i++) {
            samplePoints.add(new CompassRouteGeometry.SamplePoint(
                    track.get(i),
                    index.distanceAtPointIndex(i)
            ));
        }
        return samplePoints;
    }

    @NonNull
    private static CompassOrientationCue orientationCue(
            @NonNull NavigationLocation currentLocation,
            @NonNull LatLon target
    ) {
        return new CompassOrientationCue((float) GeoMath.bearingDegrees(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                target.lat,
                target.lon
        ));
    }
}
