package vibro.navigator.poi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vibro.navigator.geo.LatLon;

public final class CoordinateParser {

    private static final Pattern LAT_LON = Pattern.compile(
            "(?<![\\d.])(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)(?![\\d.])"
    );

    private CoordinateParser() {
    }

    @Nullable
    public static Poi tryParse(@Nullable String text, @Nullable String fallbackName) {
        if (text == null) {
            return null;
        }
        Matcher m = LAT_LON.matcher(text);
        if (!m.find()) {
            return null;
        }
        return parseMatch(m, fallbackName);
    }

    @Nullable
    private static Poi parseMatch(@NonNull Matcher m, @Nullable String fallbackName) {
        try {
            double lat = Double.parseDouble(m.group(1));
            double lon = Double.parseDouble(m.group(2));
            if (!LatLon.isValidCoordinate(lat, lon)) {
                return null;
            }
            return new Poi(displayName(lat, lon, fallbackName), lat, lon);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @NonNull
    private static String displayName(double lat, double lon, @Nullable String fallbackName) {
        if (fallbackName != null) {
            return fallbackName;
        }
        return String.format(Locale.US, "%.6f, %.6f", lat, lon);
    }
}
