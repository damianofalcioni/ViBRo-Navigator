package vibro.navigator.poi.search;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.poi.Poi;

final class GoogleGeocodeResponseParser {
    private static final String STATUS_OK = "OK";
    private static final String STATUS_REQUEST_DENIED = "REQUEST_DENIED";

    private GoogleGeocodeResponseParser() {
    }

    @NonNull
    static List<Poi> parseResults(@NonNull String body, int limit) throws JSONException {
        JSONObject root = new JSONObject(body);
        JSONArray results = root.optJSONArray("results");
        List<Poi> out = new ArrayList<>();
        if (results == null) {
            return out;
        }
        for (int i = 0; i < results.length() && out.size() < limit; i++) {
            Poi poi = parseResult(results.optJSONObject(i));
            if (poi != null) {
                out.add(poi);
            }
        }
        return out;
    }

    @NonNull
    static String parseStatus(@NonNull String body) throws JSONException {
        JSONObject root = new JSONObject(body);
        return root.optString("status", "");
    }

    static boolean isOkStatus(@NonNull String body) throws JSONException {
        return STATUS_OK.equals(parseStatus(body));
    }

    static boolean isRequestDeniedStatus(@NonNull String body) throws JSONException {
        return STATUS_REQUEST_DENIED.equals(parseStatus(body));
    }

    private static Poi parseResult(JSONObject result) {
        if (result == null) {
            return null;
        }
        String name = result.optString("formatted_address", "");
        JSONObject geometry = result.optJSONObject("geometry");
        if (geometry == null) {
            return null;
        }
        JSONObject location = geometry.optJSONObject("location");
        if (location == null) {
            return null;
        }
        double lat = location.optDouble("lat", Double.NaN);
        double lon = location.optDouble("lng", Double.NaN);
        if (name.isEmpty() || Double.isNaN(lat) || Double.isNaN(lon)) {
            return null;
        }
        return new Poi(name, lat, lon);
    }
}
