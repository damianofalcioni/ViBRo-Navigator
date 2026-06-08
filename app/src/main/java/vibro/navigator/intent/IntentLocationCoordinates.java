package vibro.navigator.intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLonTextParser;


final class IntentLocationCoordinates {

    private IntentLocationCoordinates() {
    }

    @Nullable
    static String extract(@Nullable String text) {
        if (text == null) {
            return null;
        }
        return coordinateText(LatLonTextParser.find(IntentUriDecoder.decodeComponent(text)));
    }

    @Nullable
    static String extractDecoded(@Nullable String text) {
        return coordinateText(LatLonTextParser.find(text));
    }

    @Nullable
    static String extractAtCoordinates(@NonNull String text) {
        return coordinateText(LatLonTextParser.findAtCoordinates(text));
    }

    static boolean looksLikeNumericCoordinates(@NonNull String value) {
        return LatLonTextParser.looksLikeNumericCoordinates(value);
    }

    @Nullable
    private static String coordinateText(@Nullable LatLonTextParser.Match match) {
        return match == null ? null : match.coordinateText();
    }
}
