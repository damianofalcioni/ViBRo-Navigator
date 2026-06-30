package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

final class SurroundingStreetChunkKey {
    static final double CELL_SIZE_DEGREES = 0.0045d;
    static final double LOAD_RADIUS_METERS = 360.0d;

    private final int latIndex;
    private final int lonIndex;

    private SurroundingStreetChunkKey(int latIndex, int lonIndex) {
        this.latIndex = latIndex;
        this.lonIndex = lonIndex;
    }

    @NonNull
    static SurroundingStreetChunkKey fromIndexes(int latIndex, int lonIndex) {
        return new SurroundingStreetChunkKey(latIndex, lonIndex);
    }

    @NonNull
    static SurroundingStreetChunkKey from(@NonNull LatLon point) {
        return from(point.lat, point.lon);
    }

    @NonNull
    static SurroundingStreetChunkKey from(double latitude, double longitude) {
        return new SurroundingStreetChunkKey(
                (int) Math.floor(latitude / CELL_SIZE_DEGREES),
                (int) Math.floor(longitude / CELL_SIZE_DEGREES)
        );
    }

    @NonNull
    LatLon center() {
        return new LatLon(
                (latIndex + 0.5d) * CELL_SIZE_DEGREES,
                (lonIndex + 0.5d) * CELL_SIZE_DEGREES
        );
    }

    double distanceMetersTo(@NonNull LatLon point) {
        LatLon center = center();
        return GeoMath.distanceMeters(center.lat, center.lon, point.lat, point.lon);
    }

    int latIndex() {
        return latIndex;
    }

    int lonIndex() {
        return lonIndex;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SurroundingStreetChunkKey)) {
            return false;
        }
        SurroundingStreetChunkKey that = (SurroundingStreetChunkKey) other;
        return latIndex == that.latIndex && lonIndex == that.lonIndex;
    }

    @Override
    public int hashCode() {
        int result = latIndex;
        result = 31 * result + lonIndex;
        return result;
    }
}
