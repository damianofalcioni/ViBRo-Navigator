package vibro.navigator.settings;

import android.content.Context;

import androidx.annotation.NonNull;

public final class AppNotificationSettings {
    private static final String KEY_NAVIGATION_NOTIFICATIONS_ENABLED = "navigation_notifications_enabled";

    private AppNotificationSettings() {
    }

    public static boolean areNavigationNotificationsEnabled(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_NAVIGATION_NOTIFICATIONS_ENABLED, true);
    }

    public static void setNavigationNotificationsEnabled(@NonNull Context context, boolean enabled) {
        context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NAVIGATION_NOTIFICATIONS_ENABLED, enabled)
                .apply();
    }
}
