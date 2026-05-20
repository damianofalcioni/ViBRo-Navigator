package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RunWith(RobolectricTestRunner.class)
public class AboutLoggingSettingsRobolectricTest {

    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        AppLogger.setLoggingEnabled(context, false);
        AppLogger.init(context);
        AppSettings.setImperialUnitsEnabled(context, false);
        AppSettings.setManeuverVoiceName(context, AppSettings.MANEUVER_VOICE_DISABLED);
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
        Switch logEnabledSwitch = activity.findViewById(R.id.aboutLogEnabledSwitch);
        Switch imperialUnitsSwitch = activity.findViewById(R.id.aboutImperialUnitsSwitch);
        TextView sensorStatusTitle = activity.findViewById(R.id.aboutSensorStatusTitle);
        TextView sensorStatusBody = activity.findViewById(R.id.aboutSensorStatusBody);
        TextView symbolTestTitle = activity.findViewById(R.id.aboutSymbolTestTitle);
        Button exportDatabaseButton = activity.findViewById(R.id.aboutExportDatabaseButton);
        Button importDatabaseButton = activity.findViewById(R.id.aboutImportDatabaseButton);
        Button symbolTestLeftButton = activity.findViewById(R.id.aboutSymbolTestLeftButton);
        Button symbolTestOtherButton = activity.findViewById(R.id.aboutSymbolTestOtherButton);
        Button symbolTestRightButton = activity.findViewById(R.id.aboutSymbolTestRightButton);
        View googlePoiApiKeyContainer = activity.findViewById(R.id.aboutGooglePoiApiKeyContainer);
        Spinner maneuverVoiceSpinner = activity.findViewById(R.id.aboutManeuverVoiceSpinner);
        ImageButton ttsSettingsButton = activity.findViewById(R.id.aboutTtsSettingsButton);

        assertFalse(logEnabledSwitch.isChecked());
        assertFalse(imperialUnitsSwitch.isChecked());
        assertEquals(View.VISIBLE, maneuverVoiceSpinner.getVisibility());
        assertEquals(View.VISIBLE, ttsSettingsButton.getVisibility());
        assertEquals(
                activity.getString(R.string.action_open_tts_settings),
                ttsSettingsButton.getContentDescription().toString()
        );
        assertEquals(
                activity.getString(R.string.label_maneuver_voice_disabled),
                maneuverVoiceSpinner.getSelectedItem().toString()
        );
        assertEquals(
                DistributionServices.supportsUserGooglePoiApiKey() ? View.VISIBLE : View.GONE,
                googlePoiApiKeyContainer.getVisibility()
        );
        assertEquals(View.VISIBLE, exportDatabaseButton.getVisibility());
        assertEquals(View.VISIBLE, importDatabaseButton.getVisibility());
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

        assertTrue(AppLogger.isLoggingEnabled(activity));
        assertTrue(new File(AppLogger.getLogFilePath(activity)).exists());
    }

    @Test
    public void aboutPageImperialUnitsSwitchPersistsPreference() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Switch imperialUnitsSwitch = activity.findViewById(R.id.aboutImperialUnitsSwitch);

        assertFalse(AppSettings.isImperialUnitsEnabled(activity));

        imperialUnitsSwitch.performClick();

        assertTrue(AppSettings.isImperialUnitsEnabled(activity));
    }

    @Test
    public void aboutPageTtsSettingsButtonOpensTextToSpeechSettings() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        ImageButton ttsSettingsButton = activity.findViewById(R.id.aboutTtsSettingsButton);

        ttsSettingsButton.performClick();

        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(AboutTtsSettingsLauncher.ACTION_TTS_SETTINGS, startedIntent.getAction());
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
    public void aboutPageCanSendSymbolTestNotificationFromDiagnostics() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Button symbolTestLeftButton = activity.findViewById(R.id.aboutSymbolTestLeftButton);
        Button symbolTestOtherButton = activity.findViewById(R.id.aboutSymbolTestOtherButton);
        Button symbolTestRightButton = activity.findViewById(R.id.aboutSymbolTestRightButton);
        NotificationManager notificationManager = activity.getSystemService(NotificationManager.class);
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
}
