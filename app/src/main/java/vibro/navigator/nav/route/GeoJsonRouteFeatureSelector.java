package vibro.navigator.nav.route;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

final class GeoJsonRouteFeatureSelector {
    private GeoJsonRouteFeatureSelector() {
    }

    @Nullable
    static JSONObject firstTrackFeature(@NonNull JSONArray features) {
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.optJSONObject(i);
            if (feature != null && hasTrackGeometry(feature.optJSONObject("geometry"))) {
                return feature;
            }
        }
        return null;
    }

    private static boolean hasTrackGeometry(@Nullable JSONObject geometry) {
        JSONArray coordinates = geometry != null ? geometry.optJSONArray("coordinates") : null;
        if (coordinates == null || coordinates.optJSONArray(0) == null) {
            return false;
        }
        String type = geometry.optString("type", "");
        return type.isEmpty() || "LineString".equalsIgnoreCase(type);
    }
}
