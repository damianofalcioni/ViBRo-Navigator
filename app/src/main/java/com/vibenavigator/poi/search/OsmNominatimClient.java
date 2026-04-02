package com.vibenavigator.poi.search;

import androidx.annotation.NonNull;

import com.vibenavigator.poi.Poi;
import com.vibenavigator.util.AppLogger;

import org.json.JSONArray;
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

public final class OsmNominatimClient implements PoiSearchClient {

    private static final String TAG = "OsmNominatim";

    @NonNull
    @Override
    public List<Poi> search(@NonNull String query, int limit) throws IOException {
        AppLogger.i(TAG, "Searching query=" + query + " limit=" + limit);
        String q = URLEncoder.encode(query, "UTF-8");
        String url = String.format(Locale.US,
                "https://nominatim.openstreetmap.org/search?q=%s&format=jsonv2&addressdetails=0&limit=%d",
                q, Math.max(1, Math.min(20, limit))
        );
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "VibeNavigator");
        conn.setRequestProperty("Accept", "application/json");
        try {
            int code = conn.getResponseCode();
            AppLogger.i(TAG, "HTTP response code=" + code);
            InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) {
                AppLogger.w(TAG, "No response stream available for query=" + query);
                return new ArrayList<>();
            }
            String body = readAll(is);
            JSONArray arr = new JSONArray(body);
            List<Poi> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                String display = o.optString("display_name", "");
                String latStr = o.optString("lat", "");
                String lonStr = o.optString("lon", "");
                if (display.isEmpty() || latStr.isEmpty() || lonStr.isEmpty()) {
                    continue;
                }
                try {
                    double lat = Double.parseDouble(latStr);
                    double lon = Double.parseDouble(lonStr);
                    out.add(new Poi(display, lat, lon));
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }
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
