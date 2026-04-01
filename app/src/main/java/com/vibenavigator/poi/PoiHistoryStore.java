package com.vibenavigator.poi;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PoiHistoryStore {

    private static final String PREFS = "vibenavigator_poi_history";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 30;

    private final SharedPreferences prefs;

    public PoiHistoryStore(@NonNull Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public List<Poi> list() {
        String raw = prefs.getString(KEY_ITEMS, "[]");
        List<Poi> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                String name = o.optString("name", "");
                double lat = o.optDouble("lat", Double.NaN);
                double lon = o.optDouble("lon", Double.NaN);
                if (name.isEmpty() || Double.isNaN(lat) || Double.isNaN(lon)) {
                    continue;
                }
                out.add(new Poi(name, lat, lon));
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return out;
    }

    public void addOrPromote(@NonNull Poi poi) {
        List<Poi> current = list();
        Map<String, Poi> unique = new LinkedHashMap<>();

        unique.put(poi.stableKey(), poi);
        for (Poi p : current) {
            if (unique.size() >= MAX_ITEMS) {
                break;
            }
            unique.put(p.stableKey(), p);
        }
        save(unique.values());
    }

    public void remove(@NonNull Poi poi) {
        List<Poi> current = list();
        List<Poi> next = new ArrayList<>();
        for (Poi p : current) {
            if (!p.stableKey().equals(poi.stableKey())) {
                next.add(p);
            }
        }
        save(next);
    }

    private void save(@NonNull Iterable<Poi> items) {
        JSONArray arr = new JSONArray();
        for (Poi p : items) {
            JSONObject o = new JSONObject();
            try {
                o.put("name", p.name);
                o.put("lat", p.lat);
                o.put("lon", p.lon);
                arr.put(o);
            } catch (JSONException ignored) {
                // ignore
            }
        }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply();
    }
}
