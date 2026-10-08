package vibro.navigator.about;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.widget.Switch;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppDataBackup;
import vibro.navigator.settings.AppNavigationHintSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutNavigationHintSettingsRobolectricTest {
    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void switchDefaultsOnDefersWritesAndPersistsBothDirections() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch switchView = activity.findViewById(R.id.aboutShowHintPanelSwitch);
        assertTrue(switchView.isChecked());
        assertTrue(AppNavigationHintSettings.isEnabled(activity));
        switchView.performClick();
        assertFalse(switchView.isChecked());
        assertTrue(AppNavigationHintSettings.isEnabled(activity));
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
        assertFalse(AppNavigationHintSettings.isEnabled(activity));
        AboutActivity reopened = AboutActivityTestSupport.setupWithSettings();
        Switch reopenedSwitch = reopened.findViewById(R.id.aboutShowHintPanelSwitch);
        assertFalse(reopenedSwitch.isChecked());
        reopenedSwitch.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
        assertTrue(AppNavigationHintSettings.isEnabled(reopened));
    }

    @Test
    public void pendingSwitchFlushesBeforeExportAndRefreshesAfterImport() throws JSONException {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AboutNavigationHintSettings settings = new AboutNavigationHintSettings(activity);
        Switch switchView = activity.findViewById(R.id.aboutShowHintPanelSwitch);
        switchView.performClick();
        settings.flush();
        String backup = AppDataBackup.exportJson(activity);
        AppNavigationHintSettings.setEnabled(activity, true);
        settings.refresh();
        assertTrue(switchView.isChecked());
        AppDataBackup.importJson(activity, backup);
        settings.refresh();
        assertFalse(switchView.isChecked());
        assertFalse(AppNavigationHintSettings.isEnabled(activity));
    }

    @Test
    public void leavingSettingsImmediatelyFlushesThePendingHintChange() {
        ActivityController<AboutActivity> controller = AboutActivityTestSupport.setupControllerWithSettings();
        AboutActivity activity = controller.get();
        Switch switchView = activity.findViewById(R.id.aboutShowHintPanelSwitch);
        switchView.performClick();
        assertTrue(AppNavigationHintSettings.isEnabled(activity));
        controller.pause().stop();
        assertFalse(AppNavigationHintSettings.isEnabled(activity));
        controller.destroy();
    }
}
