package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidThemedButtons;

final class AboutSettingInfoButtons {
    @NonNull
    private final Activity activity;

    AboutSettingInfoButtons(@NonNull Activity activity) {
        this.activity = activity;
    }

    void configure() {
        bind(R.id.aboutLogEnabledInfoButton, R.string.label_log_enabled, R.string.about_setting_log_info);
        bind(
                R.id.aboutAutoSaveGpxInfoButton,
                R.string.label_auto_save_gpx_enabled,
                R.string.about_setting_auto_save_gpx_info
        );
        bind(
                R.id.aboutFusedLocationInfoButton,
                R.string.label_fused_location_enabled,
                R.string.about_setting_fused_location_info
        );
        bind(
                R.id.aboutDynamicGpsFixIntervalInfoButton,
                R.string.label_dynamic_gps_fix_interval_enabled,
                R.string.about_setting_dynamic_gps_fix_interval_info
        );
        bind(
                R.id.aboutImperialUnitsInfoButton,
                R.string.label_imperial_units_enabled,
                R.string.about_setting_imperial_units_info
        );
        bind(R.id.aboutLightThemeInfoButton, R.string.label_light_theme_enabled, R.string.about_setting_light_theme_info);
        bind(
                R.id.aboutCompassSurroundingStreetsInfoButton,
                R.string.label_compass_surrounding_streets_enabled,
                R.string.about_setting_compass_surrounding_streets_info
        );
        bind(
                R.id.aboutNavigationNotificationsInfoButton,
                R.string.label_navigation_notifications_enabled,
                R.string.about_setting_navigation_notifications_info
        );
        bind(
                R.id.aboutSingleInstructionModeInfoButton,
                R.string.label_single_instruction_mode_enabled,
                R.string.about_setting_single_instruction_mode_info
        );
        bind(
                R.id.aboutAndroidAutoInfoButton,
                R.string.label_android_auto_integration_enabled,
                R.string.about_setting_android_auto_info
        );
        bind(R.id.aboutPoiCategoriesInfoButton, R.string.label_poi_categories, R.string.about_setting_poi_categories_info);
        bind(
                R.id.aboutSpeechRecognitionInfoButton,
                R.string.label_speech_recognition,
                R.string.about_setting_speech_recognition_info
        );
        bind(R.id.aboutManeuverVoiceInfoButton, R.string.label_maneuver_voice, R.string.about_setting_maneuver_voice_info);
        bind(R.id.aboutGooglePoiApiKeyInfoButton, R.string.label_google_poi_api_key, R.string.about_setting_google_poi_api_key_info);
        bind(R.id.aboutExportDatabaseInfoButton, R.string.action_export_database, R.string.about_setting_export_database_info);
        bind(R.id.aboutImportDatabaseInfoButton, R.string.action_import_database, R.string.about_setting_import_database_info);
    }

    private void bind(int buttonId, int titleResId, int messageResId) {
        ImageButton button = activity.findViewById(buttonId);
        AndroidThemedButtons.applyIconButtonBackground(button);
        button.setContentDescription(activity.getString(
                R.string.format_about_setting_info_content_description,
                activity.getString(titleResId)
        ));
        AboutDeferredDialogAction.configure(activity, button, () -> showInfo(titleResId, messageResId));
    }

    private void showInfo(int titleResId, int messageResId) {
        new AlertDialog.Builder(activity)
                .setTitle(titleResId)
                .setMessage(messageResId)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
