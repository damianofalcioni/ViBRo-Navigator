package vibro.navigator.intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IntentLocationCoordinates {

    private static final Pattern COORDS = Pattern.compile("(?<![\\d.])(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)(?![\\d.])");
    private static final Pattern AT_COORDS = Pattern.compile("@\\s*(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)");

    private IntentLocationCoordinates() {
    }

    @Nullable
    static String extract(@Nullable String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = COORDS.matcher(IntentUriDecoder.decodeComponent(text));
        if (!matcher.find()) {
            return null;
        }
        return normalize(matcher.group(1), matcher.group(2));
    }

    @Nullable
    static String extractAtCoordinates(@NonNull String text) {
        Matcher matcher = AT_COORDS.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return normalize(matcher.group(1), matcher.group(2));
    }

    static boolean looksLikeNumericCoordinates(@NonNull String value) {
        return value.indexOf(',') >= 0 && value.matches("[()\\s+\\-\\d.,]+");
    }

    @Nullable
    private static String normalize(@NonNull String latString, @NonNull String lonString) {
        try {
            double lat = Double.parseDouble(latString);
            double lon = Double.parseDouble(lonString);
            if (!LatLon.isValidCoordinate(lat, lon)) {
                return null;
            }
            return latString + "," + lonString;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
