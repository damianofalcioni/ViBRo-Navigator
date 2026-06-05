package vibro.navigator.geo;

import androidx.annotation.NonNull;

public final class LatLon {
    public final double lat;
    public final double lon;

    public LatLon(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public boolean isValid() {
        return isValidCoordinate(lat, lon);
    }

    public static boolean isValidCoordinate(double lat, double lon) {
        return Double.isFinite(lat)
                && Double.isFinite(lon)
                && lat >= -90.0
                && lat <= 90.0
                && lon >= -180.0
                && lon <= 180.0;
    }

    @NonNull
    @Override
    public String toString() {
        return lat + "," + lon;
    }
}
