package vibro.navigator.geo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LatLonTextParser {

    private static final String LAT_DECIMAL_NUMBER = "[+-]?\\d{1,2}(?:\\.\\d+)?";
    private static final String LON_DECIMAL_NUMBER = "[+-]?\\d{1,3}(?:\\.\\d+)?";
    private static final Pattern LAT_LON = Pattern.compile(
            "(?<![\\d.])(" + LAT_DECIMAL_NUMBER + ")\\s*,\\s*("
                    + LON_DECIMAL_NUMBER + ")(?![\\d.])"
    );
    private static final Pattern AT_LAT_LON = Pattern.compile(
            "@\\s*(" + LAT_DECIMAL_NUMBER + ")\\s*,\\s*("
                    + LON_DECIMAL_NUMBER + ")"
    );

    private LatLonTextParser() {
    }

    @Nullable
    public static Match find(@Nullable String text) {
        if (text == null) {
            return null;
        }
        return match(LAT_LON.matcher(text));
    }

    @Nullable
    public static Match findAtCoordinates(@NonNull String text) {
        return match(AT_LAT_LON.matcher(text));
    }

    public static boolean looksLikeNumericCoordinates(@NonNull String value) {
        return value.indexOf(',') >= 0 && value.matches("[()\\s+\\-\\d.,]+");
    }

    @Nullable
    private static Match match(@NonNull Matcher matcher) {
        if (!matcher.find()) {
            return null;
        }
        return normalize(matcher.group(1), matcher.group(2));
    }

    @Nullable
    private static Match normalize(@NonNull String latString, @NonNull String lonString) {
        try {
            double lat = Double.parseDouble(latString);
            double lon = Double.parseDouble(lonString);
            if (!LatLon.isValidCoordinate(lat, lon)) {
                return null;
            }
            return new Match(latString, lonString, new LatLon(lat, lon));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static final class Match {
        @NonNull
        public final String latText;
        @NonNull
        public final String lonText;
        @NonNull
        public final LatLon latLon;

        private Match(
                @NonNull String latText,
                @NonNull String lonText,
                @NonNull LatLon latLon
        ) {
            this.latText = latText;
            this.lonText = lonText;
            this.latLon = latLon;
        }

        @NonNull
        public String coordinateText() {
            return latText + "," + lonText;
        }
    }
}
