package vibro.navigator.settings;

import android.content.Context;

import androidx.annotation.NonNull;

public final class AppAndroidAutoSettings {
    private static final String KEY_ANDROID_AUTO_INTEGRATION_ENABLED = "android_auto_integration_enabled";

    private AppAndroidAutoSettings() {
    }

    public static boolean isIntegrationEnabled(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ANDROID_AUTO_INTEGRATION_ENABLED, true);
    }

    public static void setIntegrationEnabled(@NonNull Context context, boolean enabled) {
        context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ANDROID_AUTO_INTEGRATION_ENABLED, enabled)
                .apply();
    }
}
