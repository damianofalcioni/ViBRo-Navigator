package vibro.navigator.poi.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class GoogleGeocodeAddressComponents {
    private static final String KEY_LONG_NAME = "long_name";
    private static final String KEY_TYPES = "types";
    private static final String[][] TYPE_KEYS = {
            {"point_of_interest", "amenity"},
            {"establishment", "amenity"},
            {"premise", "building"},
            {"street_number", "house_number"},
            {"route", "road"},
            {"locality", "city"},
            {"postal_town", "town"},
            {"sublocality", "suburb"},
            {"sublocality_level_1", "suburb"},
            {"neighborhood", "neighbourhood"},
            {"administrative_area_level_2", "county"},
            {"administrative_area_level_1", "state"},
            {"postal_code", "postcode"},
            {"country", "country"}
    };
    private static final Map<String, String> ADDRESS_KEYS = addressKeys();

    private GoogleGeocodeAddressComponents() {
    }

    @NonNull
    static Map<String, String> parse(@Nullable JSONArray components) {
        Map<String, String> out = new LinkedHashMap<>();
        if (components == null) {
            return out;
        }
        for (int i = 0; i < components.length(); i++) {
            addAddressComponent(out, components.optJSONObject(i));
        }
        return out;
    }

    private static void addAddressComponent(
            @NonNull Map<String, String> out,
            @Nullable JSONObject component
    ) {
        if (component == null) {
            return;
        }
        String value = component.optString(KEY_LONG_NAME, "").trim();
        if (value.isEmpty()) {
            return;
        }
        String key = addressComponentKey(component.optJSONArray(KEY_TYPES));
        if (key != null && !out.containsKey(key)) {
            out.put(key, value);
        }
    }

    @Nullable
    private static String addressComponentKey(@Nullable JSONArray types) {
        if (types == null) {
            return null;
        }
        for (int i = 0; i < types.length(); i++) {
            String key = ADDRESS_KEYS.get(types.optString(i, ""));
            if (key != null) {
                return key;
            }
        }
        return null;
    }

    @NonNull
    private static Map<String, String> addressKeys() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String[] pair : TYPE_KEYS) {
            out.put(pair[0], pair[1]);
        }
        return Collections.unmodifiableMap(out);
    }
}
