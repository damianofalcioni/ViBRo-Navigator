package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class AppAndroidAutoSettings {
    private static final String KEY_ANDROID_AUTO_INTEGRATION_ENABLED = "android_auto_integration_enabled";

    private AppAndroidAutoSettings() {
    }

    public static boolean isIntegrationEnabled(@NonNull Context context) {
        return isIntegrationEnabled(prefs(context));
    }

    static boolean isIntegrationEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_ANDROID_AUTO_INTEGRATION_ENABLED, true);
    }

    public static void setIntegrationEnabled(@NonNull Context context, boolean enabled) {
        setIntegrationEnabled(prefs(context), enabled);
    }

    static void setIntegrationEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_ANDROID_AUTO_INTEGRATION_ENABLED, enabled)
                .apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
