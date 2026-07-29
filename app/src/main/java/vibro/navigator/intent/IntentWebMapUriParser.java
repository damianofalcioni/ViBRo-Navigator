package vibro.navigator.intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IntentWebMapUriParser {
    private static final String LAT_DECIMAL_NUMBER = "[+-]?\\d{1,2}(?:\\.\\d+)?";
    private static final String LON_DECIMAL_NUMBER = "[+-]?\\d{1,3}(?:\\.\\d+)?";
    private static final Pattern MAP_FRAGMENT_COORDINATES = Pattern.compile(
            "(?:^|&)map=[^/]+/(" + LAT_DECIMAL_NUMBER + ")/("
                    + LON_DECIMAL_NUMBER + ")(?:$|&)"
    );

    private IntentWebMapUriParser() {
    }

    @Nullable
    static String parse(@NonNull String uriString, @NonNull String... mapQueryKeys) {
        String fragment = extractFragment(uriString);
        String withoutFragment = stripFragment(uriString);
        int queryIndex = withoutFragment.indexOf('?');
        String base = queryIndex >= 0 ? withoutFragment.substring(0, queryIndex) : withoutFragment;
        String query = queryIndex >= 0 ? withoutFragment.substring(queryIndex + 1) : null;
        String host = extractHost(base);
        String path = extractPath(base);

        String knownMapResult = parseKnownMapUrl(host, path, query, fragment, withoutFragment, mapQueryKeys);
        if (knownMapResult != null) {
            return knownMapResult;
        }
        return parseCoordinateFallback(withoutFragment);
    }

    @Nullable
    private static String parseKnownMapUrl(
            @Nullable String host,
            @NonNull String path,
            @Nullable String query,
            @Nullable String fragment,
            @NonNull String withoutFragment,
            @NonNull String... mapQueryKeys
    ) {
        if (!isKnownMapUrl(host, path)) {
            return null;
        }
        String mapQueryResult = parseKnownMapQuery(query, mapQueryKeys);
        if (mapQueryResult != null) {
            return mapQueryResult;
        }
        String googleDataCoordinates = IntentGoogleMapsUrlParser.parseDataCoordinates(host, path, withoutFragment);
        if (googleDataCoordinates != null) {
            return googleDataCoordinates;
        }
        return parseMapFragment(fragment);
    }

    @Nullable
    private static String parseCoordinateFallback(@NonNull String withoutFragment) {
        String atCoordinates = IntentLocationCoordinates.extractAtCoordinates(withoutFragment);
        if (atCoordinates != null) {
            return atCoordinates;
        }
        return IntentLocationCoordinates.extract(withoutFragment);
    }

    @Nullable
    private static String parseMapFragment(@Nullable String fragment) {
        if (fragment == null || fragment.isEmpty()) {
            return null;
        }
        Matcher matcher = MAP_FRAGMENT_COORDINATES.matcher(fragment);
        if (!matcher.find()) {
            return null;
        }
        return IntentLocationCoordinates.extract(matcher.group(1) + "," + matcher.group(2));
    }

    @Nullable
    private static String parseKnownMapQuery(@Nullable String query, @NonNull String... mapQueryKeys) {
        if (IntentLocationQueryParams.hasAnyKey(query, mapQueryKeys)) {
            return IntentLocationUriParser.normalizeCandidate(
                    IntentLocationQueryParams.firstValue(query, mapQueryKeys)
            );
        }
        String mlat = IntentLocationQueryParams.firstValue(query, "mlat");
        String mlon = IntentLocationQueryParams.firstValue(query, "mlon");
        if (mlat == null || mlon == null) {
            return null;
        }
        return IntentLocationCoordinates.extract(mlat + "," + mlon);
    }

    @NonNull
    private static String stripFragment(@NonNull String uriString) {
        int fragmentIndex = uriString.indexOf('#');
        return fragmentIndex >= 0 ? uriString.substring(0, fragmentIndex) : uriString;
    }

    @Nullable
    private static String extractFragment(@NonNull String uriString) {
        int fragmentIndex = uriString.indexOf('#');
        return fragmentIndex >= 0 ? uriString.substring(fragmentIndex + 1) : null;
    }

    @Nullable
    private static String extractHost(@NonNull String base) {
        int schemeSeparator = base.indexOf("://");
        if (schemeSeparator < 0) {
            return null;
        }
        int hostStart = schemeSeparator + 3;
        int hostEnd = base.indexOf('/', hostStart);
        String authority = hostEnd >= 0 ? base.substring(hostStart, hostEnd) : base.substring(hostStart);
        int portIndex = authority.indexOf(':');
        String host = portIndex >= 0 ? authority.substring(0, portIndex) : authority;
        return host.toLowerCase(Locale.US);
    }

    @NonNull
    private static String extractPath(@NonNull String base) {
        int schemeSeparator = base.indexOf("://");
        if (schemeSeparator < 0) {
            return "";
        }
        int hostStart = schemeSeparator + 3;
        int pathStart = base.indexOf('/', hostStart);
        return pathStart >= 0 ? base.substring(pathStart) : "";
    }

    private static boolean isKnownMapUrl(@Nullable String host, @NonNull String path) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        if (IntentGoogleMapsUrlParser.isMapUrl(host, path)) {
            return true;
        }
        return host.equals("openstreetmap.org")
                || host.equals("www.openstreetmap.org");
    }
}
