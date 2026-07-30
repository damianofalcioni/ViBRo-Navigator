package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class AppCompassSettings {
    private static final String KEY_COMPASS_SURROUNDING_STREETS_ENABLED =
            "compass_surrounding_streets_enabled";
    private static final String KEY_COMPASS_INSTANT_ZOOM_ENABLED =
            "compass_instant_zoom_enabled";
    private static final String KEY_COMPASS_STATIONARY_FULL_ROUTE_ZOOM_ENABLED =
            "compass_stationary_full_route_zoom_enabled";
    private static final String KEY_COMPASS_FULLSCREEN_ROUTE_ENABLED =
            "compass_fullscreen_route_enabled";

    private AppCompassSettings() {
    }

    public static boolean isSurroundingStreetsEnabled(@NonNull Context context) {
        return isSurroundingStreetsEnabled(prefs(context));
    }

    static boolean isSurroundingStreetsEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_COMPASS_SURROUNDING_STREETS_ENABLED, true);
    }

    public static void setSurroundingStreetsEnabled(@NonNull Context context, boolean enabled) {
        setSurroundingStreetsEnabled(prefs(context), enabled);
    }

    static void setSurroundingStreetsEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_COMPASS_SURROUNDING_STREETS_ENABLED, enabled)
                .apply();
    }

    public static boolean isInstantZoomEnabled(@NonNull Context context) {
        return isInstantZoomEnabled(prefs(context));
    }

    static boolean isInstantZoomEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_COMPASS_INSTANT_ZOOM_ENABLED, false);
    }

    public static void setInstantZoomEnabled(@NonNull Context context, boolean enabled) {
        setInstantZoomEnabled(prefs(context), enabled);
    }

    static void setInstantZoomEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_COMPASS_INSTANT_ZOOM_ENABLED, enabled)
                .apply();
    }

    public static boolean isStationaryFullRouteZoomEnabled(@NonNull Context context) {
        return isStationaryFullRouteZoomEnabled(prefs(context));
    }

    static boolean isStationaryFullRouteZoomEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_COMPASS_STATIONARY_FULL_ROUTE_ZOOM_ENABLED, false);
    }

    public static void setStationaryFullRouteZoomEnabled(@NonNull Context context, boolean enabled) {
        setStationaryFullRouteZoomEnabled(prefs(context), enabled);
    }

    static void setStationaryFullRouteZoomEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_COMPASS_STATIONARY_FULL_ROUTE_ZOOM_ENABLED, enabled)
                .apply();
    }

    public static boolean isFullscreenRouteEnabled(@NonNull Context context) {
        return isFullscreenRouteEnabled(prefs(context));
    }

    static boolean isFullscreenRouteEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_COMPASS_FULLSCREEN_ROUTE_ENABLED, true);
    }

    public static void setFullscreenRouteEnabled(@NonNull Context context, boolean enabled) {
        setFullscreenRouteEnabled(prefs(context), enabled);
    }

    static void setFullscreenRouteEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_COMPASS_FULLSCREEN_ROUTE_ENABLED, enabled)
                .apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
