package vibro.navigator.settings;

import android.content.Context;

import androidx.annotation.NonNull;

public final class AppCompassSettings {
    private static final String KEY_COMPASS_SURROUNDING_STREETS_ENABLED =
            "compass_surrounding_streets_enabled";
    private static final String KEY_COMPASS_INSTANT_ZOOM_ENABLED =
            "compass_instant_zoom_enabled";

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

    public static boolean isInstantZoomEnabled(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_COMPASS_INSTANT_ZOOM_ENABLED, false);
    }

    public static void setInstantZoomEnabled(@NonNull Context context, boolean enabled) {
        context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_COMPASS_INSTANT_ZOOM_ENABLED, enabled)
                .apply();
    }
}
