package vibro.navigator.poi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

import java.util.Locale;

public final class Poi {
    @NonNull
    public final String name;
    public final double lat;
    public final double lon;
    @Nullable
    private final PoiDetails details;

    public Poi(@NonNull String name, double lat, double lon) {
        this(name, lat, lon, null);
    }

    public Poi(@NonNull String name, double lat, double lon, @Nullable PoiDetails details) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.details = details;
    }

    public boolean hasValidCoordinates() {
        return LatLon.isValidCoordinate(lat, lon);
    }

    @NonNull
    public String displayLabel() {
        String trimmed = name.trim();
        if (!trimmed.isEmpty()) {
            return trimmed;
        }
        return String.format(Locale.US, "%.6f, %.6f", lat, lon);
    }

    @NonNull
    public String stableKey() {
        return String.format(Locale.US, "%.6f,%.6f", lat, lon);
    }

    @Nullable
    public PoiDetails details() {
        return details;
    }
}
