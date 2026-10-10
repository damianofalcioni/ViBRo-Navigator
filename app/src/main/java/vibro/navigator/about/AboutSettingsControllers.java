package vibro.navigator.about;

import android.app.Activity;
import android.content.Intent;

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
    private AboutCompassProjectionSettings compassProjectionSettings;
    private AboutNavigationHintSettings navigationHintSettings;
    private AboutOutputFolderSettings outputFolderSettings;

    AboutSettingsControllers(@NonNull Activity activity) {
        this.activity = activity;
    }

    void configure() {
        outputFolderSettings = new AboutOutputFolderSettings(activity);
        outputFolderSettings.configure();
        compassProjectionSettings = new AboutCompassProjectionSettings(activity);
        navigationHintSettings = new AboutNavigationHintSettings(activity);
        new AboutSurroundingStreetTypes(activity).configure(
                activity.findViewById(R.id.aboutCompassSurroundingStreetsSettingsButton)
        );
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
        AboutNavigationCustomButtonSettings navigationCustomButtonSettings = new AboutNavigationCustomButtonSettings(
                activity,
                activity.findViewById(R.id.aboutNavigationCustomButtonSettingsButton)
        );
        navigationCustomButtonSettings.configure();
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
        outputFolderSettings.shutdown();
        if (speechRecognitionSettings != null) {
            speechRecognitionSettings.shutdown();
        }
        if (maneuverVoiceSettings != null) {
            maneuverVoiceSettings.shutdown();
        }
        if (googlePoiApiKeySettings != null) {
            googlePoiApiKeySettings.shutdown();
        }
    }

    boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return outputFolderSettings.handleActivityResult(requestCode, resultCode, data);
    }

    void renderStorage() {
        outputFolderSettings.render();
    }

    void requestOutputStorageAccess() {
        outputFolderSettings.requestStorageAccess();
    }

    boolean handlePermissionResult(int requestCode) {
        return outputFolderSettings.handlePermissionResult(requestCode);
    }

    void refreshAfterDatabaseImport() {
        compassProjectionSettings.refresh();
        navigationHintSettings.refresh();
        refreshGooglePoiApiKeySetting();
        refreshAndroidAutoSettingAfterDatabaseImport();
        refreshPoiCategorySetting();
        if (speechRecognitionSettings != null) {
            speechRecognitionSettings.refreshSelection();
        }
        if (maneuverVoiceSettings != null) {
            maneuverVoiceSettings.refreshSelection();
        }
    }

    void flush() {
        if (outputFolderSettings != null) {
            outputFolderSettings.flush();
        }
        if (navigationHintSettings != null) {
            navigationHintSettings.flush();
        }
        if (compassProjectionSettings != null) {
            compassProjectionSettings.flush();
        }
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

    private void refreshAndroidAutoSettingAfterDatabaseImport() {
        if (androidAutoSettings != null) {
            androidAutoSettings.refreshAfterDatabaseImport();
        }
    }
}
