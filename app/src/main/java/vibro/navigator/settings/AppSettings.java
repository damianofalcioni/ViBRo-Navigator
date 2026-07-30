package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.List;

public final class AppSettings {
    static final String PREFS = "vibro.navigator.settings";
    public static final String MANEUVER_VOICE_DISABLED = "__disabled__";
    public static final String MANEUVER_VOICE_SYSTEM_DEFAULT = "__system_default__";

    private AppSettings() {
    }

    public static boolean isFusedLocationEnabled(@NonNull Context context) {
        return AppSettingsPreferenceValues.isFusedLocationEnabled(prefs(context));
    }

    public static void setFusedLocationEnabled(@NonNull Context context, boolean enabled) {
        AppSettingsPreferenceValues.setFusedLocationEnabled(prefs(context), enabled);
    }

    public static boolean isImperialUnitsEnabled(@NonNull Context context) {
        return AppSettingsPreferenceValues.isImperialUnitsEnabled(prefs(context));
    }

    public static void setImperialUnitsEnabled(@NonNull Context context, boolean enabled) {
        AppSettingsPreferenceValues.setImperialUnitsEnabled(prefs(context), enabled);
    }

    @NonNull
    public static String getGooglePoiApiKey(@NonNull Context context) {
        return AppSettingsPreferenceValues.getGooglePoiApiKey(prefs(context));
    }

    public static void setGooglePoiApiKey(@NonNull Context context, @NonNull String apiKey) {
        AppSettingsPreferenceValues.setGooglePoiApiKey(prefs(context), apiKey);
    }

    public static void setValidatedGooglePoiApiKey(@NonNull Context context, @NonNull String apiKey) {
        AppSettingsPreferenceValues.setValidatedGooglePoiApiKey(prefs(context), apiKey);
    }

    public static boolean hasValidGooglePoiApiKey(@NonNull Context context) {
        return AppSettingsPreferenceValues.hasValidGooglePoiApiKey(prefs(context));
    }

    public static boolean isGooglePoiSearchEnabled(@NonNull Context context) {
        return AppSettingsPreferenceValues.isGooglePoiSearchEnabled(prefs(context));
    }

    public static void setGooglePoiSearchEnabled(@NonNull Context context, boolean enabled) {
        AppSettingsPreferenceValues.setGooglePoiSearchEnabled(prefs(context), enabled);
    }

    @NonNull
    public static String getManeuverVoiceName(@NonNull Context context) {
        return AppSettingsPreferenceValues.getManeuverVoiceName(prefs(context));
    }

    public static void setManeuverVoiceName(@NonNull Context context, @NonNull String voiceName) {
        AppSettingsPreferenceValues.setManeuverVoiceName(prefs(context), voiceName);
    }

    public static boolean isManeuverSpeechEnabled(@NonNull Context context) {
        return AppSettingsPreferenceValues.isManeuverSpeechEnabled(prefs(context));
    }

    public static void setManeuverSpeechEnabled(@NonNull Context context, boolean enabled) {
        AppSettingsPreferenceValues.setManeuverSpeechEnabled(prefs(context), enabled);
    }

    public static boolean isSystemDefaultManeuverVoice(@NonNull String voiceName) {
        return MANEUVER_VOICE_SYSTEM_DEFAULT.equals(voiceName);
    }

    public static boolean isMapPoiCategoryFilterEnabled(@NonNull Context context) {
        return AppPoiCategoryPreferences.isMapPoiCategoryFilterEnabled(prefs(context));
    }

    public static void setMapPoiCategoryFilterEnabled(@NonNull Context context, boolean enabled) {
        AppPoiCategoryPreferences.setMapPoiCategoryFilterEnabled(prefs(context), enabled);
    }

    @NonNull
    public static List<String> getMapPoiCategoryNames(@NonNull Context context) {
        return AppPoiCategoryPreferences.getMapPoiCategoryNames(prefs(context));
    }

    @NonNull
    public static List<String> getEnabledMapPoiCategoryNames(@NonNull Context context) {
        return AppPoiCategoryPreferences.getEnabledMapPoiCategoryNames(prefs(context));
    }

    @NonNull
    public static List<AppPoiCategorySetting> getMapPoiCategorySettings(@NonNull Context context) {
        return AppPoiCategoryPreferences.getMapPoiCategorySettings(prefs(context));
    }

    public static void setMapPoiCategorySettings(
            @NonNull Context context,
            @NonNull List<AppPoiCategorySetting> settings
    ) {
        AppPoiCategoryPreferences.setMapPoiCategorySettings(prefs(context), settings);
    }

    public static void setMapPoiCategoryNames(@NonNull Context context, @NonNull List<String> names) {
        AppPoiCategoryPreferences.setMapPoiCategoryNames(prefs(context), names);
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
