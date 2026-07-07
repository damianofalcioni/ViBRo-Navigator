package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.route.PolylineIndex;

final class CompassPassedRouteArchive {
    @NonNull
    private final List<List<LatLon>> segments = new ArrayList<>();

    void reset() {
        segments.clear();
    }

    void archive(
            @Nullable CompassRouteGeometry routeGeometry,
            @Nullable PolylineIndex.Match previousRouteMatch,
            int fallbackPassedRouteSamplePointCount
    ) {
        if (routeGeometry == null) {
            return;
        }
        List<LatLon> passedSegment = routeGeometry.copyRouteSamplePointsUntil(
                resolvePassedRouteSamplePointCount(
                        routeGeometry,
                        previousRouteMatch,
                        fallbackPassedRouteSamplePointCount
                )
        );
        if (passedSegment.size() >= 2) {
            segments.add(passedSegment);
        }
    }

    @NonNull
    List<List<LatLon>> segments() {
        return segments;
    }

    private static int resolvePassedRouteSamplePointCount(
            @NonNull CompassRouteGeometry routeGeometry,
            @Nullable PolylineIndex.Match previousRouteMatch,
            int fallbackPassedRouteSamplePointCount
    ) {
        if (previousRouteMatch == null) {
            return fallbackPassedRouteSamplePointCount;
        }
        return Math.max(
                fallbackPassedRouteSamplePointCount,
                routeGeometry.passedRoutePointCount(previousRouteMatch.alongTrackMeters)
        );
    }
}
