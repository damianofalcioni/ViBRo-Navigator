package vibro.navigator.settings;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public final class AppDataBackup {

    public static final String MIME_TYPE = "application/json";
    public static final String DEFAULT_FILE_NAME = "vibro-navigator-backup.json";

    private AppDataBackup() {
    }

    @NonNull
    public static String exportJson(@NonNull Context context) throws JSONException {
        JSONObject root = new JSONObject();
        root.put(AppDataBackupContract.KEY_SCHEMA_VERSION, AppDataBackupContract.SCHEMA_VERSION);
        root.put(AppDataBackupContract.KEY_SHARED_PREFERENCES, AppDataBackupPreferences.exportAll(context));
        return root.toString(2);
    }

    public static void importJson(@NonNull Context context, @NonNull String json) throws JSONException {
        List<AppDataBackupPreferenceFile> pendingPreferences = AppDataBackupImportParser.parse(json);
        if (!AppDataBackupPreferences.replaceAll(context, pendingPreferences)) {
            throw new JSONException("Failed to write backup preferences");
        }
        AppLogger.init(context);
    }
}
