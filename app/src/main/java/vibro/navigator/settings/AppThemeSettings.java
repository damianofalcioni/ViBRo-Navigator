package vibro.navigator.settings;

import android.content.Context;

import androidx.annotation.NonNull;

public final class AppThemeSettings {
    private static final String KEY_LIGHT_THEME = "light_theme";

    private AppThemeSettings() {
    }

    public static boolean isLightThemeEnabled(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_LIGHT_THEME, false);
    }

    public static void setLightThemeEnabled(@NonNull Context context, boolean enabled) {
        context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LIGHT_THEME, enabled)
                .apply();
    }
}
