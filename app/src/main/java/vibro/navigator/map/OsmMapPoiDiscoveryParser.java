package vibro.navigator.map;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class OsmMapPoiDiscoveryParser {
    @NonNull
    private final OsmMapPoiCategoryParser categoryParser = new OsmMapPoiCategoryParser();
    @NonNull
    private final OsmMapPoiMarkerParser markerParser = new OsmMapPoiMarkerParser();

    @NonNull
    OsmMapPoiDiscoveryResult parse(@NonNull String body) throws JSONException {
        List<MapPoiCategory> categories = categoryParser.parse(body);
        return new OsmMapPoiDiscoveryResult(categories, parseMarkers(body, categories));
    }

    @NonNull
    private List<MapPoiMarker> parseMarkers(
            @NonNull String body,
            @NonNull List<MapPoiCategory> categories
    ) throws JSONException {
        JSONArray elements = new JSONObject(body).optJSONArray("elements");
        List<MapPoiMarker> markers = new ArrayList<>();
        if (elements == null) {
            return markers;
        }
        for (int i = 0; i < elements.length(); i++) {
            markers.addAll(markerParser.parseElement(elements.optJSONObject(i), categories));
        }
        return markers;
    }
}
