package vibro.navigator.about;

import android.app.Activity;

import androidx.annotation.NonNull;

import vibro.navigator.R;

final class AboutSettingsControllers {
    @NonNull
    private final Activity activity;

    private AboutManeuverVoiceSettings maneuverVoiceSettings;
    private AboutGooglePoiApiKeySettings googlePoiApiKeySettings;
    private AboutAndroidAutoSettings androidAutoSettings;
    private AboutPoiCategorySettings poiCategorySettings;
    private AboutSpeechRecognitionSettings speechRecognitionSettings;

    AboutSettingsControllers(@NonNull Activity activity) {
        this.activity = activity;
    }

    void configure() {
        poiCategorySettings = new AboutPoiCategorySettings(
                activity,
                activity.findViewById(R.id.aboutPoiCategoriesButton),
                activity.findViewById(R.id.aboutPoiCategoriesSwitch)
        );
        poiCategorySettings.configure();
        speechRecognitionSettings = new AboutSpeechRecognitionSettings(
                activity,
                activity.findViewById(R.id.aboutSpeechRecognitionSettingsButton),
                activity.findViewById(R.id.aboutSpeechRecognitionSwitch)
        );
        maneuverVoiceSettings = new AboutManeuverVoiceSettings(
                activity,
                activity.findViewById(R.id.aboutManeuverVoiceSettingsButton),
                activity.findViewById(R.id.aboutManeuverVoiceSwitch)
        );
        googlePoiApiKeySettings = new AboutGooglePoiApiKeySettings(
                activity,
                activity.findViewById(R.id.aboutGooglePoiApiKeyContainer),
                activity.findViewById(R.id.aboutGooglePoiApiKeyButton),
                activity.findViewById(R.id.aboutGooglePoiSearchSwitch)
        );
        googlePoiApiKeySettings.configure();
        androidAutoSettings = new AboutAndroidAutoSettings(
                activity,
                activity.findViewById(R.id.aboutAndroidAutoRow),
                activity.findViewById(R.id.aboutAndroidAutoSwitch)
        );
        androidAutoSettings.configure();
    }

    void shutdown() {
        if (maneuverVoiceSettings != null) {
            maneuverVoiceSettings.shutdown();
        }
        if (googlePoiApiKeySettings != null) {
            googlePoiApiKeySettings.shutdown();
        }
    }

    void refreshAfterDatabaseImport() {
        refreshGooglePoiApiKeySetting();
        refreshAndroidAutoSetting();
        refreshPoiCategorySetting();
        if (speechRecognitionSettings != null) {
            speechRecognitionSettings.refreshSelection();
        }
        if (maneuverVoiceSettings != null) {
            maneuverVoiceSettings.refreshSelection();
        }
    }

    void flush() {
        if (androidAutoSettings != null) {
            androidAutoSettings.flush();
        }
    }

    private void refreshPoiCategorySetting() {
        if (poiCategorySettings != null) {
            poiCategorySettings.refresh();
        }
    }

    private void refreshGooglePoiApiKeySetting() {
        if (googlePoiApiKeySettings != null) {
            googlePoiApiKeySettings.refresh();
        }
    }

    private void refreshAndroidAutoSetting() {
        if (androidAutoSettings != null) {
            androidAutoSettings.refresh();
        }
    }
}
