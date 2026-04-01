package com.vibenavigator.poi.search;

import androidx.annotation.NonNull;

import com.vibenavigator.poi.Poi;

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

public final class GoogleGeocodeClient implements PoiSearchClient {

    private final String apiKey;

    public GoogleGeocodeClient(@NonNull String apiKey) {
        this.apiKey = apiKey;
    }

    @NonNull
    @Override
    public List<Poi> search(@NonNull String query, int limit) throws IOException {
        String q = URLEncoder.encode(query, "UTF-8");
        String url = String.format(Locale.US,
                "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s",
                q,
                URLEncoder.encode(apiKey, "UTF-8")
        );
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        try {
            int code = conn.getResponseCode();
            InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) {
                return new ArrayList<>();
            }
            String body = readAll(is);
            JSONObject root = new JSONObject(body);
            JSONArray results = root.optJSONArray("results");
            List<Poi> out = new ArrayList<>();
            if (results == null) {
                return out;
            }
            for (int i = 0; i < results.length() && out.size() < limit; i++) {
                JSONObject r = results.optJSONObject(i);
                if (r == null) {
                    continue;
                }
                String name = r.optString("formatted_address", "");
                JSONObject geometry = r.optJSONObject("geometry");
                if (geometry == null) {
                    continue;
                }
                JSONObject location = geometry.optJSONObject("location");
                if (location == null) {
                    continue;
                }
                double lat = location.optDouble("lat", Double.NaN);
                double lon = location.optDouble("lng", Double.NaN);
                if (name.isEmpty() || Double.isNaN(lat) || Double.isNaN(lon)) {
                    continue;
                }
                out.add(new Poi(name, lat, lon));
            }
            return out;
        } catch (Exception e) {
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
