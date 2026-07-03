package vibro.navigator.poi.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.poi.Poi;
import vibro.navigator.logging.AppLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

public final class OsmNominatimClient implements PoiSearchClient, PoiReverseGeocodingClient {

    private static final String TAG = "OsmNominatim";

    @NonNull
    @Override
    public List<Poi> search(@NonNull String query, int limit) throws IOException {
        AppLogger.i(TAG, "Searching query=" + query + " limit=" + limit);
        HttpURLConnection conn = openConnection(query, limit);
        try {
            String body = readResponseBody(conn, query);
            List<Poi> out = body.isEmpty() ? new ArrayList<>() : parsePois(body);
            AppLogger.i(TAG, "Search completed query=" + query + " results=" + out.size());
            return out;
        } catch (Exception e) {
            AppLogger.e(TAG, "Search failed query=" + query, e);
            throw new IOException(e);
        } finally {
            conn.disconnect();
        }
    }

    @Nullable
    @Override
    public String reverseGeocode(double lat, double lon) throws IOException {
        AppLogger.i(TAG, "Reverse geocoding lat=" + lat + " lon=" + lon);
        HttpURLConnection conn = openConnection(buildReverseGeocodeUrl(lat, lon));
        try {
            String body = readResponseBody(conn, lat + "," + lon);
            String address = body.isEmpty() ? null : parseReverseDisplayName(body);
            AppLogger.i(TAG, "Reverse geocoding completed hasAddress=" + (address != null));
            return address;
        } catch (Exception e) {
            AppLogger.e(TAG, "Reverse geocoding failed lat=" + lat + " lon=" + lon, e);
            throw new IOException(e);
        } finally {
            conn.disconnect();
        }
    }

    @NonNull
    private static HttpURLConnection openConnection(@NonNull String query, int limit) throws IOException {
        return openConnection(buildSearchUrl(query, limit));
    }

    @NonNull
    private static HttpURLConnection openConnection(@NonNull String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "VibeNavigator");
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    @NonNull
    private static String buildSearchUrl(@NonNull String query, int limit) throws IOException {
        String q = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        return String.format(Locale.US,
                "https://nominatim.openstreetmap.org/search?q=%s&format=jsonv2&addressdetails=0&limit=%d",
                q, Math.max(1, Math.min(20, limit))
        );
    }

    @NonNull
    private static String buildReverseGeocodeUrl(double lat, double lon) {
        return String.format(Locale.US,
                "https://nominatim.openstreetmap.org/reverse?lat=%.8f&lon=%.8f&format=jsonv2&addressdetails=0",
                lat,
                lon
        );
    }

    @NonNull
    private static String readResponseBody(@NonNull HttpURLConnection conn, @NonNull String query) throws IOException {
        int code = conn.getResponseCode();
        AppLogger.i(TAG, "HTTP response code=" + code);
        if (!isSuccessfulHttpStatus(code)) {
            throw new IOException("Nominatim returned HTTP " + code);
        }
        InputStream response = conn.getInputStream();
        if (response == null) {
            AppLogger.w(TAG, "No response stream available for query=" + query);
            return "";
        }
        try (InputStream is = response) {
            return readAll(is);
        }
    }

    @NonNull
    private static List<Poi> parsePois(@NonNull String body) throws JSONException {
        JSONArray arr = new JSONArray(body);
        List<Poi> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            Poi poi = parsePoi(arr.optJSONObject(i));
            if (poi != null) {
                out.add(poi);
            }
        }
        return out;
    }

    @Nullable
    private static Poi parsePoi(@Nullable JSONObject object) {
        if (object == null) {
            return null;
        }
        String display = object.optString("display_name", "");
        String latStr = object.optString("lat", "");
        String lonStr = object.optString("lon", "");
        if (display.isEmpty() || latStr.isEmpty() || lonStr.isEmpty()) {
            return null;
        }
        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            return LatLon.isValidCoordinate(lat, lon) ? new Poi(display, lat, lon) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    static String parseReverseDisplayName(@NonNull String body) throws JSONException {
        JSONObject root = new JSONObject(body);
        String displayName = root.optString("display_name", "").trim();
        return displayName.isEmpty() ? null : displayName;
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

    private static boolean isSuccessfulHttpStatus(int code) {
        return code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE;
    }
}
