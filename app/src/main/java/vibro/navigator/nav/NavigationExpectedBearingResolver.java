package vibro.navigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.PolylineIndex;

final class NavigationExpectedBearingResolver {

    private static final double LOOKAHEAD_METERS = 20.0;
    private static final double MIN_BASELINE_METERS = 3.0;

    private NavigationExpectedBearingResolver() {
    }

    static double resolve(
            @Nullable PolylineIndex polylineIndex,
            @NonNull PolylineIndex.Match match
    ) {
        if (polylineIndex == null) {
            return match.segmentBearingDegrees;
        }
        LatLon current = polylineIndex.pointAtDistance(match.alongTrackMeters);
        if (current == null) {
            return match.segmentBearingDegrees;
        }
        LatLon ahead = polylineIndex.pointAtDistance(lookaheadAlongTrackMeters(polylineIndex, match));
        if (ahead == null || baselineMeters(current, ahead) < MIN_BASELINE_METERS) {
            return match.segmentBearingDegrees;
        }
        return GeoMath.bearingDegrees(current.lat, current.lon, ahead.lat, ahead.lon);
    }

    private static double lookaheadAlongTrackMeters(
            @NonNull PolylineIndex polylineIndex,
            @NonNull PolylineIndex.Match match
    ) {
        return Math.min(polylineIndex.totalLengthMeters(), match.alongTrackMeters + LOOKAHEAD_METERS);
    }

    private static double baselineMeters(@NonNull LatLon current, @NonNull LatLon ahead) {
        return GeoMath.distanceMeters(current.lat, current.lon, ahead.lat, ahead.lon);
    }
}
