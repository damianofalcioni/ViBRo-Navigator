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

    public static double angularDiffDegrees(double a, double b) {
        double d = ((a - b + 540.0) % 360.0) - 180.0;
        return Math.abs(d);
    }

    public static double eastMeters(double refLat, double refLon, double lat, double lon) {
        double refLatRad = Math.toRadians(refLat);
        return (lon - refLon) * 111320.0 * Math.cos(refLatRad);
    }

    public static double northMeters(double refLat, double lat) {
        return (lat - refLat) * 111320.0;
    }
}
