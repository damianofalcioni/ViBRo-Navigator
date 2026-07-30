package vibro.navigator.poi;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PoiHistoryStore {

    private static final String PREFS = "vibenavigator_poi_history";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 30;
    private static final String TAG = "PoiHistory";

    private final SharedPreferences prefs;

    public PoiHistoryStore(@NonNull Context context) {
        this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }

    public PoiHistoryStore(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
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
                Poi poi = parseItem(arr.optJSONObject(i));
                if (poi != null) {
                    out.add(poi);
                }
            }
        } catch (JSONException e) {
            AppLogger.w(TAG, "Failed to parse POI history payload", e);
            return new ArrayList<>();
        }
        return out;
    }

    @NonNull
    public List<Poi> search(@NonNull String query, int limit) {
        List<Poi> matches = new ArrayList<>();
        if (limit <= 0) {
            return matches;
        }

        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return matches;
        }

        for (Poi poi : list()) {
            if (matches.size() >= limit) {
                break;
            }
            if (matches(poi, normalizedQuery)) {
                matches.add(poi);
            }
        }
        return matches;
    }

    public void addOrPromote(@NonNull Poi poi) {
        if (!poi.hasValidCoordinates()) {
            AppLogger.w(TAG, "Ignoring POI history entry with invalid coordinates " + poi.displayLabel());
            return;
        }
        AppLogger.i(TAG, "Saving or promoting POI " + poi.displayLabel());
        Poi historyPoi = historyPoi(poi);
        List<Poi> current = list();
        Map<String, Poi> unique = new LinkedHashMap<>();

        unique.put(historyPoi.stableKey(), historyPoi);
        for (Poi p : current) {
            if (unique.size() >= MAX_ITEMS) {
                break;
            }
            String key = p.stableKey();
            if (!unique.containsKey(key)) {
                unique.put(key, p);
            }
        }
        save(unique.values());
    }

    @NonNull
    private static Poi historyPoi(@NonNull Poi poi) {
        if (poi.name.trim().isEmpty()) {
            return new Poi(poi.displayLabel(), poi.lat, poi.lon);
        }
        return poi;
    }

    public void remove(@NonNull Poi poi) {
        AppLogger.i(TAG, "Removing POI from history " + poi.displayLabel());
        List<Poi> current = list();
        List<Poi> next = new ArrayList<>();
        for (Poi p : current) {
            if (!p.stableKey().equals(poi.stableKey())) {
                next.add(p);
            }
        }
        save(next);
    }

    public boolean rename(@NonNull Poi poi, @NonNull String name) {
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            AppLogger.w(TAG, "Ignoring POI rename because name is blank key=" + poi.stableKey());
            return false;
        }

        AppLogger.i(TAG, "Renaming POI in history key=" + poi.stableKey() + " newName=" + trimmedName);
        List<Poi> current = list();
        List<Poi> next = new ArrayList<>();
        boolean renamed = false;
        for (Poi p : current) {
            if (p.stableKey().equals(poi.stableKey())) {
                next.add(new Poi(trimmedName, p.lat, p.lon));
                renamed = true;
            } else {
                next.add(p);
            }
        }
        if (renamed) {
            save(next);
        }
        return renamed;
    }

    private void save(@NonNull Iterable<Poi> items) {
        JSONArray arr = new JSONArray();
        int count = 0;
        for (Poi p : items) {
            JSONObject o = new JSONObject();
            try {
                o.put("name", p.name);
                o.put("lat", p.lat);
                o.put("lon", p.lon);
                arr.put(o);
                count++;
            } catch (JSONException ignored) {
                // ignore
            }
        }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply();
        AppLogger.d(TAG, "Persisted POI history count=" + count);
    }

    @Nullable
    private static Poi parseItem(@Nullable JSONObject item) {
        if (item == null) {
            return null;
        }
        String name = item.optString("name", "");
        double lat = item.optDouble("lat", Double.NaN);
        double lon = item.optDouble("lon", Double.NaN);
        Poi poi = new Poi(name, lat, lon);
        if (name.isEmpty() || !poi.hasValidCoordinates()) {
            return null;
        }
        return poi;
    }

    private static boolean matches(@NonNull Poi poi, @NonNull String normalizedQuery) {
        return normalize(poi.displayLabel()).contains(normalizedQuery)
                || normalize(poi.stableKey()).contains(normalizedQuery);
    }

    @NonNull
    private static String normalize(@NonNull String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
