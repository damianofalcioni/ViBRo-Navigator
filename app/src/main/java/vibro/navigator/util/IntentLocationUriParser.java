package vibro.navigator.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

final class IntentLocationUriParser {

    private static final String QUERY_KEY_Q = "q";
    private static final String QUERY_KEY_QUERY = "query";
    private static final String QUERY_KEY_DESTINATION = "destination";
    private static final String QUERY_KEY_DADDR = "daddr";
    private static final String QUERY_KEY_LL = "ll";
    private static final String[] GEO_QUERY_KEYS = {QUERY_KEY_Q, QUERY_KEY_QUERY};
    private static final String[] MAP_QUERY_KEYS = {
            QUERY_KEY_Q,
            QUERY_KEY_QUERY,
            QUERY_KEY_DESTINATION,
            QUERY_KEY_DADDR,
            QUERY_KEY_LL
    };

    private IntentLocationUriParser() {
    }

    @Nullable
    static String parse(@Nullable String uriString) {
        if (uriString == null) {
            return null;
        }
        String trimmed = uriString.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int schemeEnd = trimmed.indexOf(':');
        if (schemeEnd <= 0) {
            return IntentLocationCoordinates.extract(trimmed);
        }

        String scheme = trimmed.substring(0, schemeEnd).toLowerCase(Locale.US);
        return parseByScheme(scheme, trimmed, schemeEnd);
    }

    @Nullable
    private static String parseByScheme(@NonNull String scheme, @NonNull String trimmed, int schemeEnd) {
        switch (scheme) {
            case "geo":
                return parseGeoUri(trimmed.substring(schemeEnd + 1));
            case "google.navigation":
                return parseNavigationUri(trimmed.substring(schemeEnd + 1));
            case "http":
            case "https":
                return IntentWebMapUriParser.parse(trimmed, MAP_QUERY_KEYS);
            default:
                return IntentLocationCoordinates.extract(trimmed);
        }
    }

    @Nullable
    private static String parseGeoUri(@NonNull String schemeSpecific) {
        int queryIndex = schemeSpecific.indexOf('?');
        String locationPart = queryIndex >= 0 ? schemeSpecific.substring(0, queryIndex) : schemeSpecific;
        String queryPart = queryIndex >= 0 ? schemeSpecific.substring(queryIndex + 1) : null;

        if (IntentLocationQueryParams.hasAnyKey(queryPart, GEO_QUERY_KEYS)) {
            return normalizeCandidate(IntentLocationQueryParams.firstValue(queryPart, GEO_QUERY_KEYS));
        }
        return IntentLocationCoordinates.extract(locationPart);
    }

    @Nullable
    private static String parseNavigationUri(@NonNull String schemeSpecific) {
        String query = stripLeadingSlashes(schemeSpecific);
        if (IntentLocationQueryParams.hasAnyKey(query, MAP_QUERY_KEYS)) {
            return normalizeCandidate(IntentLocationQueryParams.firstValue(query, MAP_QUERY_KEYS));
        }
        return IntentLocationCoordinates.extract(schemeSpecific);
    }

    @Nullable
    static String normalizeCandidate(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String decoded = IntentUriDecoder.decodeComponent(raw).trim();
        if (decoded.isEmpty()) {
            return null;
        }
        String nestedUri = parse(decoded);
        if (nestedUri != null) {
            return nestedUri;
        }
        String coords = IntentLocationCoordinates.extract(decoded);
        if (coords != null) {
            return coords;
        }
        if (IntentLocationCoordinates.looksLikeNumericCoordinates(decoded)) {
            return null;
        }
        return decoded;
    }

    @NonNull
    private static String stripLeadingSlashes(@NonNull String text) {
        int start = 0;
        while (start < text.length() && text.charAt(start) == '/') {
            start++;
        }
        return text.substring(start);
    }

}
