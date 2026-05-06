package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class AppSettings {
    private static final String PREFS = "vibro.navigator.settings";
    private static final String KEY_USE_FUSED_LOCATION = "use_fused_location";

    private AppSettings() {
    }

    public static boolean isFusedLocationEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_USE_FUSED_LOCATION, true);
    }

    public static void setFusedLocationEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit()
                .putBoolean(KEY_USE_FUSED_LOCATION, enabled)
                .apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
