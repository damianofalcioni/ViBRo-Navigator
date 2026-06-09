package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AppDataBackupPreferences {
    private static final String TAG = "AppDataBackup";

    private AppDataBackupPreferences() {
    }

    @NonNull
    static JSONObject exportAll(@NonNull Context context) throws JSONException {
        JSONObject all = new JSONObject();
        for (String prefsName : AppDataBackupContract.BACKED_UP_PREFERENCES) {
            all.put(prefsName, exportPreferenceFile(context, prefsName));
        }
        return all;
    }

    static boolean replaceAll(
            @NonNull Context context,
            @NonNull List<AppDataBackupPreferenceFile> preferenceFiles
    ) {
        List<AppDataBackupPreferenceFile> originalPreferences = snapshotAll(context);
        if (replacePreferenceFiles(context, preferenceFiles)) {
            return true;
        }
        if (!replacePreferenceFiles(context, originalPreferences)) {
            AppLogger.w(TAG, "Failed to roll back preferences after backup import write failure");
        }
        return false;
    }

    @NonNull
    private static List<AppDataBackupPreferenceFile> snapshotAll(@NonNull Context context) {
        List<AppDataBackupPreferenceFile> snapshots = new ArrayList<>();
        for (String prefsName : AppDataBackupContract.BACKED_UP_PREFERENCES) {
            snapshots.add(snapshotPreferenceFile(context, prefsName));
        }
        return snapshots;
    }

    @NonNull
    private static AppDataBackupPreferenceFile snapshotPreferenceFile(
            @NonNull Context context,
            @NonNull String prefsName
    ) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        List<AppDataBackupPreferenceValue> values = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            values.add(new AppDataBackupPreferenceValue(entry.getKey(), snapshotValue(entry.getValue())));
        }
        return new AppDataBackupPreferenceFile(prefsName, values);
    }

    @NonNull
    private static Object snapshotValue(@NonNull Object value) {
        if (value instanceof Set<?>) {
            return stringSetSnapshot((Set<?>) value);
        }
        return value;
    }

    @NonNull
    private static Set<String> stringSetSnapshot(@NonNull Set<?> rawValues) {
        Set<String> values = new HashSet<>();
        for (Object rawValue : rawValues) {
            if (rawValue instanceof String) {
                values.add((String) rawValue);
            }
        }
        return values;
    }

    private static boolean replacePreferenceFiles(
            @NonNull Context context,
            @NonNull List<AppDataBackupPreferenceFile> preferenceFiles
    ) {
        for (AppDataBackupPreferenceFile preferenceFile : preferenceFiles) {
            if (!replacePreferenceFile(context, preferenceFile)) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    private static JSONObject exportPreferenceFile(@NonNull Context context, @NonNull String prefsName)
            throws JSONException {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        JSONObject out = new JSONObject();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            out.put(entry.getKey(), AppDataBackupValueJson.toJson(entry.getValue()));
        }
        return out;
    }

    private static boolean replacePreferenceFile(
            @NonNull Context context,
            @NonNull AppDataBackupPreferenceFile preferenceFile
    ) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                preferenceFile.prefsName,
                Context.MODE_PRIVATE
        ).edit().clear();
        for (AppDataBackupPreferenceValue value : preferenceFile.values) {
            AppDataBackupValueWriter.put(editor, value);
        }
        return editor.commit();
    }
}
