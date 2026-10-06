package vibro.navigator.settings;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class AppDataBackup {

    public static final String MIME_TYPE = "application/json";

    private AppDataBackup() {
    }

    @NonNull
    public static String defaultFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date());
        return "vibro-navigator-backup-" + timestamp + ".json";
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
