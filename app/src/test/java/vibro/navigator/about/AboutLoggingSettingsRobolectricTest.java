package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.app.NotificationManager;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
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
        TextView sensorStatusTitle = activity.findViewById(R.id.aboutSensorStatusTitle);
        TextView sensorStatusBody = activity.findViewById(R.id.aboutSensorStatusBody);
        Button symbolTestButton = activity.findViewById(R.id.aboutSymbolTestButton);

        assertFalse(logEnabledSwitch.isChecked());
        assertEquals(View.VISIBLE, sensorStatusTitle.getVisibility());
        assertEquals(View.VISIBLE, sensorStatusBody.getVisibility());
        assertEquals(View.VISIBLE, symbolTestButton.getVisibility());
        assertTrue(sensorStatusBody.getText().toString().contains(
                activity.getString(R.string.label_sensor_gps_provider)
        ));
        assertTrue(sensorStatusBody.getText().toString().contains(
                activity.getString(R.string.label_sensor_network_provider)
        ));
        assertTrue(sensorStatusBody.getText().toString().contains(
                activity.getString(R.string.label_sensor_geomagnetic_rotation_vector)
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
    public void aboutPageCanSendSymbolTestNotificationFromDiagnostics() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Button symbolTestButton = activity.findViewById(R.id.aboutSymbolTestButton);
        NotificationManager notificationManager = activity.getSystemService(NotificationManager.class);

        assertEquals(View.VISIBLE, symbolTestButton.getVisibility());

        symbolTestButton.performClick();

        assertEquals(
                activity.getString(R.string.msg_symbol_test_notification_sent),
                ShadowToast.getTextOfLatestToast()
        );
        assertTrue(notificationManager.getActiveNotifications().length > 0);
    }

    private static void performFiveTaps(View view) {
        for (int i = 0; i < 5; i++) {
            view.performClick();
        }
    }
}
