package vibro.navigator.geo;

import androidx.annotation.NonNull;

public final class LatLon {
    public final double lat;
    public final double lon;

    public LatLon(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    @NonNull
    @Override
    public String toString() {
        return lat + "," + lon;
    }
}
