package vibro.navigator.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class OsmMapPoiCategoryParser {
    @NonNull
    List<MapPoiCategory> parse(@NonNull String body) throws JSONException {
        JSONArray elements = new JSONObject(body).optJSONArray("elements");
        Map<String, CategoryCount> counts = new LinkedHashMap<>();
        if (elements == null) {
            return new ArrayList<>();
        }
        for (int i = 0; i < elements.length(); i++) {
            collectCategories(elements.optJSONObject(i), counts);
        }
        return sortedCategories(counts);
    }

    private static void collectCategories(
            @Nullable JSONObject element,
            @NonNull Map<String, CategoryCount> counts
    ) {
        JSONObject tags = element != null ? element.optJSONObject("tags") : null;
        if (tags == null) {
            return;
        }
        for (String key : MapPoiCategory.discoveryKeys()) {
            collectCategory(tags, key, counts);
        }
        collectCategory(tags, "highway", counts);
        collectCategory(tags, "railway", counts);
    }

    private static void collectCategory(
            @NonNull JSONObject tags,
            @NonNull String key,
            @NonNull Map<String, CategoryCount> counts
    ) {
        String value = tags.optString(key, "");
        if (value.isEmpty()) {
            return;
        }
        String id = key + "=" + value;
        CategoryCount count = counts.get(id);
        if (count == null) {
            counts.put(id, new CategoryCount(key, value));
            return;
        }
        count.count += 1;
    }

    @NonNull
    private static List<MapPoiCategory> sortedCategories(@NonNull Map<String, CategoryCount> counts) {
        List<MapPoiCategory> categories = new ArrayList<>();
        for (CategoryCount count : counts.values()) {
            categories.add(count.category());
        }
        Collections.sort(categories, (left, right) -> left.label.compareToIgnoreCase(right.label));
        return categories;
    }

    private static final class CategoryCount {
        @NonNull
        final String key;
        @NonNull
        final String value;
        int count = 1;

        private CategoryCount(@NonNull String key, @NonNull String value) {
            this.key = key;
            this.value = value;
        }

        @NonNull
        MapPoiCategory category() {
            return MapPoiCategory.fromTag(key, value, count);
        }
    }
}
