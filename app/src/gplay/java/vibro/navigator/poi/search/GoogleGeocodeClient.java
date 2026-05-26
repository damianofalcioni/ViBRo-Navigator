package vibro.navigator.poi.search;

import androidx.annotation.NonNull;

import org.json.JSONException;

import vibro.navigator.distribution.GooglePoiApiKeyValidationResult;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.Poi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GoogleGeocodeClient implements PoiSearchClient {

    private static final String TAG = "GoogleGeocode";
    private static final String VALIDATION_QUERY = "Vienna, Austria";

    private final String apiKey;

    public GoogleGeocodeClient(@NonNull String apiKey) {
        this.apiKey = apiKey;
    }

    @NonNull
    public static GooglePoiApiKeyValidationResult validateApiKey(@NonNull String apiKey) {
        if (apiKey.trim().isEmpty()) {
            return GooglePoiApiKeyValidationResult.INVALID;
        }
        try {
            return validateApiKeyWithService(apiKey);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to validate Google API key", e);
            return GooglePoiApiKeyValidationResult.ERROR;
        }
    }

    @NonNull
    @Override
    public List<Poi> search(@NonNull String query, int limit) throws IOException {
        AppLogger.i(TAG, "Searching query=" + query + " limit=" + limit);
        HttpURLConnection conn = openConnection(buildSearchUrl(query, apiKey));
        try {
            int code = conn.getResponseCode();
            AppLogger.i(TAG, "HTTP response code=" + code);
            InputStream is = responseStream(conn, code);
            if (is == null) {
                AppLogger.w(TAG, "No response stream available for query=" + query);
                return new ArrayList<>();
            }
            List<Poi> out = GoogleGeocodeResponseParser.parseResults(readAll(is), limit);
            AppLogger.i(TAG, "Search completed query=" + query + " results=" + out.size());
            return out;
        } catch (Exception e) {
            AppLogger.e(TAG, "Search failed query=" + query, e);
            throw new IOException(e);
        } finally {
            conn.disconnect();
        }
    }

    @NonNull
    private static GooglePoiApiKeyValidationResult validateApiKeyWithService(@NonNull String apiKey)
            throws IOException, JSONException {
        HttpURLConnection conn = openConnection(buildSearchUrl(VALIDATION_QUERY, apiKey));
        try {
            int code = conn.getResponseCode();
            String body = readValidationBody(conn, code);
            if (code >= 200 && code < 300 && GoogleGeocodeResponseParser.isOkStatus(body)) {
                return GooglePoiApiKeyValidationResult.VALID;
            }
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED
                    || code == HttpURLConnection.HTTP_FORBIDDEN
                    || GoogleGeocodeResponseParser.isRequestDeniedStatus(body)) {
                return GooglePoiApiKeyValidationResult.INVALID;
            }
            return GooglePoiApiKeyValidationResult.ERROR;
        } finally {
            conn.disconnect();
        }
    }

    @NonNull
    private static String buildSearchUrl(@NonNull String query, @NonNull String apiKey) throws IOException {
        String q = URLEncoder.encode(query, "UTF-8");
        return String.format(Locale.US,
                "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s",
                q,
                URLEncoder.encode(apiKey, "UTF-8")
        );
    }

    @NonNull
    private static HttpURLConnection openConnection(@NonNull String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private static InputStream responseStream(@NonNull HttpURLConnection conn, int code) throws IOException {
        return code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
    }

    @NonNull
    private static String readValidationBody(@NonNull HttpURLConnection conn, int code) throws IOException {
        InputStream is = responseStream(conn, code);
        if (is == null) {
            return "";
        }
        return readAll(is);
    }

    @NonNull
    private static String readAll(@NonNull InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = br.read(buf)) >= 0) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }
}
