package com.vibenavigator.poi;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        try {
            double lat = Double.parseDouble(m.group(1));
            double lon = Double.parseDouble(m.group(2));
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                return null;
            }
            String name = fallbackName != null ? fallbackName : String.format(Locale.US, "%.6f, %.6f", lat, lon);
            return new Poi(name, lat, lon);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
