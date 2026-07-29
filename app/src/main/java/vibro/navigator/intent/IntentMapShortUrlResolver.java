package vibro.navigator.intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public final class IntentMapShortUrlResolver {

    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";
    private static final String SHORT_GOOGLE_MAPS_HOST = "maps.app.goo.gl";
    private static final String HOST_TERMINATORS = "/?#";
    private static final int MAX_REDIRECTS = 8;
    private static final int TIMEOUT_MS = 8000;

    private IntentMapShortUrlResolver() {
    }

    @Nullable
    public static String normalizeShortMapUrl(@Nullable String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String candidate = rawUrl.trim();
        if (candidate.isEmpty()) {
            return null;
        }
        String normalized = ensureHttpScheme(candidate);
        String host = extractHost(normalized);
        if (!SHORT_GOOGLE_MAPS_HOST.equals(host)) {
            return null;
        }
        return normalized;
    }

    @Nullable
    public static String expand(@NonNull String rawUrl) throws IOException {
        return expand(rawUrl, IntentMapShortUrlResolver::openConnection);
    }

    @Nullable
    static String expand(
            @NonNull String rawUrl,
            @NonNull ConnectionFactory connectionFactory
    ) throws IOException {
        String current = normalizeShortMapUrl(rawUrl);
        if (current == null) {
            return null;
        }
        for (int redirects = 0; redirects < MAX_REDIRECTS; redirects++) {
            URL currentUrl = new URL(current);
            HttpURLConnection connection = connectionFactory.open(currentUrl);
            connection.setInstanceFollowRedirects(false);
            try {
                int responseCode = connection.getResponseCode();
                if (!isRedirect(responseCode)) {
                    return current;
                }
                String location = connection.getHeaderField("Location");
                if (location == null || location.trim().isEmpty()) {
                    return current;
                }
                current = new URL(currentUrl, location.trim()).toExternalForm();
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Too many redirects expanding Google Maps short URL");
    }

    @NonNull
    private static HttpURLConnection openConnection(@NonNull URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "VibeNavigator");
        connection.setRequestProperty("Accept", "text/html,*/*");
        return connection;
    }

    @NonNull
    private static String ensureHttpScheme(@NonNull String candidate) {
        String lower = candidate.toLowerCase(Locale.US);
        if (lower.startsWith(HTTP_PREFIX) || lower.startsWith(HTTPS_PREFIX)) {
            return candidate;
        }
        if (lower.startsWith(SHORT_GOOGLE_MAPS_HOST + "/")) {
            return HTTPS_PREFIX + candidate;
        }
        return candidate;
    }

    @Nullable
    private static String extractHost(@NonNull String url) {
        String lower = url.toLowerCase(Locale.US);
        int schemeEnd = lower.indexOf("://");
        if (schemeEnd < 0) {
            return null;
        }
        if (!lower.startsWith(HTTP_PREFIX) && !lower.startsWith(HTTPS_PREFIX)) {
            return null;
        }
        int hostStart = schemeEnd + 3;
        int hostEnd = firstHostTerminator(lower, hostStart);
        String authority = url.substring(hostStart, hostEnd);
        int portIndex = authority.indexOf(':');
        String host = portIndex >= 0 ? authority.substring(0, portIndex) : authority;
        return host.toLowerCase(Locale.US);
    }

    private static int firstHostTerminator(@NonNull String value, int start) {
        int end = value.length();
        for (int i = 0; i < HOST_TERMINATORS.length(); i++) {
            char terminator = HOST_TERMINATORS.charAt(i);
            int index = value.indexOf(terminator, start);
            if (index >= 0 && index < end) {
                end = index;
            }
        }
        return end;
    }

    private static boolean isRedirect(int responseCode) {
        return responseCode == HttpURLConnection.HTTP_MOVED_PERM
                || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                || responseCode == 307
                || responseCode == 308;
    }

    interface ConnectionFactory {
        @NonNull
        HttpURLConnection open(@NonNull URL url) throws IOException;
    }
}
