package vibro.navigator.map;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import vibro.navigator.geo.LatLon;

final class MapPickerCenter {
    final double lat;
    final double lon;

    private MapPickerCenter(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    @Nullable
    static MapPickerCenter parseJavascriptResult(@Nullable String value) throws JSONException {
        if (value == null || "null".equals(value)) {
            return null;
        }
        String json = new JSONArray("[" + value + "]").optString(0, "");
        if (json.isEmpty()) {
            return null;
        }
        JSONObject object = new JSONObject(json);
        double lat = object.getDouble("centerLat");
        double lon = object.getDouble("centerLon");
        if (!LatLon.isValidCoordinate(lat, lon)) {
            return null;
        }
        return new MapPickerCenter(lat, lon);
    }
}
