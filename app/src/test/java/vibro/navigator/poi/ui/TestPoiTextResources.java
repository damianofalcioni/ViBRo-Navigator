package vibro.navigator.poi.ui;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import vibro.navigator.R;

final class TestPoiTextResources implements PoiTextResources {
    private static final Map<Integer, String> STRINGS = buildStrings();

    @NonNull
    @Override
    public String getString(int resId, Object... formatArgs) {
        String pattern = STRINGS.get(resId);
        if (pattern == null) {
            throw new IllegalArgumentException("Unhandled test string resource: " + resId);
        }
        return formatArgs.length == 0 ? pattern : String.format(Locale.US, pattern, formatArgs);
    }

    @NonNull
    private static Map<Integer, String> buildStrings() {
        Map<Integer, String> strings = new HashMap<>();
        strings.put(R.string.action_search_google_maps, "Search in Google Maps");
        strings.put(R.string.label_poi_entrance, "Entrance");
        strings.put(R.string.label_poi_entrances, "Entrances");
        strings.put(R.string.label_poi_entrance_type, "Type");
        strings.put(R.string.label_poi_coordinates, "Coordinates");
        strings.put(R.string.label_poi_extra_tags, "Extra tags");
        strings.put(R.string.label_poi_address_details, "Address");
        strings.put(R.string.format_poi_entrance_label, "%1$s - %2$s");
        strings.put(R.string.format_poi_entrance_heading, "Entrance %1$d");
        strings.put(R.string.format_poi_entrance_type, "Entrance: %1$s");
        strings.put(R.string.format_poi_detail_pair, "%1$s: %2$s");
        strings.put(R.string.msg_poi_details_unavailable, "No extra details available.");
        strings.put(
                R.string.msg_poi_details_map_check_hint,
                "Use the map to double-check that this is the intended location."
        );
        strings.put(R.string.format_coordinates, "%1$.6f, %2$.6f");
        return strings;
    }
}
