package vibro.navigator.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class AppPoiCategorySettingsJson {
    private static final String KEY_NAME = "name";
    private static final String KEY_ENABLED = "enabled";

    private AppPoiCategorySettingsJson() {
    }

    @NonNull
    static List<AppPoiCategorySetting> parseOrDefault(
            @Nullable String payload,
            @NonNull List<AppPoiCategorySetting> defaults
    ) {
        if (payload == null || payload.trim().isEmpty()) {
            return new ArrayList<>(defaults);
        }
        try {
            return sanitize(parseArray(new JSONArray(payload)));
        } catch (JSONException ignored) {
            return new ArrayList<>(defaults);
        }
    }

    @NonNull
    static List<AppPoiCategorySetting> sanitize(@NonNull List<AppPoiCategorySetting> settings) {
        List<AppPoiCategorySetting> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (AppPoiCategorySetting setting : settings) {
            String trimmed = setting.name.trim();
            String key = trimmed.toLowerCase(Locale.US);
            if (!trimmed.isEmpty() && seen.add(key)) {
                out.add(new AppPoiCategorySetting(trimmed, setting.enabled));
            }
        }
        return out;
    }

    @NonNull
    static String toJson(@NonNull List<AppPoiCategorySetting> settings) {
        JSONArray array = new JSONArray();
        for (AppPoiCategorySetting setting : sanitize(settings)) {
            array.put(toJson(setting));
        }
        return array.toString();
    }

    @NonNull
    private static List<AppPoiCategorySetting> parseArray(@NonNull JSONArray array) {
        List<AppPoiCategorySetting> settings = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object item = array.opt(i);
            if (item instanceof JSONObject) {
                settings.add(parseObject((JSONObject) item));
            } else {
                settings.add(new AppPoiCategorySetting(array.optString(i, ""), true));
            }
        }
        return settings;
    }

    @NonNull
    private static AppPoiCategorySetting parseObject(@NonNull JSONObject object) {
        return new AppPoiCategorySetting(
                object.optString(KEY_NAME, ""),
                object.optBoolean(KEY_ENABLED, true)
        );
    }

    @NonNull
    private static JSONObject toJson(@NonNull AppPoiCategorySetting setting) {
        JSONObject object = new JSONObject();
        try {
            object.put(KEY_NAME, setting.name);
            object.put(KEY_ENABLED, setting.enabled);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return object;
    }
}
