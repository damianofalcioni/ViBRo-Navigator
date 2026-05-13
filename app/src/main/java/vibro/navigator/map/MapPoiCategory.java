package vibro.navigator.map;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.Arrays;
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

    private MapPoiCategory(
            @NonNull String key,
            @NonNull String value,
            @NonNull String label,
            int count
    ) {
        this.key = key;
        this.value = value;
        this.id = key + "=" + value;
        this.label = label;
        this.count = count;
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
        return new MapPoiCategory(key, value, labelFor(key, value), count);
    }

    boolean matches(@NonNull JSONObject tags) {
        return value.equals(tags.optString(key, ""));
    }

    @NonNull
    List<String> overpassSelectors() {
        return Arrays.asList(exactSelector(key, value));
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
}
