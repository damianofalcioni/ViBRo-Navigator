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
    @NonNull
    private final List<List<LatLon>> bridgeSegments = new ArrayList<>();

    void reset() {
        segments.clear();
        bridgeSegments.clear();
    }

    void archive(
            @Nullable CompassRouteGeometry routeGeometry,
            @Nullable PolylineIndex.Match previousRouteMatch,
            int fallbackPassedRouteSamplePointCount,
            @Nullable LatLon replacementRouteStart
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
            appendBridge(lastPoint(passedSegment), replacementRouteStart);
        }
    }

    @NonNull
    List<List<LatLon>> segments() {
        return segments;
    }

    @NonNull
    List<List<LatLon>> bridgeSegments() {
        return bridgeSegments;
    }

    private void appendBridge(@NonNull LatLon from, @Nullable LatLon to) {
        List<LatLon> bridge = RouteRecalculationBridge.segment(from, to);
        if (!bridge.isEmpty()) {
            bridgeSegments.add(bridge);
        }
    }

    @NonNull
    private static LatLon lastPoint(@NonNull List<LatLon> points) {
        return points.get(points.size() - 1);
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
