package vibro.navigator.settings;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class AppDataBackupImportParser {

    private AppDataBackupImportParser() {
    }

    @NonNull
    static List<AppDataBackupPreferenceFile> parse(@NonNull String json) throws JSONException {
        JSONObject prefsRoot = readPreferencesRoot(json);
        List<AppDataBackupPreferenceFile> pending = new ArrayList<>();
        for (String prefsName : AppDataBackupContract.BACKED_UP_PREFERENCES) {
            JSONObject rawPrefs = requirePreferenceObject(prefsRoot, prefsName);
            pending.add(new AppDataBackupPreferenceFile(prefsName, parsePreferenceValues(rawPrefs)));
        }
        return pending;
    }

    @NonNull
    private static JSONObject readPreferencesRoot(@NonNull String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (root.optInt(AppDataBackupContract.KEY_SCHEMA_VERSION, -1) != AppDataBackupContract.SCHEMA_VERSION) {
            throw new JSONException("Unsupported backup schema version");
        }
        JSONObject prefsRoot = root.optJSONObject(AppDataBackupContract.KEY_SHARED_PREFERENCES);
        if (prefsRoot == null) {
            throw new JSONException("Missing sharedPreferences object");
        }
        return prefsRoot;
    }

    @NonNull
    private static JSONObject requirePreferenceObject(
            @NonNull JSONObject prefsRoot,
            @NonNull String prefsName
    ) throws JSONException {
        Object rawPrefs = prefsRoot.opt(prefsName);
        if (rawPrefs instanceof JSONObject) {
            return (JSONObject) rawPrefs;
        }
        throw new JSONException("Missing or invalid backup preferences object " + prefsName);
    }

    @NonNull
    private static List<AppDataBackupPreferenceValue> parsePreferenceValues(@NonNull JSONObject rawPrefs)
            throws JSONException {
        List<AppDataBackupPreferenceValue> values = new ArrayList<>();
        Iterator<String> keys = rawPrefs.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            values.add(AppDataBackupValueJson.fromJson(key, rawPrefs.getJSONObject(key)));
        }
        return values;
    }
}
