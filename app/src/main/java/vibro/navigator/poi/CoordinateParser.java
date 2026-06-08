package vibro.navigator.poi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.geo.LatLonTextParser;

import java.util.Locale;

public final class CoordinateParser {

    private CoordinateParser() {
    }

    @Nullable
    public static Poi tryParse(@Nullable String text, @Nullable String fallbackName) {
        if (text == null) {
            return null;
        }
        LatLonTextParser.Match match = LatLonTextParser.find(text);
        if (match == null) {
            return null;
        }
        LatLon latLon = match.latLon;
        return new Poi(displayName(latLon.lat, latLon.lon, fallbackName), latLon.lat, latLon.lon);
    }

    @NonNull
    private static String displayName(double lat, double lon, @Nullable String fallbackName) {
        if (fallbackName != null) {
            return fallbackName;
        }
        return String.format(Locale.US, "%.6f, %.6f", lat, lon);
    }
}
