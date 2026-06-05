package vibro.navigator.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.geo.LatLon;

final class OsmMapPoiMarkerParser {
    @NonNull
    List<MapPoiMarker> parse(
            @NonNull String body,
            @NonNull List<MapPoiCategory> categories
    ) throws JSONException {
        JSONArray elements = new JSONObject(body).optJSONArray("elements");
        List<MapPoiMarker> markers = new ArrayList<>();
        if (elements == null) {
            return markers;
        }
        for (int i = 0; i < elements.length(); i++) {
            markers.addAll(parseElement(elements.optJSONObject(i), categories));
        }
        return markers;
    }

    @NonNull
    List<MapPoiMarker> parseElement(
            @Nullable JSONObject element,
            @NonNull List<MapPoiCategory> categories
    ) {
        List<MapPoiMarker> markers = new ArrayList<>();
        if (element == null) {
            return markers;
        }
        JSONObject tags = element.optJSONObject("tags");
        LatLon latLon = parseLatLon(element);
        if (tags == null || latLon == null) {
            return markers;
        }
        for (MapPoiCategory category : matchingCategories(tags, categories)) {
            markers.add(new MapPoiMarker(nameFor(tags), latLon.lat, latLon.lon, category));
        }
        return markers;
    }

    private static List<MapPoiCategory> matchingCategories(
            @NonNull JSONObject tags,
            @NonNull List<MapPoiCategory> categories
    ) {
        List<MapPoiCategory> matches = new ArrayList<>();
        for (MapPoiCategory category : categories) {
            if (category.matches(tags)) {
                matches.add(category);
            }
        }
        return matches;
    }

    @NonNull
    private static String nameFor(@Nullable JSONObject tags) {
        if (tags == null) {
            return "";
        }
        String name = tags.optString("name", "");
        if (!name.isEmpty()) {
            return name;
        }
        return tags.optString("brand", "");
    }

    @Nullable
    private static LatLon parseLatLon(@NonNull JSONObject element) {
        double lat = element.optDouble("lat", Double.NaN);
        double lon = element.optDouble("lon", Double.NaN);
        if (LatLon.isValidCoordinate(lat, lon)) {
            return new LatLon(lat, lon);
        }
        JSONObject center = element.optJSONObject("center");
        if (center == null) {
            return null;
        }
        lat = center.optDouble("lat", Double.NaN);
        lon = center.optDouble("lon", Double.NaN);
        return LatLon.isValidCoordinate(lat, lon) ? new LatLon(lat, lon) : null;
    }
}
