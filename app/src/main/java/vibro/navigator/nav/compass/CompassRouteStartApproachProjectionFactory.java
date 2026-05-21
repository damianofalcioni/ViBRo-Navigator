package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

final class CompassRouteStartApproachProjectionFactory {
    private CompassRouteStartApproachProjectionFactory() {
    }

    @Nullable
    static CompassDestinationProjection resolve(
            double currentLat,
            double currentLon,
            @Nullable LatLon target,
            float reachedRadiusMeters
    ) {
        if (target == null) {
            return null;
        }
        float eastMeters = (float) GeoMath.eastMeters(currentLat, currentLon, target.lat, target.lon);
        float northMeters = (float) GeoMath.northMeters(currentLat, target.lat);
        return new CompassDestinationProjection(eastMeters, northMeters, reachedRadiusMeters, true);
    }

    static double extendFurthestDistance(
            double furthestDistanceMeters,
            @Nullable CompassDestinationProjection projection
    ) {
        if (projection == null) {
            return furthestDistanceMeters;
        }
        return Math.max(
                furthestDistanceMeters,
                Math.hypot(projection.eastMeters, projection.northMeters)
        );
    }
}
