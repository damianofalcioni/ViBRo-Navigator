package vibro.navigator.brouter;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vibro.navigator.logging.AppLogger;

final class BRouterProfileParameterStore {

    private static final String KEY_PROFILE_PARAM_NAMES_PREFIX = "profile_param_names:";
    private static final String KEY_PROFILE_PARAM_VALUE_PREFIX = "profile_param_value:";
    private static final String UTF_8 = "UTF-8";
    private static final String TAG = "BRouterProfiles";

    @NonNull
    private final String prefsName;

    BRouterProfileParameterStore(@NonNull String prefsName) {
        this.prefsName = prefsName;
    }

    @NonNull
    Map<String, String> getOverrides(@NonNull Context context, @NonNull String profileName) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        Set<String> names = prefs.getStringSet(profileParamNamesKey(profileName), Collections.emptySet());
        if (names == null || names.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> values = new HashMap<>();
        for (String name : names) {
            String value = prefs.getString(profileParamValueKey(profileName, name), null);
            if (value != null) {
                values.put(name, value);
            }
        }
        return Collections.unmodifiableMap(values);
    }

    void saveValues(
            @NonNull Context context,
            @NonNull String profileName,
            @NonNull List<BRouterProfileParameter> parameters,
            @NonNull Map<String, String> values
    ) {
        Map<String, String> overrides = profileParameterOverrides(parameters, values);
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        removeSavedValues(prefs, editor, profileName);
        Set<String> names = new HashSet<>(overrides.keySet());
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            editor.putString(profileParamValueKey(profileName, entry.getKey()), entry.getValue());
        }
        editor.putStringSet(profileParamNamesKey(profileName), names);
        editor.apply();
        AppLogger.i(TAG, "Saved profile parameter overrides profile=" + profileName
                + " count=" + overrides.size());
    }

    void reset(@NonNull Context context, @NonNull String profileName) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        removeSavedValues(prefs, editor, profileName);
        editor.apply();
        AppLogger.i(TAG, "Reset profile parameter overrides profile=" + profileName);
    }

    @NonNull
    private static Map<String, String> profileParameterOverrides(
            @NonNull List<BRouterProfileParameter> parameters,
            @NonNull Map<String, String> values
    ) {
        Map<String, String> overrides = new HashMap<>();
        for (BRouterProfileParameter parameter : parameters) {
            String value = values.get(parameter.name);
            if (value != null && !value.equals(parameter.defaultValue)) {
                overrides.put(parameter.name, value);
            }
        }
        return overrides;
    }

    private static void removeSavedValues(
            @NonNull SharedPreferences prefs,
            @NonNull SharedPreferences.Editor editor,
            @NonNull String profileName
    ) {
        Set<String> oldNames = prefs.getStringSet(profileParamNamesKey(profileName), Collections.emptySet());
        if (oldNames != null) {
            for (String name : oldNames) {
                editor.remove(profileParamValueKey(profileName, name));
            }
        }
        editor.remove(profileParamNamesKey(profileName));
    }

    @NonNull
    private static String profileParamNamesKey(@NonNull String profileName) {
        return KEY_PROFILE_PARAM_NAMES_PREFIX + encodeKeyPart(profileName);
    }

    @NonNull
    private static String profileParamValueKey(@NonNull String profileName, @NonNull String parameterName) {
        return KEY_PROFILE_PARAM_VALUE_PREFIX
                + encodeKeyPart(profileName)
                + ":"
                + encodeKeyPart(parameterName);
    }

    @NonNull
    private static String encodeKeyPart(@NonNull String value) {
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding unavailable", e);
        }
    }
}
