package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Locale;

/** Stores the display language independently of speech recognition and voice preferences. */
public final class AppLanguageSettings {
    private static final String KEY_USE_SYSTEM_LANGUAGE = "use_system_language";
    private static final String KEY_SELECTED_LANGUAGE = "selected_language";
    private static final String[] LANGUAGE_TAGS = {"en", "it", "de"};

    private AppLanguageSettings() {
    }

    @NonNull
    public static String[] availableLanguageTags() {
        return LANGUAGE_TAGS.clone();
    }

    public static boolean isUseSystemLanguageEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_USE_SYSTEM_LANGUAGE, true);
    }

    public static void setUseSystemLanguageEnabled(@NonNull Context context, boolean enabled,
                                                  @NonNull String systemLanguageTag) {
        // Freeze the initial system choice when entering manual mode, even before opening the picker.
        prefs(context).edit()
                .putString(KEY_SELECTED_LANGUAGE, getSelectedLanguageTag(context, systemLanguageTag))
                .putBoolean(KEY_USE_SYSTEM_LANGUAGE, enabled)
                .apply();
    }

    @NonNull
    public static String getSelectedLanguageTag(@NonNull Context context, @NonNull String systemLanguageTag) {
        return supportedLanguage(prefs(context).getString(KEY_SELECTED_LANGUAGE, systemLanguageTag));
    }

    public static void setSelectedLanguageTag(@NonNull Context context, @NonNull String languageTag) {
        prefs(context).edit().putString(KEY_SELECTED_LANGUAGE, supportedLanguage(languageTag)).apply();
    }

    @NonNull
    public static String getEffectiveLanguageTag(@NonNull Context context, @NonNull String systemLanguageTag) {
        return isUseSystemLanguageEnabled(context)
                ? supportedLanguage(systemLanguageTag) : getSelectedLanguageTag(context, systemLanguageTag);
    }

    @NonNull
    public static String supportedLanguage(String languageTag) {
        String language = languageTag == null ? "" : Locale.forLanguageTag(languageTag).getLanguage();
        for (String available : LANGUAGE_TAGS) {
            if (available.equals(language)) {
                return available;
            }
        }
        return "en";
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
