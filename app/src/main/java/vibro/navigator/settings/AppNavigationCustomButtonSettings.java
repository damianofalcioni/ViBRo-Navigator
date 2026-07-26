package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AppNavigationCustomButtonSettings {
    private static final String KEY_NAVIGATION_CUSTOM_BUTTON_ENABLED = "navigation_custom_button_enabled";
    private static final String KEY_NAVIGATION_CUSTOM_BUTTON_TARGET = "navigation_custom_button_target";
    private static final Target DEFAULT_TARGET = Target.DYNAMIC_GPS_INTERVAL;

    private AppNavigationCustomButtonSettings() {
    }

    public enum Target {
        DYNAMIC_GPS_INTERVAL("dynamic_gps_interval"),
        LIGHT_THEME("light_theme"),
        SURROUNDING_STREETS("surrounding_streets"),
        FULLSCREEN_ROUTE("fullscreen_route"),
        NOTIFICATIONS("notifications"),
        SPEECH_DIRECTIONS("speech_directions");

        @NonNull
        private final String serializedName;

        Target(@NonNull String serializedName) {
            this.serializedName = serializedName;
        }

        @NonNull
        public String serializedName() {
            return serializedName;
        }

        @NonNull
        static Target fromSerializedName(@Nullable String serializedName) {
            if (serializedName != null) {
                for (Target target : values()) {
                    if (target.serializedName.equals(serializedName)) {
                        return target;
                    }
                }
            }
            return DEFAULT_TARGET;
        }
    }

    public static boolean isEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_NAVIGATION_CUSTOM_BUTTON_ENABLED, false);
    }

    public static void setEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit()
                .putBoolean(KEY_NAVIGATION_CUSTOM_BUTTON_ENABLED, enabled)
                .apply();
    }

    @NonNull
    public static Target getTarget(@NonNull Context context) {
        return Target.fromSerializedName(prefs(context).getString(KEY_NAVIGATION_CUSTOM_BUTTON_TARGET, null));
    }

    public static void setTarget(@NonNull Context context, @NonNull Target target) {
        prefs(context).edit()
                .putString(KEY_NAVIGATION_CUSTOM_BUTTON_TARGET, target.serializedName())
                .apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
