package vibro.navigator.poi.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import vibro.navigator.geo.LatLon;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiDetails;

final class OsmNominatimSearchParser {
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_ENTRANCES = "entrances";
    private static final String KEY_EXTRA_TAGS = "extratags";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_TYPE = "type";

    private OsmNominatimSearchParser() {
    }

    @NonNull
    static List<Poi> parsePois(@NonNull String body) throws JSONException {
        JSONArray arr = new JSONArray(body);
        List<Poi> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            addPlacePois(out, arr.optJSONObject(i));
        }
        return out;
    }

    private static void addPlacePois(
            @NonNull List<Poi> out,
            @Nullable JSONObject object
    ) {
        if (object == null) {
            return;
        }

        List<PoiDetails.Entrance> entrances = parseEntrances(object);
        Poi basePoi = parseBasePoi(object, entrances);
        if (basePoi == null) {
            return;
        }
        out.add(basePoi);
        addEntrancePois(out, object, basePoi, entrances);
    }

    @Nullable
    private static Poi parseBasePoi(
            @NonNull JSONObject object,
            @NonNull List<PoiDetails.Entrance> entrances
    ) {
        String display = trimmedString(object, KEY_DISPLAY_NAME);
        String latStr = trimmedString(object, KEY_LAT);
        String lonStr = trimmedString(object, KEY_LON);
        if (display.isEmpty() || latStr.isEmpty() || lonStr.isEmpty()) {
            return null;
        }

        Coordinate coordinate = parseCoordinate(latStr, lonStr);
        if (coordinate == null) {
            return null;
        }
        return new Poi(display, coordinate.lat, coordinate.lon, parseBaseDetails(object, entrances));
    }

    @NonNull
    private static PoiDetails parseBaseDetails(
            @NonNull JSONObject object,
            @NonNull List<PoiDetails.Entrance> entrances
    ) {
        return new PoiDetails(
                parseStringMap(object.optJSONObject(KEY_ADDRESS)),
                parseStringMap(object.optJSONObject(KEY_EXTRA_TAGS)),
                entrances
        );
    }

    private static void addEntrancePois(
            @NonNull List<Poi> out,
            @NonNull JSONObject object,
            @NonNull Poi basePoi,
            @NonNull List<PoiDetails.Entrance> entrances
    ) {
        if (entrances.isEmpty() || singleEntranceMatchesBasePoi(basePoi, entrances)) {
            return;
        }
        Map<String, String> addressDetails = parseStringMap(object.optJSONObject(KEY_ADDRESS));
        for (PoiDetails.Entrance entrance : entrances) {
            out.add(entrancePoi(basePoi.displayLabel(), addressDetails, entrance));
        }
    }

    private static boolean singleEntranceMatchesBasePoi(
            @NonNull Poi basePoi,
            @NonNull List<PoiDetails.Entrance> entrances
    ) {
        if (entrances.size() != 1) {
            return false;
        }
        PoiDetails.Entrance entrance = entrances.get(0);
        return new Poi(basePoi.displayLabel(), entrance.lat, entrance.lon).stableKey()
                .equals(basePoi.stableKey());
    }

    @NonNull
    private static Poi entrancePoi(
            @NonNull String parentName,
            @NonNull Map<String, String> addressDetails,
            @NonNull PoiDetails.Entrance entrance
    ) {
        PoiDetails details = new PoiDetails(
                addressDetails,
                entrance.extraTags(),
                parentName,
                entrance.type()
        );
        return new Poi(parentName, entrance.lat, entrance.lon, details);
    }

    @NonNull
    private static List<PoiDetails.Entrance> parseEntrances(@NonNull JSONObject object) {
        JSONArray entrances = object.optJSONArray(KEY_ENTRANCES);
        List<PoiDetails.Entrance> out = new ArrayList<>();
        if (entrances == null) {
            return out;
        }
        for (int i = 0; i < entrances.length(); i++) {
            PoiDetails.Entrance entrance = parseEntrance(entrances.optJSONObject(i));
            if (entrance != null) {
                out.add(entrance);
            }
        }
        return out;
    }

    @Nullable
    private static PoiDetails.Entrance parseEntrance(@Nullable JSONObject entrance) {
        if (entrance == null) {
            return null;
        }
        Coordinate coordinate = parseCoordinate(
                trimmedString(entrance, KEY_LAT),
                trimmedString(entrance, KEY_LON)
        );
        return coordinate == null ? null : new PoiDetails.Entrance(
                coordinate.lat,
                coordinate.lon,
                trimmedString(entrance, KEY_TYPE),
                parseStringMap(entrance.optJSONObject(KEY_EXTRA_TAGS))
        );
    }

    @Nullable
    private static Coordinate parseCoordinate(
            @NonNull String latStr,
            @NonNull String lonStr
    ) {
        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            return LatLon.isValidCoordinate(lat, lon) ? new Coordinate(lat, lon) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @NonNull
    private static Map<String, String> parseStringMap(@Nullable JSONObject object) {
        Map<String, String> out = new LinkedHashMap<>();
        if (object == null) {
            return out;
        }
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            addStringEntry(out, object, keys.next());
        }
        return out;
    }

    private static void addStringEntry(
            @NonNull Map<String, String> out,
            @NonNull JSONObject object,
            @NonNull String key
    ) {
        String trimmedKey = key.trim();
        String value = trimmedString(object, key);
        if (!trimmedKey.isEmpty() && !value.isEmpty()) {
            out.put(trimmedKey, value);
        }
    }

    @NonNull
    private static String trimmedString(@NonNull JSONObject object, @NonNull String key) {
        return object.optString(key, "").trim();
    }

    private static final class Coordinate {
        final double lat;
        final double lon;

        Coordinate(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }
}
