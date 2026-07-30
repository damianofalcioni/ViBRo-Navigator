package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AppNavigationCustomButtonSettings {
    private static final String KEY_NAVIGATION_CUSTOM_BUTTON_ENABLED = "navigation_custom_button_enabled";
    private static final String KEY_NAVIGATION_CUSTOM_BUTTON_TARGET = "navigation_custom_button_target";
    private static final Target DEFAULT_TARGET = Target.LIGHT_THEME;

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
        return isEnabled(prefs(context));
    }

    static boolean isEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_NAVIGATION_CUSTOM_BUTTON_ENABLED, true);
    }

    public static void setEnabled(@NonNull Context context, boolean enabled) {
        setEnabled(prefs(context), enabled);
    }

    static void setEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_NAVIGATION_CUSTOM_BUTTON_ENABLED, enabled)
                .apply();
    }

    @NonNull
    public static Target getTarget(@NonNull Context context) {
        return getTarget(prefs(context));
    }

    @NonNull
    static Target getTarget(@NonNull SharedPreferences preferences) {
        return Target.fromSerializedName(preferences.getString(KEY_NAVIGATION_CUSTOM_BUTTON_TARGET, null));
    }

    public static void setTarget(@NonNull Context context, @NonNull Target target) {
        setTarget(prefs(context), target);
    }

    static void setTarget(@NonNull SharedPreferences preferences, @NonNull Target target) {
        preferences.edit()
                .putString(KEY_NAVIGATION_CUSTOM_BUTTON_TARGET, target.serializedName())
                .apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
