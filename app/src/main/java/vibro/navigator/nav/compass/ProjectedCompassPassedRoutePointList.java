package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

import java.util.AbstractList;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

final class ProjectedCompassPassedRoutePointList extends AbstractList<CompassRoutePoint> {
    @NonNull
    private final CompassRouteGeometry routeGeometry;
    @NonNull
    private final CompassPassedRouteSegments archivedSegments;
    private final double currentLatitude;
    private final double currentLongitude;
    private final int activePassedRouteSamplePointCount;

    ProjectedCompassPassedRoutePointList(
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLatitude,
            double currentLongitude,
            int activePassedRouteSamplePointCount
    ) {
        this.routeGeometry = routeGeometry;
        this.archivedSegments = routeGeometry.archivedPassedRouteSegments();
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.activePassedRouteSamplePointCount = Math.max(0, activePassedRouteSamplePointCount);
    }

    @NonNull
    @Override
    public CompassRoutePoint get(int index) {
        LatLon point = pointAt(index);
        if (point == null) {
            throw new IndexOutOfBoundsException("index=" + index);
        }
        return new CompassRoutePoint(
                (float) GeoMath.eastMeters(currentLatitude, currentLongitude, point.lat, point.lon),
                (float) GeoMath.northMeters(currentLatitude, point.lat)
        );
    }

    @Override
    public int size() {
        return archivedSegments.totalSamplePointCount() + activePassedRouteSamplePointCount;
    }

    private LatLon pointAt(int index) {
        int activeRouteIndex = index - archivedSegments.totalSamplePointCount();
        if (activeRouteIndex >= 0) {
            return routeGeometry.routeSamplePointAt(activeRouteIndex);
        }
        return archivedPointAt(index);
    }

    private LatLon archivedPointAt(int index) {
        int remainingIndex = index;
        for (int segmentIndex = 0; segmentIndex < archivedSegments.segmentCount(); segmentIndex++) {
            int segmentSize = archivedSegments.samplePointCount(segmentIndex);
            if (remainingIndex < segmentSize) {
                return archivedSegments.samplePointAt(segmentIndex, remainingIndex);
            }
            remainingIndex -= segmentSize;
        }
        return null;
    }
}
