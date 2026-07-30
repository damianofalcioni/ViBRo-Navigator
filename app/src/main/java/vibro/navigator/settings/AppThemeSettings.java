package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class AppThemeSettings {
    private static final String KEY_LIGHT_THEME = "light_theme";

    private AppThemeSettings() {
    }

    public static boolean isLightThemeEnabled(@NonNull Context context) {
        return isLightThemeEnabled(prefs(context));
    }

    static boolean isLightThemeEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_LIGHT_THEME, false);
    }

    public static void setLightThemeEnabled(@NonNull Context context, boolean enabled) {
        setLightThemeEnabled(prefs(context), enabled);
    }

    static void setLightThemeEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_LIGHT_THEME, enabled)
                .apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
