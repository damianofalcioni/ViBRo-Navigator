package vibro.navigator.map;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import vibro.navigator.logging.AppLogger;

final class MapPoiMarkerJson {
    private static final String TAG = "MapPoiMarkerJson";

    interface CategoryLabelSource {
        @NonNull
        String labelFor(@NonNull MapPoiCategory category);
    }

    private MapPoiMarkerJson() {
    }

    @NonNull
    static JSONArray toJson(
            @NonNull List<MapPoiMarker> markers,
            @NonNull CategoryLabelSource labelSource
    ) {
        JSONArray array = new JSONArray();
        for (MapPoiMarker marker : markers) {
            array.put(toJson(marker, labelSource));
        }
        return array;
    }

    @NonNull
    private static JSONObject toJson(
            @NonNull MapPoiMarker marker,
            @NonNull CategoryLabelSource labelSource
    ) {
        JSONObject object = new JSONObject();
        try {
            object.put("name", marker.name);
            object.put("lat", marker.lat);
            object.put("lon", marker.lon);
            object.put("categoryId", marker.category.id);
            object.put("categoryLabel", labelSource.labelFor(marker.category));
        } catch (JSONException ignored) {
            AppLogger.w(TAG, "Failed to build map POI marker JSON");
        }
        return object;
    }
}
