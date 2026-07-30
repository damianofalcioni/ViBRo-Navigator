package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class AppNotificationSettings {
    private static final String KEY_NAVIGATION_NOTIFICATIONS_ENABLED = "navigation_notifications_enabled";
    private static final String KEY_SINGLE_INSTRUCTION_MODE_ENABLED = "single_instruction_mode_enabled";

    private AppNotificationSettings() {
    }

    public static boolean areNavigationNotificationsEnabled(@NonNull Context context) {
        return areNavigationNotificationsEnabled(prefs(context));
    }

    static boolean areNavigationNotificationsEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_NAVIGATION_NOTIFICATIONS_ENABLED, true);
    }

    public static void setNavigationNotificationsEnabled(@NonNull Context context, boolean enabled) {
        setNavigationNotificationsEnabled(prefs(context), enabled);
    }

    static void setNavigationNotificationsEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_NAVIGATION_NOTIFICATIONS_ENABLED, enabled)
                .apply();
    }

    public static boolean isSingleInstructionModeEnabled(@NonNull Context context) {
        return isSingleInstructionModeEnabled(prefs(context));
    }

    static boolean isSingleInstructionModeEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_SINGLE_INSTRUCTION_MODE_ENABLED, false);
    }

    public static void setSingleInstructionModeEnabled(@NonNull Context context, boolean enabled) {
        setSingleInstructionModeEnabled(prefs(context), enabled);
    }

    static void setSingleInstructionModeEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_SINGLE_INSTRUCTION_MODE_ENABLED, enabled)
                .apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
