package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class AppSettings {
    private static final String PREFS = "vibro.navigator.settings";
    private static final String KEY_USE_FUSED_LOCATION = "use_fused_location";
    private static final String KEY_USE_IMPERIAL_UNITS = "use_imperial_units";
    private static final String KEY_GOOGLE_POI_API_KEY = "google_poi_api_key";
    private static final String KEY_MANEUVER_VOICE_NAME = "maneuver_voice_name";
    public static final String MANEUVER_VOICE_DISABLED = "__disabled__";
    public static final String MANEUVER_VOICE_SYSTEM_DEFAULT = "__system_default__";

    private AppSettings() {
    }

    public static boolean isFusedLocationEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_USE_FUSED_LOCATION, true);
    }

    public static void setFusedLocationEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit()
                .putBoolean(KEY_USE_FUSED_LOCATION, enabled)
                .apply();
    }

    public static boolean isImperialUnitsEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_USE_IMPERIAL_UNITS, false);
    }

    public static void setImperialUnitsEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit()
                .putBoolean(KEY_USE_IMPERIAL_UNITS, enabled)
                .apply();
    }

    @NonNull
    public static String getGooglePoiApiKey(@NonNull Context context) {
        String apiKey = prefs(context).getString(KEY_GOOGLE_POI_API_KEY, "");
        return apiKey == null ? "" : apiKey;
    }

    public static void setGooglePoiApiKey(@NonNull Context context, @NonNull String apiKey) {
        String trimmed = apiKey.trim();
        SharedPreferences.Editor editor = prefs(context).edit();
        if (trimmed.isEmpty()) {
            editor.remove(KEY_GOOGLE_POI_API_KEY);
        } else {
            editor.putString(KEY_GOOGLE_POI_API_KEY, trimmed);
        }
        editor.apply();
    }

    @NonNull
    public static String getManeuverVoiceName(@NonNull Context context) {
        String voiceName = prefs(context).getString(KEY_MANEUVER_VOICE_NAME, MANEUVER_VOICE_DISABLED);
        return voiceName == null || voiceName.trim().isEmpty() ? MANEUVER_VOICE_DISABLED : voiceName;
    }

    public static void setManeuverVoiceName(@NonNull Context context, @NonNull String voiceName) {
        String trimmed = voiceName.trim();
        SharedPreferences.Editor editor = prefs(context).edit();
        if (trimmed.isEmpty() || MANEUVER_VOICE_DISABLED.equals(trimmed)) {
            editor.remove(KEY_MANEUVER_VOICE_NAME);
        } else {
            editor.putString(KEY_MANEUVER_VOICE_NAME, trimmed);
        }
        editor.apply();
    }

    public static boolean isManeuverSpeechEnabled(@NonNull Context context) {
        return !MANEUVER_VOICE_DISABLED.equals(getManeuverVoiceName(context));
    }

    public static boolean isSystemDefaultManeuverVoice(@NonNull String voiceName) {
        return MANEUVER_VOICE_SYSTEM_DEFAULT.equals(voiceName);
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
