package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

import java.util.AbstractList;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

final class ProjectedCompassRoutePointList extends AbstractList<CompassRoutePoint> {
    @NonNull
    private final CompassRouteGeometry routeGeometry;
    private final double currentLatitude;
    private final double currentLongitude;
    private final int startIndex;
    private final int endIndex;
    private final boolean hintPoints;

    ProjectedCompassRoutePointList(
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLatitude,
            double currentLongitude,
            int startIndex,
            int endIndex,
            boolean hintPoints
    ) {
        this.routeGeometry = routeGeometry;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.startIndex = startIndex;
        this.endIndex = Math.max(startIndex, endIndex);
        this.hintPoints = hintPoints;
    }

    @NonNull
    @Override
    public CompassRoutePoint get(int index) {
        int absoluteIndex = startIndex + index;
        LatLon point = hintPoints
                ? routeGeometry.hintSamplePointAt(absoluteIndex)
                : routeGeometry.routeSamplePointAt(absoluteIndex);
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
        return endIndex - startIndex;
    }
}
