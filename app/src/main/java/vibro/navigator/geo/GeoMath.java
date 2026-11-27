package vibro.navigator.geo;

public final class GeoMath {
    private static final double EARTH_RADIUS_M = 6371000.0;

    private GeoMath() {
    }

    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double boundedA = Math.max(0.0, Math.min(1.0, a));
        double c = 2 * Math.atan2(Math.sqrt(boundedA), Math.sqrt(1 - boundedA));
        return EARTH_RADIUS_M * c;
    }

    public static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);

        double y = Math.sin(dLon) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));
        brng = (brng + 360.0) % 360.0;
        return brng;
    }

    /** Returns the equivalent longitude in [-180, 180) for positions and signed offsets. */
    public static double normalizeLongitude(double longitude) {
        double wrapped = longitude % 360.0;
        if (wrapped >= 180.0) {
            return wrapped - 360.0;
        }
        return wrapped < -180.0 ? wrapped + 360.0 : wrapped;
    }

    public static LatLon destinationPoint(LatLon start, double bearingDegrees, double distanceMeters) {
        double bearing = Math.toRadians(bearingDegrees);
        double angle = distanceMeters / EARTH_RADIUS_M;
        double lat = Math.toRadians(start.lat);
        double sinLat = Math.sin(lat) * Math.cos(angle) + Math.cos(lat) * Math.sin(angle) * Math.cos(bearing);
        double endLat = Math.asin(Math.max(-1.0, Math.min(1.0, sinLat)));
        double lonOffset = Math.atan2(Math.sin(bearing) * Math.sin(angle) * Math.cos(lat),
                Math.cos(angle) - Math.sin(lat) * Math.sin(endLat));
        return new LatLon(Math.toDegrees(endLat), normalizeLongitude(start.lon + Math.toDegrees(lonOffset)));
    }

    public static double angularDiffDegrees(double a, double b) {
        double d = ((a - b + 540.0) % 360.0) - 180.0;
        return Math.abs(d);
    }

    public static double eastMeters(double refLat, double refLon, double lat, double lon) {
        double refLatRad = Math.toRadians(refLat);
        return normalizeLongitude(lon - refLon) * 111320.0 * Math.cos(refLatRad);
    }

    public static double northMeters(double refLat, double lat) {
        return (lat - refLat) * 111320.0;
    }
}
