package vibro.navigator.main;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.Poi;

final class SavedRouteStore {
    static final String PREFS = "vibenavigator_saved_routes";

    private static final String KEY_ITEMS = "items";
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_CREATED_AT = "createdAtMillis";
    private static final String KEY_DESTINATION = "destination";
    private static final String KEY_STOPS = "stops";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String TAG = "SavedRouteStore";

    @NonNull
    private final SharedPreferences prefs;

    SavedRouteStore(@NonNull Context context) {
        this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }

    SavedRouteStore(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    @NonNull
    List<SavedRoute> list() {
        String raw = prefs.getString(KEY_ITEMS, "[]");
        List<SavedRoute> routes = new ArrayList<>();
        if (raw == null) {
            return routes;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                SavedRoute route = parseRoute(array.optJSONObject(i));
                if (route != null) {
                    routes.add(route);
                }
            }
        } catch (JSONException e) {
            AppLogger.w(TAG, "Failed to parse saved routes", e);
        }
        return routes;
    }

    @NonNull
    SavedRoute save(
            @NonNull String name,
            @NonNull Poi destination,
            @NonNull List<Poi> stops
    ) {
        List<SavedRoute> routes = list();
        SavedRoute route = new SavedRoute(
                SavedRouteIds.newRouteId(routes),
                name.trim(),
                destination,
                stops,
                System.currentTimeMillis()
        );
        routes.add(0, route);
        persist(routes);
        AppLogger.i(TAG, "Saved route id=" + route.id + " stopCount=" + route.stops.size());
        return route;
    }

    boolean rename(@NonNull String routeId, @NonNull String updatedName) {
        String trimmedName = updatedName.trim();
        if (trimmedName.isEmpty()) {
            return false;
        }
        List<SavedRoute> routes = list();
        List<SavedRoute> updatedRoutes = new ArrayList<>(routes.size());
        boolean renamed = false;
        for (SavedRoute route : routes) {
            if (route.id.equals(routeId)) {
                updatedRoutes.add(route.renamed(trimmedName));
                renamed = true;
            } else {
                updatedRoutes.add(route);
            }
        }
        if (renamed) {
            persist(updatedRoutes);
            AppLogger.i(TAG, "Renamed route id=" + routeId);
        }
        return renamed;
    }

    void remove(@NonNull String routeId) {
        List<SavedRoute> routes = list();
        List<SavedRoute> remainingRoutes = new ArrayList<>();
        for (SavedRoute route : routes) {
            if (!route.id.equals(routeId)) {
                remainingRoutes.add(route);
            }
        }
        persist(remainingRoutes);
        AppLogger.i(TAG, "Removed route id=" + routeId);
    }

    private void persist(@NonNull List<SavedRoute> routes) {
        JSONArray array = new JSONArray();
        try {
            for (SavedRoute route : routes) {
                array.put(routeToJson(route));
            }
            prefs.edit().putString(KEY_ITEMS, array.toString()).apply();
        } catch (JSONException e) {
            AppLogger.w(TAG, "Failed to serialize saved routes", e);
        }
    }

    @Nullable
    private static SavedRoute parseRoute(@Nullable JSONObject item) {
        if (item == null) {
            return null;
        }
        String id = item.optString(KEY_ID, "").trim();
        String name = item.optString(KEY_NAME, "").trim();
        Poi destination = parsePoi(item.optJSONObject(KEY_DESTINATION));
        if (id.isEmpty() || name.isEmpty() || destination == null) {
            return null;
        }
        return new SavedRoute(
                id,
                name,
                destination,
                parseStops(item.optJSONArray(KEY_STOPS)),
                item.optLong(KEY_CREATED_AT, 0L)
        );
    }

    @NonNull
    private static List<Poi> parseStops(@Nullable JSONArray rawStops) {
        List<Poi> stops = new ArrayList<>();
        if (rawStops == null) {
            return stops;
        }
        for (int i = 0; i < rawStops.length(); i++) {
            Poi stop = parsePoi(rawStops.optJSONObject(i));
            if (stop != null) {
                stops.add(stop);
            }
        }
        return stops;
    }

    @Nullable
    private static Poi parsePoi(@Nullable JSONObject item) {
        if (item == null) {
            return null;
        }
        Poi poi = new Poi(
                item.optString(KEY_NAME, ""),
                item.optDouble(KEY_LAT, Double.NaN),
                item.optDouble(KEY_LON, Double.NaN)
        );
        return poi.hasValidCoordinates() ? poi : null;
    }

    @NonNull
    private static JSONObject routeToJson(@NonNull SavedRoute route) throws JSONException {
        JSONObject item = new JSONObject();
        item.put(KEY_ID, route.id);
        item.put(KEY_NAME, route.name);
        item.put(KEY_CREATED_AT, route.createdAtMillis);
        item.put(KEY_DESTINATION, poiToJson(route.destination));
        item.put(KEY_STOPS, poisToJson(route.stops));
        return item;
    }

    @NonNull
    private static JSONArray poisToJson(@NonNull List<Poi> pois) throws JSONException {
        JSONArray array = new JSONArray();
        for (Poi poi : pois) {
            array.put(poiToJson(poi));
        }
        return array;
    }

    @NonNull
    private static JSONObject poiToJson(@NonNull Poi poi) throws JSONException {
        JSONObject item = new JSONObject();
        item.put(KEY_NAME, poi.name);
        item.put(KEY_LAT, poi.lat);
        item.put(KEY_LON, poi.lon);
        return item;
    }
}
