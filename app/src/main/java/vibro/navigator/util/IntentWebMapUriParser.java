package vibro.navigator.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

final class IntentWebMapUriParser {

    private IntentWebMapUriParser() {
    }

    @Nullable
    static String parse(@NonNull String uriString, @NonNull String... mapQueryKeys) {
        String withoutFragment = stripFragment(uriString);
        int queryIndex = withoutFragment.indexOf('?');
        String base = queryIndex >= 0 ? withoutFragment.substring(0, queryIndex) : withoutFragment;
        String query = queryIndex >= 0 ? withoutFragment.substring(queryIndex + 1) : null;
        String host = extractHost(base);

        if (isKnownMapHost(host)) {
            String mapQueryResult = parseKnownMapQuery(query, mapQueryKeys);
            if (mapQueryResult != null) {
                return mapQueryResult;
            }
        }

        String atCoordinates = IntentLocationCoordinates.extractAtCoordinates(withoutFragment);
        if (atCoordinates != null) {
            return atCoordinates;
        }
        return IntentLocationCoordinates.extract(withoutFragment);
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
}
