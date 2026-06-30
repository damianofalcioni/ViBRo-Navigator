package vibro.navigator.about;

import static vibro.navigator.about.AboutDialogButtonStyleAssertions.assertIconButtonBackground;
import static vibro.navigator.about.AboutDialogButtonStyleAssertions.assertSecondaryButtonBackground;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.voice.NavigationTextToSpeechSettingsLauncher;
import vibro.navigator.settings.AppAndroidAutoSettings;
import vibro.navigator.settings.AppCompassSettings;
import vibro.navigator.settings.AppNotificationSettings;
import vibro.navigator.settings.AppSettings;
import vibro.navigator.settings.AppThemeSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class AboutLoggingSettingsRobolectricTest {

    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        AppLogger.setLoggingEnabled(context, false);
        AppLogger.init(context);
        AppSettings.setFusedLocationEnabled(context, true);
        AppSettings.setImperialUnitsEnabled(context, false);
        AppCompassSettings.setSurroundingStreetsEnabled(context, false);
        AppNotificationSettings.setNavigationNotificationsEnabled(context, true);
        AppThemeSettings.setLightThemeEnabled(context, false);
        AppAndroidAutoSettings.setIntegrationEnabled(context, true);
        AppSettings.setManeuverVoiceName(context, AppSettings.MANEUVER_VOICE_DISABLED);
        AppSettings.setMapPoiCategoryFilterEnabled(context, false);
        AppSettings.setMapPoiCategoryNames(context, Collections.emptyList());
        ShadowToast.reset();
    }

    @Test
    public void loggingSettingStartsDisabledAndPersistsWhenEnabled() {
        Application context = ApplicationProvider.getApplicationContext();

        assertFalse(AppLogger.isLoggingEnabled(context));

        assertTrue(AppLogger.setLoggingEnabled(context, true));
        assertTrue(AppLogger.isLoggingEnabled(context));

        AppLogger.init(context);

        assertTrue(AppLogger.isLoggingEnabled(context));
    }

    @Test
    public void settingLoggingToSameValueKeepsCurrentLogSession() {
        Application context = ApplicationProvider.getApplicationContext();

        assertTrue(AppLogger.setLoggingEnabled(context, true));
        String firstPath = AppLogger.getLogFilePath(context);

        assertFalse(AppLogger.setLoggingEnabled(context, true));
        assertEquals(firstPath, AppLogger.getLogFilePath(context));
    }

    @Test
    public void enabledLoggingCreatesFreshSessionFileAfterRestart() throws Exception {
        Application context = ApplicationProvider.getApplicationContext();

        assertTrue(AppLogger.setLoggingEnabled(context, true));
        String firstPath = AppLogger.getLogFilePath(context);
        AppLogger.i("AboutLoggingSettingsTest", "first run marker");
        String firstContent = new String(
                Files.readAllBytes(new File(firstPath).toPath()),
                StandardCharsets.UTF_8
        );
        assertTrue(firstContent.contains("first run marker"));

        AppLogger.init(context);

        String secondPath = AppLogger.getLogFilePath(context);
        String secondContent = new String(
                Files.readAllBytes(new File(secondPath).toPath()),
                StandardCharsets.UTF_8
        );

        assertNotEquals(firstPath, secondPath);
        assertFalse(secondContent.contains("first run marker"));
    }

    @Test
    public void aboutPageFiveTapsDoNotEnableLogging() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        View root = activity.findViewById(R.id.aboutRoot);

        performFiveTaps(root);

        assertFalse(AppLogger.isLoggingEnabled(activity));
    }

    @Test
    public void aboutPageShowsSettingsAndDiagnosticsWithoutGesture() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        idleInitialDiagnosticRender();
        Switch logEnabledSwitch = activity.findViewById(R.id.aboutLogEnabledSwitch);
        Switch imperialUnitsSwitch = activity.findViewById(R.id.aboutImperialUnitsSwitch);
        Switch surroundingStreetsSwitch = activity.findViewById(R.id.aboutCompassSurroundingStreetsSwitch);
        Switch androidAutoSwitch = activity.findViewById(R.id.aboutAndroidAutoSwitch);
        TextView poiCategoriesLabel = activity.findViewById(R.id.aboutPoiCategoriesLabel);
        ImageButton poiCategoriesButton = activity.findViewById(R.id.aboutPoiCategoriesButton);
        Switch poiCategoriesSwitch = activity.findViewById(R.id.aboutPoiCategoriesSwitch);
        TextView sensorStatusTitle = activity.findViewById(R.id.aboutSensorStatusTitle);
        TextView sensorStatusBody = activity.findViewById(R.id.aboutSensorStatusBody);
        TextView symbolTestTitle = activity.findViewById(R.id.aboutSymbolTestTitle);
        Button exportDatabaseButton = activity.findViewById(R.id.aboutExportDatabaseButton);
        Button importDatabaseButton = activity.findViewById(R.id.aboutImportDatabaseButton);
        Button symbolTestLeftButton = activity.findViewById(R.id.aboutSymbolTestLeftButton);
        Button symbolTestOtherButton = activity.findViewById(R.id.aboutSymbolTestOtherButton);
        Button symbolTestRightButton = activity.findViewById(R.id.aboutSymbolTestRightButton);
        View googlePoiApiKeyContainer = activity.findViewById(R.id.aboutGooglePoiApiKeyContainer);
        TextView maneuverVoiceLabel = activity.findViewById(R.id.aboutManeuverVoiceLabel);
        ImageButton maneuverVoiceSettingsButton = activity.findViewById(R.id.aboutManeuverVoiceSettingsButton);
        Switch maneuverVoiceSwitch = activity.findViewById(R.id.aboutManeuverVoiceSwitch);

        assertFalse(logEnabledSwitch.isChecked());
        assertFalse(imperialUnitsSwitch.isChecked());
        assertEquals(
                activity.getString(R.string.label_compass_surrounding_streets_enabled),
                surroundingStreetsSwitch.getText().toString()
        );
        assertFalse(surroundingStreetsSwitch.isChecked());
        assertAndroidAutoSetting(activity, androidAutoSwitch);
        assertEquals(activity.getString(R.string.label_poi_categories), poiCategoriesLabel.getText().toString());
        assertEquals(
                activity.getString(R.string.action_edit_poi_categories),
                poiCategoriesButton.getContentDescription().toString()
        );
        assertFalse(poiCategoriesSwitch.isChecked());
        assertEquals(activity.getString(R.string.label_maneuver_voice), maneuverVoiceLabel.getText().toString());
        assertEquals(View.VISIBLE, maneuverVoiceSettingsButton.getVisibility());
        assertEquals(View.VISIBLE, maneuverVoiceSwitch.getVisibility());
        assertFalse(maneuverVoiceSwitch.isChecked());
        assertEquals(
                activity.getString(R.string.action_open_speech_directions_settings),
                maneuverVoiceSettingsButton.getContentDescription().toString()
        );
        assertEquals(
                DistributionServices.supportsUserGooglePoiApiKey() ? View.VISIBLE : View.GONE,
                googlePoiApiKeyContainer.getVisibility()
        );
        assertGooglePoiApiKeySetting(activity);
        assertEquals(View.VISIBLE, exportDatabaseButton.getVisibility());
        assertEquals(View.VISIBLE, importDatabaseButton.getVisibility());
        assertHasStartIcon(exportDatabaseButton);
        assertHasStartIcon(importDatabaseButton);
        assertEquals(
                Arrays.asList(
                        activity.getString(R.string.about_source_code_link),
                        activity.getString(R.string.about_report_issue_link),
                        activity.getString(R.string.about_privacy_policy_link),
                        activity.getString(R.string.about_terms_of_service_link)
                ),
                Arrays.asList(
                        ((TextView) activity.findViewById(R.id.aboutSourceCodeLink)).getText().toString(),
                        ((TextView) activity.findViewById(R.id.aboutReportIssueLink)).getText().toString(),
                        ((TextView) activity.findViewById(R.id.aboutPrivacyPolicyLink)).getText().toString(),
                        ((TextView) activity.findViewById(R.id.aboutTermsOfServiceLink)).getText().toString()
                )
        );
        assertEquals(View.VISIBLE, sensorStatusTitle.getVisibility());
        assertEquals(View.VISIBLE, sensorStatusBody.getVisibility());
        assertEquals(View.VISIBLE, symbolTestTitle.getVisibility());
        assertEquals(
                activity.getString(R.string.label_direction_notification_tests),
                symbolTestTitle.getText().toString()
        );
        assertEquals(View.VISIBLE, symbolTestLeftButton.getVisibility());
        assertEquals(View.VISIBLE, symbolTestOtherButton.getVisibility());
        assertEquals(View.VISIBLE, symbolTestRightButton.getVisibility());
        assertEquals(activity.getString(R.string.action_test_left_notification), symbolTestLeftButton.getText().toString());
        assertEquals(activity.getString(R.string.action_test_other_notification), symbolTestOtherButton.getText().toString());
        assertEquals(activity.getString(R.string.action_test_right_notification), symbolTestRightButton.getText().toString());
        assertTrue(sensorStatusBody.getText().toString().contains(
                activity.getString(R.string.label_sensor_gps_provider)
        ));
        assertTrue(sensorStatusBody.getText().toString().contains(
                activity.getString(R.string.label_sensor_network_provider)
        ));
        assertTrue(sensorStatusBody.getText().toString().contains(
                activity.getString(R.string.label_sensor_rotation_vector)
        ));
        assertTrue(sensorStatusBody.getText().toString().contains(
                activity.getString(R.string.label_sensor_geomagnetic_rotation_vector)
        ));
        assertTrue(sensorStatusBody.getText().toString().contains(
                activity.getString(R.string.label_sensor_orientation)
        ));
        assertTrue(sensorStatusBody.getText().toString().contains("value="));
    }

    @Test
    public void aboutPageLogSwitchEnablesLogging() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Switch logEnabledSwitch = activity.findViewById(R.id.aboutLogEnabledSwitch);

        assertFalse(AppLogger.isLoggingEnabled(activity));

        logEnabledSwitch.performClick();

        assertTrue(logEnabledSwitch.isChecked());
        assertFalse(AppLogger.isLoggingEnabled(activity));
        idleDeferredSettingApply();

        assertTrue(AppLogger.isLoggingEnabled(activity));
        assertTrue(new File(AppLogger.getLogFilePath(activity)).exists());
    }

    @Test
    public void aboutPageUnitAndFusedLocationSwitchesPersistPreferences() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Switch imperialUnitsSwitch = activity.findViewById(R.id.aboutImperialUnitsSwitch);
        Switch fusedLocationSwitch = activity.findViewById(R.id.aboutFusedLocationSwitch);
        Switch surroundingStreetsSwitch = activity.findViewById(R.id.aboutCompassSurroundingStreetsSwitch);

        assertFalse(AppSettings.isImperialUnitsEnabled(activity));
        assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));

        imperialUnitsSwitch.performClick();
        surroundingStreetsSwitch.performClick();

        assertTrue(imperialUnitsSwitch.isChecked());
        assertTrue(surroundingStreetsSwitch.isChecked());
        assertFalse(AppSettings.isImperialUnitsEnabled(activity));
        assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));
        idleDeferredSettingApply();

        assertTrue(AppSettings.isImperialUnitsEnabled(activity));
        assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));

        if (!DistributionServices.supportsFusedLocation()) {
            assertFalse(fusedLocationSwitch.isEnabled());
            return;
        }

        assertTrue(AppSettings.isFusedLocationEnabled(activity));

        fusedLocationSwitch.performClick();

        assertFalse(fusedLocationSwitch.isChecked());
        assertTrue(AppSettings.isFusedLocationEnabled(activity));
        idleDeferredSettingApply();

        assertFalse(AppSettings.isFusedLocationEnabled(activity));
    }

    @Test
    public void aboutPageAndroidAutoSwitchPersistsPreferenceWhenSupported() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Switch androidAutoSwitch = activity.findViewById(R.id.aboutAndroidAutoSwitch);

        if (!DistributionServices.supportsAndroidAutoIntegration()) {
            assertEquals(View.GONE, androidAutoSwitch.getVisibility());
            return;
        }

        assertTrue(AppAndroidAutoSettings.isIntegrationEnabled(activity));
        assertTrue(androidAutoSwitch.isChecked());

        androidAutoSwitch.performClick();

        assertFalse(androidAutoSwitch.isChecked());
        assertTrue(AppAndroidAutoSettings.isIntegrationEnabled(activity));
        idleDeferredSettingApply();

        assertFalse(AppAndroidAutoSettings.isIntegrationEnabled(activity));
    }

    @Test
    public void aboutPageSpeechDirectionsSwitchPersistsEnabledWithoutClearingPreferredVoice() {
        Application context = ApplicationProvider.getApplicationContext();
        String preferredVoice = "preferred-offline-voice";
        AppSettings.setManeuverVoiceName(context, preferredVoice);
        AppSettings.setManeuverSpeechEnabled(context, true);
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Switch maneuverVoiceSwitch = activity.findViewById(R.id.aboutManeuverVoiceSwitch);

        assertTrue(maneuverVoiceSwitch.isChecked());

        maneuverVoiceSwitch.performClick();

        assertFalse(AppSettings.isManeuverSpeechEnabled(activity));
        assertEquals(preferredVoice, AppSettings.getManeuverVoiceName(activity));
    }

    @Test
    public void aboutPagePoiCategorySwitchPersistsPreference() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Switch poiCategoriesSwitch = activity.findViewById(R.id.aboutPoiCategoriesSwitch);

        assertFalse(AppSettings.isMapPoiCategoryFilterEnabled(activity));

        poiCategoriesSwitch.performClick();

        assertTrue(AppSettings.isMapPoiCategoryFilterEnabled(activity));
    }

    @Test
    public void aboutPagePoiCategoryDialogSavesDynamicRows() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        ImageButton poiCategoriesButton = activity.findViewById(R.id.aboutPoiCategoriesButton);

        poiCategoriesButton.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(AboutDeferredDialogAction.OPEN_DELAY_MS + 50, TimeUnit.MILLISECONDS);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        LinearLayout categoryList = dialog.findViewById(R.id.aboutPoiCategoryDialogList);
        EditText firstField = categoryFieldAt(categoryList, 0);

        assertFalse(firstField.hasFocus());
        assertEquals(activity.getString(R.string.hint_poi_category_name), firstField.getHint().toString());

        firstField.setText("Fuel");
        categorySwitchAt(categoryList, 0).setChecked(true);
        ImageButton addButton = dialog.findViewById(R.id.aboutPoiCategoryAddButton);
        LinearLayout firstCategoryRow = (LinearLayout) categoryList.getChildAt(0);
        ImageButton removeButton = (ImageButton) firstCategoryRow.getChildAt(2);
        assertIconButtonBackground(addButton);
        assertEquals(R.drawable.ic_delete, (int) ReflectionHelpers.getField(removeButton, "mResource"));
        addButton.performClick();
        categoryFieldAt(categoryList, 1).setText("Restaurant");
        categorySwitchAt(categoryList, 1).setChecked(false);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(Arrays.asList("Fuel", "Restaurant"), AppSettings.getMapPoiCategoryNames(activity));
        assertEquals(Arrays.asList("Fuel"), AppSettings.getEnabledMapPoiCategoryNames(activity));
    }

    @Test
    public void aboutPageSpeechDirectionsDialogOmitsDisabledVoiceOption() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        ImageButton settingsButton = activity.findViewById(R.id.aboutManeuverVoiceSettingsButton);

        settingsButton.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(AboutDeferredDialogAction.OPEN_DELAY_MS + 50, TimeUnit.MILLISECONDS);

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        TextView voiceLabel = dialog.findViewById(R.id.aboutManeuverVoiceDialogVoiceLabel);
        Spinner spinner = dialog.findViewById(R.id.aboutManeuverVoiceSpinner);
        ImageButton playButton = dialog.findViewById(R.id.aboutManeuverVoicePlayButton);

        assertIconButtonBackground(playButton);
        assertEquals(activity.getString(R.string.label_maneuver_voice_spinner), voiceLabel.getText().toString());
        assertEquals(
                activity.getString(R.string.action_test_maneuver_voice),
                playButton.getContentDescription().toString()
        );
        assertEquals(
                activity.getString(R.string.label_maneuver_voice_system_default),
                spinner.getItemAtPosition(0).toString()
        );
        for (int i = 0; i < spinner.getCount(); i++) {
            assertNotEquals(
                    activity.getString(R.string.label_maneuver_voice_disabled),
                    spinner.getItemAtPosition(i).toString()
            );
        }
    }

    @Test
    public void aboutPageTtsSettingsButtonOpensTextToSpeechSettings() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        ImageButton settingsButton = activity.findViewById(R.id.aboutManeuverVoiceSettingsButton);

        settingsButton.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(AboutDeferredDialogAction.OPEN_DELAY_MS + 50, TimeUnit.MILLISECONDS);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        Button ttsSettingsButton = dialog.findViewById(R.id.aboutTtsSettingsButton);

        assertSecondaryButtonBackground(ttsSettingsButton);
        assertEquals(
                activity.getString(R.string.action_android_tts_settings_short),
                ttsSettingsButton.getText().toString()
        );
        ttsSettingsButton.performClick();

        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(NavigationTextToSpeechSettingsLauncher.ACTION_TTS_SETTINGS, startedIntent.getAction());
    }

    @Test
    public void aboutPageDatabaseButtonsOpenDocumentPickers() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Button exportDatabaseButton = activity.findViewById(R.id.aboutExportDatabaseButton);
        Button importDatabaseButton = activity.findViewById(R.id.aboutImportDatabaseButton);

        exportDatabaseButton.performClick();
        ShadowActivity.IntentForResult exportIntent = shadowOf(activity).getNextStartedActivityForResult();

        importDatabaseButton.performClick();
        ShadowActivity.IntentForResult importIntent = shadowOf(activity).getNextStartedActivityForResult();

        assertEquals(Intent.ACTION_CREATE_DOCUMENT, exportIntent.intent.getAction());
        assertEquals("application/json", exportIntent.intent.getType());
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, importIntent.intent.getAction());
        assertEquals("application/json", importIntent.intent.getType());
    }

    @Test
    public void aboutPageLinksOpenProjectUrls() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        TextView sourceCodeLink = activity.findViewById(R.id.aboutSourceCodeLink);
        TextView reportIssueLink = activity.findViewById(R.id.aboutReportIssueLink);
        TextView privacyPolicyLink = activity.findViewById(R.id.aboutPrivacyPolicyLink);
        TextView termsOfServiceLink = activity.findViewById(R.id.aboutTermsOfServiceLink);
        TextView credits = activity.findViewById(R.id.aboutCredits);
        int expectedProjectLinkColor = credits.getLinkTextColors().getDefaultColor();

        assertNotEquals(ContextCompat.getColor(activity, R.color.success), expectedProjectLinkColor);
        assertEquals(expectedProjectLinkColor, sourceCodeLink.getCurrentTextColor());
        assertEquals(expectedProjectLinkColor, reportIssueLink.getCurrentTextColor());
        assertEquals(expectedProjectLinkColor, privacyPolicyLink.getCurrentTextColor());
        assertEquals(expectedProjectLinkColor, termsOfServiceLink.getCurrentTextColor());
        assertTrue(TextViewCompat.getCompoundDrawableTintList(sourceCodeLink) == null);
        assertTrue(TextViewCompat.getCompoundDrawableTintList(reportIssueLink) == null);
        assertTrue(TextViewCompat.getCompoundDrawableTintList(privacyPolicyLink) == null);
        assertTrue(TextViewCompat.getCompoundDrawableTintList(termsOfServiceLink) == null);
        sourceCodeLink.performClick();
        Intent sourceIntent = shadowOf(activity).getNextStartedActivity();
        reportIssueLink.performClick();
        Intent issueIntent = shadowOf(activity).getNextStartedActivity();
        privacyPolicyLink.performClick();
        Intent privacyIntent = shadowOf(activity).getNextStartedActivity();
        termsOfServiceLink.performClick();
        Intent termsIntent = shadowOf(activity).getNextStartedActivity();

        assertEquals(Intent.ACTION_VIEW, sourceIntent.getAction());
        assertEquals("https://github.com/damianofalcioni/ViBRo-Navigator", sourceIntent.getDataString());
        assertEquals(Intent.ACTION_VIEW, issueIntent.getAction());
        assertEquals("https://github.com/damianofalcioni/ViBRo-Navigator/issues/new", issueIntent.getDataString());
        assertEquals(Intent.ACTION_VIEW, privacyIntent.getAction());
        assertEquals(
                "https://damianofalcioni.github.io/ViBRo-Navigator/privacy-policy/",
                privacyIntent.getDataString()
        );
        assertEquals(Intent.ACTION_VIEW, termsIntent.getAction());
        assertEquals(
                "https://damianofalcioni.github.io/ViBRo-Navigator/terms-of-service/",
                termsIntent.getDataString()
        );
    }

    @Test
    public void aboutPageCanSendSymbolTestNotificationFromDiagnostics() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Button symbolTestLeftButton = activity.findViewById(R.id.aboutSymbolTestLeftButton);
        Button symbolTestOtherButton = activity.findViewById(R.id.aboutSymbolTestOtherButton);
        Button symbolTestRightButton = activity.findViewById(R.id.aboutSymbolTestRightButton);
        NotificationManager notificationManager = activity.getSystemService(NotificationManager.class);
        AppNotificationSettings.setNavigationNotificationsEnabled(activity, false);
        notificationManager.cancelAll();

        assertEquals(View.VISIBLE, symbolTestLeftButton.getVisibility());
        assertEquals(View.VISIBLE, symbolTestOtherButton.getVisibility());
        assertEquals(View.VISIBLE, symbolTestRightButton.getVisibility());

        symbolTestLeftButton.performClick();
        symbolTestOtherButton.performClick();
        symbolTestRightButton.performClick();

        assertEquals(
                activity.getString(R.string.msg_symbol_test_notification_sent),
                ShadowToast.getTextOfLatestToast()
        );
        assertEquals(3, notificationManager.getActiveNotifications().length);
    }

    private static void performFiveTaps(View view) {
        for (int i = 0; i < 5; i++) {
            view.performClick();
        }
    }

    private static void idleInitialDiagnosticRender() {
        shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS);
    }

    private static void idleDeferredSettingApply() {
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
    }

    private static EditText categoryFieldAt(LinearLayout categoryList, int rowIndex) {
        LinearLayout row = (LinearLayout) categoryList.getChildAt(rowIndex);
        return (EditText) row.getChildAt(0);
    }

    private static Switch categorySwitchAt(LinearLayout categoryList, int rowIndex) {
        LinearLayout row = (LinearLayout) categoryList.getChildAt(rowIndex);
        return (Switch) row.getChildAt(1);
    }

    private static void assertGooglePoiApiKeySetting(AboutActivity activity) {
        TextView label = activity.findViewById(R.id.aboutGooglePoiApiKeyLabel);
        ImageButton button = activity.findViewById(R.id.aboutGooglePoiApiKeyButton);
        Switch enabledSwitch = activity.findViewById(R.id.aboutGooglePoiSearchSwitch);

        assertEquals(activity.getString(R.string.label_google_poi_api_key), label.getText().toString());
        assertEquals(
                activity.getString(R.string.action_edit_google_poi_api_key),
                button.getContentDescription().toString()
        );
        assertEquals(
                activity.getString(R.string.action_enable_google_poi_search),
                enabledSwitch.getContentDescription().toString()
        );
        assertFalse(enabledSwitch.isEnabled());
        assertFalse(enabledSwitch.isChecked());
    }

    private static void assertAndroidAutoSetting(AboutActivity activity, Switch enabledSwitch) {
        boolean supported = DistributionServices.supportsAndroidAutoIntegration();

        assertEquals(activity.getString(R.string.label_android_auto_integration_enabled),
                enabledSwitch.getText().toString());
        assertEquals(supported ? View.VISIBLE : View.GONE, enabledSwitch.getVisibility());
        assertEquals(supported, enabledSwitch.isEnabled());
        assertEquals(supported, enabledSwitch.isChecked());
    }

    private static void assertHasStartIcon(Button button) {
        assertTrue(button.getCompoundDrawablesRelative()[0] != null);
        assertTrue(button.getCompoundDrawablePadding() > 0);
    }

}
