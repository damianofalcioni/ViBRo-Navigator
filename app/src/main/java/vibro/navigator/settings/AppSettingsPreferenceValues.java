package vibro.navigator.settings;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

final class AppSettingsPreferenceValues {
    private static final String KEY_USE_FUSED_LOCATION = "use_fused_location";
    private static final String KEY_USE_IMPERIAL_UNITS = "use_imperial_units";
    private static final String KEY_GOOGLE_POI_API_KEY = "google_poi_api_key";
    private static final String KEY_GOOGLE_POI_API_KEY_VALID = "google_poi_api_key_valid";
    private static final String KEY_GOOGLE_POI_SEARCH_ENABLED = "google_poi_search_enabled";
    private static final String KEY_MANEUVER_SPEECH_ENABLED = "maneuver_speech_enabled";
    private static final String KEY_MANEUVER_VOICE_NAME = "maneuver_voice_name";

    private AppSettingsPreferenceValues() {
    }

    static boolean isFusedLocationEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_USE_FUSED_LOCATION, true);
    }

    static void setFusedLocationEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_USE_FUSED_LOCATION, enabled)
                .apply();
    }

    static boolean isImperialUnitsEnabled(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(KEY_USE_IMPERIAL_UNITS, false);
    }

    static void setImperialUnitsEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_USE_IMPERIAL_UNITS, enabled)
                .apply();
    }

    @NonNull
    static String getGooglePoiApiKey(@NonNull SharedPreferences preferences) {
        String apiKey = preferences.getString(KEY_GOOGLE_POI_API_KEY, "");
        return apiKey == null ? "" : apiKey;
    }

    static void setGooglePoiApiKey(@NonNull SharedPreferences preferences, @NonNull String apiKey) {
        String trimmed = apiKey.trim();
        SharedPreferences.Editor editor = preferences.edit();
        if (trimmed.isEmpty()) {
            editor.remove(KEY_GOOGLE_POI_API_KEY);
            editor.remove(KEY_GOOGLE_POI_API_KEY_VALID);
            editor.remove(KEY_GOOGLE_POI_SEARCH_ENABLED);
        } else {
            editor.putString(KEY_GOOGLE_POI_API_KEY, trimmed);
            editor.putBoolean(KEY_GOOGLE_POI_API_KEY_VALID, false);
            editor.putBoolean(KEY_GOOGLE_POI_SEARCH_ENABLED, false);
        }
        editor.apply();
    }

    static void setValidatedGooglePoiApiKey(
            @NonNull SharedPreferences preferences,
            @NonNull String apiKey
    ) {
        String trimmed = apiKey.trim();
        boolean valid = !trimmed.isEmpty();
        preferences.edit()
                .putString(KEY_GOOGLE_POI_API_KEY, trimmed)
                .putBoolean(KEY_GOOGLE_POI_API_KEY_VALID, valid)
                .putBoolean(KEY_GOOGLE_POI_SEARCH_ENABLED, valid)
                .apply();
    }

    static boolean hasValidGooglePoiApiKey(@NonNull SharedPreferences preferences) {
        boolean storedValid = preferences.getBoolean(KEY_GOOGLE_POI_API_KEY_VALID, false);
        return Boolean.logicalAnd(!getGooglePoiApiKey(preferences).trim().isEmpty(), storedValid);
    }

    static boolean isGooglePoiSearchEnabled(@NonNull SharedPreferences preferences) {
        return hasValidGooglePoiApiKey(preferences)
                && preferences.getBoolean(KEY_GOOGLE_POI_SEARCH_ENABLED, true);
    }

    static void setGooglePoiSearchEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_GOOGLE_POI_SEARCH_ENABLED, enabled && hasValidGooglePoiApiKey(preferences))
                .apply();
    }

    @NonNull
    static String getManeuverVoiceName(@NonNull SharedPreferences preferences) {
        String voiceName = preferences.getString(
                KEY_MANEUVER_VOICE_NAME,
                AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT
        );
        return normalizeManeuverVoiceName(voiceName);
    }

    static void setManeuverVoiceName(@NonNull SharedPreferences preferences, @NonNull String voiceName) {
        String trimmed = voiceName.trim();
        SharedPreferences.Editor editor = preferences.edit();
        if (trimmed.isEmpty() || AppSettings.MANEUVER_VOICE_DISABLED.equals(trimmed)) {
            editor.remove(KEY_MANEUVER_VOICE_NAME);
            editor.putBoolean(KEY_MANEUVER_SPEECH_ENABLED, false);
        } else {
            editor.putString(KEY_MANEUVER_VOICE_NAME, trimmed);
        }
        editor.apply();
    }

    static boolean isManeuverSpeechEnabled(@NonNull SharedPreferences preferences) {
        if (preferences.contains(KEY_MANEUVER_SPEECH_ENABLED)) {
            return preferences.getBoolean(KEY_MANEUVER_SPEECH_ENABLED, false);
        }
        String voiceName = preferences.getString(
                KEY_MANEUVER_VOICE_NAME,
                AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT
        );
        return !AppSettings.MANEUVER_VOICE_DISABLED.equals(voiceName);
    }

    static void setManeuverSpeechEnabled(@NonNull SharedPreferences preferences, boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_MANEUVER_SPEECH_ENABLED, enabled)
                .apply();
    }

    @NonNull
    private static String normalizeManeuverVoiceName(String voiceName) {
        if (voiceName == null) {
            return AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT;
        }
        String trimmed = voiceName.trim();
        if (trimmed.isEmpty() || AppSettings.MANEUVER_VOICE_DISABLED.equals(trimmed)) {
            return AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT;
        }
        return trimmed;
    }
}
