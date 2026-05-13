package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

final class AppDataBackupPreferences {

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

    static void clearAll(@NonNull Context context) {
        for (String prefsName : AppDataBackupContract.BACKED_UP_PREFERENCES) {
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    static void applyAll(
            @NonNull Context context,
            @NonNull List<AppDataBackupPreferenceFile> preferenceFiles
    ) {
        for (AppDataBackupPreferenceFile preferenceFile : preferenceFiles) {
            applyPreferenceFile(context, preferenceFile);
        }
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

    private static void applyPreferenceFile(
            @NonNull Context context,
            @NonNull AppDataBackupPreferenceFile preferenceFile
    ) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                preferenceFile.prefsName,
                Context.MODE_PRIVATE
        ).edit();
        for (AppDataBackupPreferenceValue value : preferenceFile.values) {
            AppDataBackupValueWriter.put(editor, value);
        }
        editor.commit();
    }
}
