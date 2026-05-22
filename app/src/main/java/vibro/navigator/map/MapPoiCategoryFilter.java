package vibro.navigator.map;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.settings.AppSettings;

final class MapPoiCategoryFilter {
    private static final String SUFFIX_SHOP = "_shop";
    private static final String PREFIX_PUBLIC_TRANSPORT = "public_transport_";

    private static final String KEY_AMENITY = "amenity";
    private static final String KEY_SHOP = "shop";
    private static final String KEY_TOURISM = "tourism";
    private static final String KEY_LEISURE = "leisure";
    private static final String KEY_PUBLIC_TRANSPORT = "public_transport";
    private static final String KEY_HIGHWAY = "highway";
    private static final String KEY_RAILWAY = "railway";

    private static final String VALUE_BUS_STOP = "bus_stop";
    private static final String VALUE_STATION = "station";
    private static final String VALUE_HALT = "halt";
    private static final String VALUE_TRAM_STOP = "tram_stop";

    private final boolean enabled;
    @NonNull
    private final List<MapPoiCategory> categories;

    private MapPoiCategoryFilter(boolean enabled, @NonNull List<MapPoiCategory> categories) {
        this.enabled = enabled;
        this.categories = categories;
    }

    @NonNull
    static MapPoiCategoryFilter fromSettings(@NonNull Context context) {
        return new MapPoiCategoryFilter(
                AppSettings.isMapPoiCategoryFilterEnabled(context),
                fromNames(AppSettings.getEnabledMapPoiCategoryNames(context))
        );
    }

    @NonNull
    static List<MapPoiCategory> fromNames(@NonNull List<String> names) {
        List<MapPoiCategory> categories = new ArrayList<>();
        for (String name : names) {
            String normalized = MapPoiCategory.normalizeValue(name);
            if (!normalized.isEmpty()) {
                categories.add(MapPoiCategory.fromName(name, tagsFor(normalized)));
            }
        }
        return categories;
    }

    boolean isEnabled() {
        return enabled;
    }

    boolean hasCategories() {
        return !categories.isEmpty();
    }

    @NonNull
    List<MapPoiCategory> categories() {
        return categories;
    }

    @NonNull
    private static List<MapPoiCategory.Tag> tagsFor(@NonNull String normalized) {
        List<MapPoiCategory.Tag> tags = new ArrayList<>();
        addSpecialNameTags(tags, normalized);
        addNameTags(tags, normalized);
        return tags;
    }

    private static void addSpecialNameTags(
            @NonNull List<MapPoiCategory.Tag> tags,
            @NonNull String normalized
    ) {
        if (normalized.endsWith(SUFFIX_SHOP) && normalized.length() > SUFFIX_SHOP.length()) {
            tags.add(new MapPoiCategory.Tag(
                    KEY_SHOP,
                    normalized.substring(0, normalized.length() - SUFFIX_SHOP.length())
            ));
        }
        if (normalized.startsWith(PREFIX_PUBLIC_TRANSPORT)
                && normalized.length() > PREFIX_PUBLIC_TRANSPORT.length()) {
            tags.add(new MapPoiCategory.Tag(
                    KEY_PUBLIC_TRANSPORT,
                    normalized.substring(PREFIX_PUBLIC_TRANSPORT.length())
            ));
        }
    }

    private static void addNameTags(
            @NonNull List<MapPoiCategory.Tag> tags,
            @NonNull String normalized
    ) {
        tags.add(new MapPoiCategory.Tag(KEY_AMENITY, normalized));
        tags.add(new MapPoiCategory.Tag(KEY_SHOP, normalized));
        tags.add(new MapPoiCategory.Tag(KEY_TOURISM, normalized));
        tags.add(new MapPoiCategory.Tag(KEY_LEISURE, normalized));
        tags.add(new MapPoiCategory.Tag(KEY_PUBLIC_TRANSPORT, normalized));
        addTransportTag(tags, normalized);
    }

    private static void addTransportTag(
            @NonNull List<MapPoiCategory.Tag> tags,
            @NonNull String normalized
    ) {
        if (VALUE_BUS_STOP.equals(normalized)) {
            tags.add(new MapPoiCategory.Tag(KEY_HIGHWAY, VALUE_BUS_STOP));
        } else if (VALUE_STATION.equals(normalized)
                || VALUE_HALT.equals(normalized)
                || VALUE_TRAM_STOP.equals(normalized)) {
            tags.add(new MapPoiCategory.Tag(KEY_RAILWAY, normalized));
        }
    }
}
