package vibro.navigator.settings;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;

/** Restores preference fallbacks without removing routes, search history, or BRouter data. */
public final class AppSettingsDefaults {
    private AppSettingsDefaults() {
    }

    public static void restore(@NonNull Context context) {
        context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE).edit().clear().apply();
        AppOutputFolderSettings.restoreDefaults(context);
        context.getSharedPreferences(AppDataBackupContract.PREFS_LOGGING, Context.MODE_PRIVATE)
                .edit().clear().apply();
        AppLogger.init(context);
    }
}
