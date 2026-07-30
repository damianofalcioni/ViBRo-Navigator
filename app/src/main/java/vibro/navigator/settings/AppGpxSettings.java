package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class AppGpxSettings {
    private static final String KEY_AUTO_SAVE_ON_STOP_ENABLED = "auto_save_gpx_on_stop_enabled";

    private AppGpxSettings() {
    }

    public static boolean isAutoSaveOnStopEnabled(@NonNull Context context) {
        return isAutoSaveOnStopEnabled(prefs(context));
    }

    static boolean isAutoSaveOnStopEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_AUTO_SAVE_ON_STOP_ENABLED, true);
    }

    public static void setAutoSaveOnStopEnabled(@NonNull Context context, boolean enabled) {
        setAutoSaveOnStopEnabled(prefs(context), enabled);
    }

    static void setAutoSaveOnStopEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_AUTO_SAVE_ON_STOP_ENABLED, enabled)
                .apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
