package vibro.navigator.map;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class MapPoiCategory {
    private static final String SELECTOR_PREFIX = "[\"";
    private static final String SELECTOR_SUFFIX = "\"]";
    private static final String KEY_AMENITY = "amenity";
    private static final String KEY_SHOP = "shop";
    private static final String KEY_TOURISM = "tourism";
    private static final String KEY_LEISURE = "leisure";
    private static final String KEY_RAILWAY = "railway";
    private static final String KEY_HIGHWAY = "highway";
    private static final String KEY_PUBLIC_TRANSPORT = "public_transport";

    @NonNull
    final String id;
    @NonNull
    final String label;
    final int count;
    @NonNull
    final String key;
    @NonNull
    final String value;
    @NonNull
    private final List<Tag> tags;

    private MapPoiCategory(
            @NonNull String id,
            @NonNull String key,
            @NonNull String value,
            @NonNull String label,
            int count,
            @NonNull List<Tag> tags
    ) {
        this.key = key;
        this.value = value;
        this.id = id;
        this.label = label;
        this.count = count;
        this.tags = tags;
    }

    @NonNull
    static List<String> discoveryKeys() {
        return Arrays.asList(
                KEY_AMENITY,
                KEY_SHOP,
                KEY_TOURISM,
                KEY_LEISURE,
                KEY_PUBLIC_TRANSPORT
        );
    }

    @NonNull
    static List<String> discoverySelectors() {
        return Arrays.asList(
                keySelector(KEY_AMENITY),
                keySelector(KEY_SHOP),
                keySelector(KEY_TOURISM),
                keySelector(KEY_LEISURE),
                keySelector(KEY_PUBLIC_TRANSPORT),
                exactSelector(KEY_HIGHWAY, "bus_stop"),
                regexSelector(KEY_RAILWAY, "^(station|halt|tram_stop)$")
        );
    }

    @NonNull
    static MapPoiCategory fromTag(@NonNull String key, @NonNull String value) {
        return fromTag(key, value, 0);
    }

    @NonNull
    static MapPoiCategory fromTag(@NonNull String key, @NonNull String value, int count) {
        return new MapPoiCategory(
                key + "=" + value,
                key,
                value,
                labelFor(key, value),
                count,
                Collections.singletonList(new Tag(key, value))
        );
    }

    @NonNull
    MapPoiCategory withCount(int nextCount) {
        return new MapPoiCategory(id, key, value, label, nextCount, tags);
    }

    @NonNull
    static MapPoiCategory fromName(@NonNull String name, @NonNull List<Tag> tags) {
        String trimmed = name.trim();
        String value = tags.isEmpty() ? normalizeValue(trimmed) : tags.get(0).value;
        return new MapPoiCategory(
                "name=" + normalizeValue(trimmed),
                tags.isEmpty() ? KEY_AMENITY : tags.get(0).key,
                value,
                labelFromName(trimmed),
                0,
                new ArrayList<>(tags)
        );
    }

    boolean matches(@NonNull JSONObject tags) {
        for (Tag tag : this.tags) {
            if (tag.matches(tags)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    List<String> overpassSelectors() {
        List<String> selectors = new ArrayList<>();
        for (Tag tag : tags) {
            selectors.add(exactSelector(tag.key, tag.value));
        }
        return selectors;
    }

    @NonNull
    private static String labelFor(@NonNull String key, @NonNull String value) {
        String base = titleize(value);
        if (KEY_SHOP.equals(key)) {
            return base + " Shop";
        }
        if (KEY_PUBLIC_TRANSPORT.equals(key)) {
            return "Public Transport " + base;
        }
        return base;
    }

    @NonNull
    private static String titleize(@NonNull String value) {
        String[] parts = value.replace('-', '_').split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(part.substring(0, 1).toUpperCase(Locale.US));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.length() > 0 ? out.toString() : value;
    }

    @NonNull
    private static String labelFromName(@NonNull String name) {
        String normalized = normalizeValue(name);
        return normalized.isEmpty() ? name : titleize(normalized);
    }

    @NonNull
    static String normalizeValue(@NonNull String value) {
        String normalized = value.trim().toLowerCase(Locale.US).replace('-', '_');
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        return normalized.replaceAll("^_+|_+$", "");
    }

    @NonNull
    private static String keySelector(@NonNull String key) {
        return SELECTOR_PREFIX + key + SELECTOR_SUFFIX;
    }

    @NonNull
    private static String exactSelector(@NonNull String key, @NonNull String value) {
        return "[\"" + key + "\"=\"" + value + "\"]";
    }

    @NonNull
    private static String regexSelector(@NonNull String key, @NonNull String valueRegex) {
        return "[\"" + key + "\"~\"" + valueRegex + "\"]";
    }

    static final class Tag {
        @NonNull
        final String key;
        @NonNull
        final String value;

        Tag(@NonNull String key, @NonNull String value) {
            this.key = key;
            this.value = value;
        }

        boolean matches(@NonNull JSONObject tags) {
            return value.equals(tags.optString(key, ""));
        }
    }
}
