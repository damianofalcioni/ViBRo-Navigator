package vibro.navigator.settings;

import android.content.Context;

import androidx.annotation.NonNull;

public final class AppCompassSettings {
    private static final String KEY_COMPASS_SURROUNDING_STREETS_ENABLED =
            "compass_surrounding_streets_enabled";

    private AppCompassSettings() {
    }

    public static boolean isSurroundingStreetsEnabled(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_COMPASS_SURROUNDING_STREETS_ENABLED, false);
    }

    public static void setSurroundingStreetsEnabled(@NonNull Context context, boolean enabled) {
        context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_COMPASS_SURROUNDING_STREETS_ENABLED, enabled)
                .apply();
    }
}
