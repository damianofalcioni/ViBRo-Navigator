package vibro.navigator.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Device-local folder grants are deliberately excluded from settings backups. */
public final class AppOutputFolderSettings {
    private static final String PREFS = "output_folders";

    public enum Kind { LOGS, GPX, BACKUP }

    private AppOutputFolderSettings() {
    }

    public static void restoreDefaults(@NonNull Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    @Nullable
    public static String get(@NonNull Context context, @NonNull Kind kind) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(kind.name(), null);
    }

    public static void set(@NonNull Context context, @NonNull Kind kind, @Nullable String uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(kind.name(), uri).apply();
    }
}
