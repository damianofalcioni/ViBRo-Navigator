package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class AppNavigationHintSettings {
    private static final String KEY_SHOW_HINT_PANEL = "show_hint_panel";

    private AppNavigationHintSettings() {
    }

    public static boolean isEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_SHOW_HINT_PANEL, true);
    }

    public static void setEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_SHOW_HINT_PANEL, enabled).apply();
    }

    public static Runnable observe(@NonNull Context context, @NonNull Runnable changed) {
        SharedPreferences preferences = prefs(context);
        SharedPreferences.OnSharedPreferenceChangeListener listener = (source, key) -> {
            if (key == null || KEY_SHOW_HINT_PANEL.equals(key)) {
                changed.run();
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(listener);
        return () -> preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
