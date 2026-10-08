package vibro.navigator.about;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.widget.Switch;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import org.json.JSONException;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppCompassSettings;
import vibro.navigator.settings.AppDataBackup;

@RunWith(RobolectricTestRunner.class)
public class AboutCompassProjectionSettingsRobolectricTest {
    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void switchDefaultsOnDefersWritesAndPersistsBothDirections() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch switchView = activity.findViewById(R.id.aboutCompassCentralPerspectiveSwitch);
        assertTrue(switchView.isChecked());
        assertTrue(AppCompassSettings.isCentralPerspectiveEnabled(activity));
        switchView.performClick();
        assertFalse(switchView.isChecked());
        assertTrue(AppCompassSettings.isCentralPerspectiveEnabled(activity));
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
        assertFalse(AppCompassSettings.isCentralPerspectiveEnabled(activity));
        AboutActivity reopened = AboutActivityTestSupport.setupWithSettings();
        Switch reopenedSwitch = reopened.findViewById(R.id.aboutCompassCentralPerspectiveSwitch);
        assertFalse(reopenedSwitch.isChecked());
        reopenedSwitch.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
        assertTrue(AppCompassSettings.isCentralPerspectiveEnabled(reopened));
    }

    @Test
    public void pendingSwitchFlushesBeforeExportAndRefreshesAfterImport() throws JSONException {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AboutCompassProjectionSettings settings = new AboutCompassProjectionSettings(activity);
        Switch switchView = activity.findViewById(R.id.aboutCompassCentralPerspectiveSwitch);
        switchView.performClick();
        settings.flush();
        String backup = AppDataBackup.exportJson(activity);
        AppCompassSettings.setCentralPerspectiveEnabled(activity, true);
        settings.refresh();
        assertTrue(switchView.isChecked());
        AppDataBackup.importJson(activity, backup);
        settings.refresh();
        assertFalse(switchView.isChecked());
        assertFalse(AppCompassSettings.isCentralPerspectiveEnabled(activity));
    }

    @Test
    public void leavingSettingsImmediatelyFlushesThePendingProjectionChange() {
        ActivityController<AboutActivity> controller = AboutActivityTestSupport.setupControllerWithSettings();
        AboutActivity activity = controller.get();
        Switch switchView = activity.findViewById(R.id.aboutCompassCentralPerspectiveSwitch);
        switchView.performClick();
        assertTrue(AppCompassSettings.isCentralPerspectiveEnabled(activity));
        controller.pause().stop();
        assertFalse(AppCompassSettings.isCentralPerspectiveEnabled(activity));
        controller.destroy();
    }
}
