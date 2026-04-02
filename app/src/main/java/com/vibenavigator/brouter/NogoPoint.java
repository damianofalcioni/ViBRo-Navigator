package com.vibenavigator.brouter;

import androidx.annotation.NonNull;

public final class NogoPoint {
    public final double lat;
    public final double lon;
    public final double radiusMeters;

    public NogoPoint(double lat, double lon, double radiusMeters) {
        this.lat = lat;
        this.lon = lon;
        this.radiusMeters = radiusMeters;
    }

    @NonNull
    @Override
    public String toString() {
        return lat + "," + lon + " r=" + radiusMeters;
    }
}
