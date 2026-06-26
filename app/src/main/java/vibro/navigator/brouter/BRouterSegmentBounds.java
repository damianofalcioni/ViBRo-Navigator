package vibro.navigator.brouter;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;

final class BRouterSegmentBounds {
    private static final double METERS_PER_DEGREE = 111_320.0d;

    final double minLat;
    final double maxLat;
    final double minLon;
    final double maxLon;
    final int minIntegerLon;
    final int maxIntegerLon;
    final int minIntegerLat;
    final int maxIntegerLat;

    private BRouterSegmentBounds(double minLat, double maxLat, double minLon, double maxLon) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        minIntegerLon = BRouterSegmentTile.integerLon(minLon);
        maxIntegerLon = BRouterSegmentTile.integerLon(maxLon);
        minIntegerLat = BRouterSegmentTile.integerLat(minLat);
        maxIntegerLat = BRouterSegmentTile.integerLat(maxLat);
    }

    @NonNull
    static BRouterSegmentBounds around(double lat, double lon, double radiusMeters) {
        double latDelta = radiusMeters / METERS_PER_DEGREE;
        double cosLat = Math.cos(Math.toRadians(lat));
        double lonMetersPerDegree = Math.max(1_000.0d, METERS_PER_DEGREE * Math.abs(cosLat));
        double lonDelta = radiusMeters / lonMetersPerDegree;
        return new BRouterSegmentBounds(lat - latDelta, lat + latDelta, lon - lonDelta, lon + lonDelta);
    }

    boolean intersects(@NonNull Iterable<LatLon> points) {
        for (LatLon point : points) {
            if (point.lat >= minLat && point.lat <= maxLat && point.lon >= minLon && point.lon <= maxLon) {
                return true;
            }
        }
        return false;
    }
}
