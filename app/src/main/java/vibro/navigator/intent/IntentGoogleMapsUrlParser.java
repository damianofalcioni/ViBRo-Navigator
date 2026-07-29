package vibro.navigator.intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IntentGoogleMapsUrlParser {

    private static final Pattern GOOGLE_HOST = Pattern.compile(
            "(?:www\\.)?google\\.[a-z]{2,3}(?:\\.[a-z]{2})?"
    );
    private static final Pattern MAPS_GOOGLE_HOST = Pattern.compile(
            "maps\\.google\\.[a-z]{2,3}(?:\\.[a-z]{2})?"
    );
    private static final String LAT_DECIMAL_NUMBER = "[+-]?\\d{1,2}(?:\\.\\d+)?";
    private static final String LON_DECIMAL_NUMBER = "[+-]?\\d{1,3}(?:\\.\\d+)?";
    private static final Pattern DATA_COORDINATES = Pattern.compile(
            "(?:^|!)3d(" + LAT_DECIMAL_NUMBER + ")!4d("
                    + LON_DECIMAL_NUMBER + ")(?=$|[!&?#])"
    );

    private IntentGoogleMapsUrlParser() {
    }

    static boolean isMapUrl(@Nullable String host, @NonNull String path) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        if (isMapsGoogleHost(host)) {
            return true;
        }
        return isGoogleHost(host)
                && (path.equals("/maps") || path.startsWith("/maps/"));
    }

    private static boolean isMapsGoogleHost(@NonNull String host) {
        return MAPS_GOOGLE_HOST.matcher(host).matches();
    }

    private static boolean isGoogleHost(@NonNull String host) {
        return GOOGLE_HOST.matcher(host).matches();
    }

    @Nullable
    static String parseDataCoordinates(
            @Nullable String host,
            @NonNull String path,
            @NonNull String withoutFragment
    ) {
        if (!isMapUrl(host, path)) {
            return null;
        }
        String rawCoordinates = parseDataCoordinatesFrom(withoutFragment);
        if (rawCoordinates != null) {
            return rawCoordinates;
        }
        return parseDataCoordinatesFrom(IntentUriDecoder.decodeComponent(withoutFragment));
    }

    @Nullable
    private static String parseDataCoordinatesFrom(@NonNull String text) {
        Matcher matcher = DATA_COORDINATES.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return IntentLocationCoordinates.extractDecoded(matcher.group(1) + "," + matcher.group(2));
    }
}
