package vibro.navigator.map;

import androidx.annotation.NonNull;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vibro.navigator.logging.AppLogger;

final class OsmMapPoiClient {
    private static final String TAG = "OsmMapPoiClient";
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final int MAX_RESULTS = 1000;
    @NonNull
    private final OsmMapPoiDiscoveryParser discoveryParser = new OsmMapPoiDiscoveryParser();
    @NonNull
    private final OsmMapPoiMarkerParser markerParser = new OsmMapPoiMarkerParser();

    @NonNull
    OsmMapPoiDiscoveryResult discover(@NonNull MapPickerBounds bounds) throws IOException {
        HttpURLConnection conn = openConnection();
        try {
            writeRequestBody(conn, buildDiscoveryRequestBody(bounds));
            String response = readResponseBody(conn);
            return discoveryParser.parse(response);
        } catch (JSONException e) {
            throw new IOException(e);
        } finally {
            conn.disconnect();
        }
    }

    @NonNull
    OsmMapPoiDiscoveryResult discover(
            @NonNull MapPickerBounds bounds,
            @NonNull List<MapPoiCategory> categories
    ) throws IOException {
        List<MapPoiMarker> markers = search(bounds, categories);
        return new OsmMapPoiDiscoveryResult(withMarkerCounts(categories, markers), markers);
    }

    @NonNull
    List<MapPoiMarker> search(
            @NonNull MapPickerBounds bounds,
            @NonNull List<MapPoiCategory> categories
    ) throws IOException {
        String body = buildRequestBody(bounds, categories);
        HttpURLConnection conn = openConnection();
        try {
            writeRequestBody(conn, body);
            String response = readResponseBody(conn);
            return markerParser.parse(response, categories);
        } catch (JSONException e) {
            throw new IOException(e);
        } finally {
            conn.disconnect();
        }
    }

    @NonNull
    private static List<MapPoiCategory> withMarkerCounts(
            @NonNull List<MapPoiCategory> categories,
            @NonNull List<MapPoiMarker> markers
    ) {
        Map<String, Integer> counts = new HashMap<>();
        for (MapPoiMarker marker : markers) {
            Integer count = counts.get(marker.category.id);
            counts.put(marker.category.id, count == null ? 1 : count + 1);
        }
        List<MapPoiCategory> out = new ArrayList<>();
        for (MapPoiCategory category : categories) {
            Integer count = counts.get(category.id);
            out.add(category.withCount(count == null ? 0 : count));
        }
        return out;
    }

    @NonNull
    private static String buildDiscoveryRequestBody(@NonNull MapPickerBounds bounds) {
        String bbox = bounds.overpassBbox();
        StringBuilder query = new StringBuilder("[out:json][timeout:10];(");
        for (String selector : MapPoiCategory.discoverySelectors()) {
            query.append("node").append(selector).append("(").append(bbox).append(");");
            query.append("way").append(selector).append("(").append(bbox).append(");");
            query.append("relation").append(selector).append("(").append(bbox).append(");");
        }
        query.append(");out tags center;");
        return query.toString();
    }

    @NonNull
    private static HttpURLConnection openConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(OVERPASS_URL).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(12000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", "VibeNavigator");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        return conn;
    }

    private static void writeRequestBody(@NonNull HttpURLConnection conn, @NonNull String body) throws IOException {
        byte[] bytes = ("data=" + URLEncoder.encode(body, StandardCharsets.UTF_8.name()))
                .getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(bytes);
        }
    }

    @NonNull
    private static String buildRequestBody(
            @NonNull MapPickerBounds bounds,
            @NonNull List<MapPoiCategory> categories
    ) {
        String bbox = bounds.overpassBbox();
        StringBuilder query = new StringBuilder("[out:json][timeout:12];(");
        for (MapPoiCategory category : categories) {
            appendCategoryQuery(query, category, bbox);
        }
        query.append(");out center ").append(MAX_RESULTS).append(";");
        return query.toString();
    }

    private static void appendCategoryQuery(
            @NonNull StringBuilder query,
            @NonNull MapPoiCategory category,
            @NonNull String bbox
    ) {
        for (String selector : category.overpassSelectors()) {
            query.append("node").append(selector).append("(").append(bbox).append(");");
            query.append("way").append(selector).append("(").append(bbox).append(");");
            query.append("relation").append(selector).append("(").append(bbox).append(");");
        }
    }

    @NonNull
    private static String readResponseBody(@NonNull HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        AppLogger.i(TAG, "Overpass response code=" + code);
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
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
