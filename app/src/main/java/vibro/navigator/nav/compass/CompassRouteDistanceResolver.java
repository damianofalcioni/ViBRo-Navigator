package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

final class CompassRouteDistanceResolver {
    private CompassRouteDistanceResolver() {
    }

    static double furthestSampleDistanceMeters(
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLat,
            double currentLon
    ) {
        return Math.max(
                furthestActiveRouteDistanceMeters(routeGeometry, currentLat, currentLon),
                furthestArchivedPassedRouteDistanceMeters(routeGeometry, currentLat, currentLon)
        );
    }

    private static double furthestActiveRouteDistanceMeters(
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLat,
            double currentLon
    ) {
        double furthestDistanceMeters = 0.0;
        for (int i = 0; i < routeGeometry.routeSamplePointCount(); i++) {
            furthestDistanceMeters = Math.max(
                    furthestDistanceMeters,
                    distanceFromCurrentMeters(routeGeometry.routeSamplePointAt(i), currentLat, currentLon)
            );
        }
        return furthestDistanceMeters;
    }

    private static double furthestArchivedPassedRouteDistanceMeters(
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLat,
            double currentLon
    ) {
        return Math.max(
                furthestSegmentCollectionDistanceMeters(
                        routeGeometry.archivedPassedRouteSegments(),
                        currentLat,
                        currentLon
                ),
                furthestSegmentCollectionDistanceMeters(
                        routeGeometry.recalculationBridgeSegments(),
                        currentLat,
                        currentLon
                )
        );
    }

    private static double furthestSegmentCollectionDistanceMeters(
            @NonNull CompassPassedRouteSegments archivedSegments,
            double currentLat,
            double currentLon
    ) {
        double furthestDistanceMeters = 0.0;
        for (int segmentIndex = 0; segmentIndex < archivedSegments.segmentCount(); segmentIndex++) {
            furthestDistanceMeters = Math.max(
                    furthestDistanceMeters,
                    furthestArchivedSegmentDistanceMeters(archivedSegments, segmentIndex, currentLat, currentLon)
            );
        }
        return furthestDistanceMeters;
    }

    private static double furthestArchivedSegmentDistanceMeters(
            @NonNull CompassPassedRouteSegments archivedSegments,
            int segmentIndex,
            double currentLat,
            double currentLon
    ) {
        double furthestDistanceMeters = 0.0;
        for (int i = 0; i < archivedSegments.samplePointCount(segmentIndex); i++) {
            furthestDistanceMeters = Math.max(
                    furthestDistanceMeters,
                    distanceFromCurrentMeters(archivedSegments.samplePointAt(segmentIndex, i), currentLat, currentLon)
            );
        }
        return furthestDistanceMeters;
    }

    private static double distanceFromCurrentMeters(
            @Nullable LatLon point,
            double currentLat,
            double currentLon
    ) {
        if (point == null) {
            return 0.0;
        }
        double eastMeters = GeoMath.eastMeters(currentLat, currentLon, point.lat, point.lon);
        double northMeters = GeoMath.northMeters(currentLat, point.lat);
        return Math.hypot(eastMeters, northMeters);
    }
}
