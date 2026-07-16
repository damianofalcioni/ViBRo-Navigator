package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Locale;

public final class AppSpeechRecognitionSettings {
    public static final String LANGUAGE_SYSTEM_DEFAULT = "";

    private static final String KEY_ENABLED = "speech_recognition_enabled";
    private static final String KEY_LANGUAGE_TAG = "speech_recognition_language_tag";

    private AppSpeechRecognitionSettings() {
    }

    public static boolean isEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
    }

    @NonNull
    public static String getLanguageTag(@NonNull Context context) {
        String languageTag = prefs(context).getString(KEY_LANGUAGE_TAG, LANGUAGE_SYSTEM_DEFAULT);
        return normalizeLanguageTag(languageTag);
    }

    public static void setLanguageTag(@NonNull Context context, @NonNull String languageTag) {
        String normalized = normalizeLanguageTag(languageTag);
        SharedPreferences.Editor editor = prefs(context).edit();
        if (normalized.isEmpty()) {
            editor.remove(KEY_LANGUAGE_TAG);
        } else {
            editor.putString(KEY_LANGUAGE_TAG, normalized);
        }
        editor.apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    private static String normalizeLanguageTag(String languageTag) {
        if (languageTag == null) {
            return LANGUAGE_SYSTEM_DEFAULT;
        }
        String trimmed = languageTag.trim().replace('_', '-');
        if (trimmed.isEmpty()) {
            return LANGUAGE_SYSTEM_DEFAULT;
        }
        Locale locale = Locale.forLanguageTag(trimmed);
        if (locale.getLanguage().isEmpty()) {
            return LANGUAGE_SYSTEM_DEFAULT;
        }
        return locale.toLanguageTag();
    }
}
