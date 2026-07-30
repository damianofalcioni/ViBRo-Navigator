package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class AppLocationSettings {
    private static final String KEY_DYNAMIC_GPS_FIX_INTERVAL_ENABLED = "dynamic_gps_fix_interval_enabled";

    private AppLocationSettings() {
    }

    public static boolean isDynamicGpsFixIntervalEnabled(@NonNull Context context) {
        return isDynamicGpsFixIntervalEnabled(prefs(context));
    }

    static boolean isDynamicGpsFixIntervalEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_DYNAMIC_GPS_FIX_INTERVAL_ENABLED, true);
    }

    public static void setDynamicGpsFixIntervalEnabled(@NonNull Context context, boolean enabled) {
        setDynamicGpsFixIntervalEnabled(prefs(context), enabled);
    }

    static void setDynamicGpsFixIntervalEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_DYNAMIC_GPS_FIX_INTERVAL_ENABLED, enabled)
                .apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
