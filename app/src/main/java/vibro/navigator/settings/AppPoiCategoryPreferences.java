package vibro.navigator.settings;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

final class AppPoiCategoryPreferences {
    private static final String KEY_MAP_POI_CATEGORY_FILTER_ENABLED = "map_poi_category_filter_enabled";
    private static final String KEY_MAP_POI_CATEGORY_NAMES = "map_poi_category_names";
    private static final String[] DEFAULT_MAP_POI_CATEGORY_NAMES = {
            "Bicycle Repair Station",
            "Drinking Water",
            "Fuel",
            "Hospital",
            "Parking",
            "Pharmacy",
            "Police",
            "Public Transport Stop Position",
            "Supermarket Shop",
            "Taxi",
            "Toilets"
    };

    private AppPoiCategoryPreferences() {
    }

    static boolean isMapPoiCategoryFilterEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_MAP_POI_CATEGORY_FILTER_ENABLED, true);
    }

    static void setMapPoiCategoryFilterEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_MAP_POI_CATEGORY_FILTER_ENABLED, enabled)
                .apply();
    }

    @NonNull
    static List<String> getMapPoiCategoryNames(@NonNull SharedPreferences preferences) {
        List<String> names = new ArrayList<>();
        for (AppPoiCategorySetting setting : getMapPoiCategorySettings(preferences)) {
            names.add(setting.name);
        }
        return names;
    }

    @NonNull
    static List<String> getEnabledMapPoiCategoryNames(@NonNull SharedPreferences preferences) {
        List<String> names = new ArrayList<>();
        for (AppPoiCategorySetting setting : getMapPoiCategorySettings(preferences)) {
            if (setting.enabled) {
                names.add(setting.name);
            }
        }
        return names;
    }

    @NonNull
    static List<AppPoiCategorySetting> getMapPoiCategorySettings(@NonNull SharedPreferences preferences) {
        String payload = preferences.getString(KEY_MAP_POI_CATEGORY_NAMES, null);
        return AppPoiCategorySettingsJson.parseOrDefault(payload, defaultMapPoiCategorySettings());
    }

    static void setMapPoiCategorySettings(
            @NonNull SharedPreferences preferences,
            @NonNull List<AppPoiCategorySetting> settings
    ) {
        preferences.edit()
                .putString(KEY_MAP_POI_CATEGORY_NAMES, AppPoiCategorySettingsJson.toJson(settings))
                .apply();
    }

    static void setMapPoiCategoryNames(@NonNull SharedPreferences preferences, @NonNull List<String> names) {
        List<AppPoiCategorySetting> settings = new ArrayList<>();
        for (String name : names) {
            settings.add(new AppPoiCategorySetting(name, true));
        }
        setMapPoiCategorySettings(preferences, settings);
    }

    @NonNull
    private static List<AppPoiCategorySetting> defaultMapPoiCategorySettings() {
        List<AppPoiCategorySetting> settings = new ArrayList<>();
        for (String name : DEFAULT_MAP_POI_CATEGORY_NAMES) {
            settings.add(new AppPoiCategorySetting(name, true));
        }
        return settings;
    }
}
