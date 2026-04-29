package vibro.navigator.util;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.net.Uri;

import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IntentLocationParser {

    private static final Pattern COORDS = Pattern.compile("(?<![\\d.])(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)(?![\\d.])");
    private static final Pattern MAP_URL_IN_TEXT = Pattern.compile("((?:https?://|geo:|google\\.navigation:)[^\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AT_COORDS = Pattern.compile("@\\s*(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)");

    private IntentLocationParser() {
    }

    @Nullable
    public static String parseToQuery(@NonNull Intent intent) {
        return parseToQuery(intent.getAction(), intent.getDataString(), intent.getStringExtra(Intent.EXTRA_TEXT));
    }

    @Nullable
    static String parseToQuery(@Nullable String action, @Nullable String dataString, @Nullable String sharedText) {
        String parsedData = parseUriString(dataString);
        if (parsedData != null) {
            return parsedData;
        }

        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action) || sharedText != null) {
            return parseSharedText(sharedText);
        }
        return null;
    }

    @Nullable
    private static String parseUriString(@Nullable String uriString) {
        if (uriString == null) {
            return null;
        }
        String trimmed = uriString.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int schemeEnd = trimmed.indexOf(':');
        if (schemeEnd <= 0) {
            return extractCoordinates(trimmed);
        }

        String scheme = trimmed.substring(0, schemeEnd).toLowerCase(Locale.US);
        switch (scheme) {
            case "geo":
                return parseGeoUri(trimmed.substring(schemeEnd + 1));
            case "google.navigation":
                return parseNavigationUri(trimmed.substring(schemeEnd + 1));
            case "http":
            case "https":
                return parseWebMapUri(trimmed);
            default:
                return extractCoordinates(trimmed);
        }
    }

    @Nullable
    private static String parseSharedText(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        Matcher urlMatcher = MAP_URL_IN_TEXT.matcher(trimmed);
        if (urlMatcher.find()) {
            String parsedUrl = parseUriString(urlMatcher.group(1));
            if (parsedUrl != null) {
                return parsedUrl;
            }
        }

        String coords = extractCoordinates(trimmed);
        if (coords != null) {
            return coords;
        }
        return trimmed;
    }

    @Nullable
    private static String parseGeoUri(@NonNull String schemeSpecific) {
        int queryIndex = schemeSpecific.indexOf('?');
        String locationPart = queryIndex >= 0 ? schemeSpecific.substring(0, queryIndex) : schemeSpecific;
        String queryPart = queryIndex >= 0 ? schemeSpecific.substring(queryIndex + 1) : null;

        if (hasAnyQueryKey(queryPart, "q", "query")) {
            return normalizeCandidate(firstQueryValue(queryPart, "q", "query"));
        }
        return extractCoordinates(locationPart);
    }

    @Nullable
    private static String parseNavigationUri(@NonNull String schemeSpecific) {
        String query = stripLeadingSlashes(schemeSpecific);
        if (hasAnyQueryKey(query, "q", "query", "destination", "daddr", "ll")) {
            return normalizeCandidate(firstQueryValue(query, "q", "query", "destination", "daddr", "ll"));
        }
        return extractCoordinates(schemeSpecific);
    }

    @Nullable
    private static String parseWebMapUri(@NonNull String uriString) {
        String withoutFragment = stripFragment(uriString);
        int queryIndex = withoutFragment.indexOf('?');
        String base = queryIndex >= 0 ? withoutFragment.substring(0, queryIndex) : withoutFragment;
        String query = queryIndex >= 0 ? withoutFragment.substring(queryIndex + 1) : null;
        String host = extractHost(base);

        if (isKnownMapHost(host)) {
            String mapQueryResult = parseKnownMapQuery(query);
            if (mapQueryResult != null) {
                return mapQueryResult;
            }
        }

        Matcher atMatcher = AT_COORDS.matcher(withoutFragment);
        if (atMatcher.find()) {
            return normalizeCoordinates(atMatcher.group(1), atMatcher.group(2));
        }
        return extractCoordinates(withoutFragment);
    }

    @Nullable
    private static String parseKnownMapQuery(@Nullable String query) {
        if (hasAnyQueryKey(query, "q", "query", "destination", "daddr", "ll")) {
            return normalizeCandidate(firstQueryValue(query, "q", "query", "destination", "daddr", "ll"));
        }
        String mlat = firstQueryValue(query, "mlat");
        String mlon = firstQueryValue(query, "mlon");
        if (mlat == null || mlon == null) {
            return null;
        }
        return extractCoordinates(mlat + "," + mlon);
    }

    @Nullable
    private static String normalizeCandidate(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String decoded = decodeComponent(raw).trim();
        if (decoded.isEmpty()) {
            return null;
        }
        String nestedUri = parseUriString(decoded);
        if (nestedUri != null) {
            return nestedUri;
        }
        String coords = extractCoordinates(decoded);
        if (coords != null) {
            return coords;
        }
        if (looksLikeNumericCoordinates(decoded)) {
            return null;
        }
        return decoded;
    }

    @Nullable
    private static String extractCoordinates(@Nullable String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = COORDS.matcher(decodeComponent(text));
        if (!matcher.find()) {
            return null;
        }
        return normalizeCoordinates(matcher.group(1), matcher.group(2));
    }

    @Nullable
    private static String normalizeCoordinates(@NonNull String latString, @NonNull String lonString) {
        try {
            double lat = Double.parseDouble(latString);
            double lon = Double.parseDouble(lonString);
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                return null;
            }
            return latString + "," + lonString;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static String firstQueryValue(@Nullable String query, @NonNull String... keys) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        Map<String, String> params = parseQueryParams(query);
        for (String key : keys) {
            String value = params.get(key.toLowerCase(Locale.US));
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasAnyQueryKey(@Nullable String query, @NonNull String... keys) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        Map<String, String> params = parseQueryParams(query);
        for (String key : keys) {
            if (params.containsKey(key.toLowerCase(Locale.US))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static Map<String, String> parseQueryParams(@NonNull String query) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String part : query.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int separator = part.indexOf('=');
            String key = separator >= 0 ? part.substring(0, separator) : part;
            String value = separator >= 0 ? part.substring(separator + 1) : "";
            params.put(decodeComponent(key).toLowerCase(Locale.US), value);
        }
        return params;
    }

    @NonNull
    private static String stripLeadingSlashes(@NonNull String text) {
        int start = 0;
        while (start < text.length() && text.charAt(start) == '/') {
            start++;
        }
        return text.substring(start);
    }

    @NonNull
    private static String stripFragment(@NonNull String uriString) {
        int fragmentIndex = uriString.indexOf('#');
        return fragmentIndex >= 0 ? uriString.substring(0, fragmentIndex) : uriString;
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

    private static boolean isKnownMapHost(@Nullable String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        return host.equals("maps.google.com")
                || host.equals("google.com")
                || host.equals("www.google.com")
                || host.equals("openstreetmap.org")
                || host.equals("www.openstreetmap.org");
    }

    private static boolean looksLikeNumericCoordinates(@NonNull String value) {
        return value.indexOf(',') >= 0 && value.matches("[()\\s+\\-\\d.,]+");
    }

    @NonNull
    private static String decodeComponent(@NonNull String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception ignored) {
            return Uri.decode(value);
        }
    }
}
