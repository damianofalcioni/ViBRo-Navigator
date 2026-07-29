package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.widget.Switch;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.settings.AppGpxSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutGpxSettingsRobolectricTest {

    @Before
    public void setUp() {
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void aboutPageShowsAutoSaveGpxSwitchEnabledByDefault() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch autoSaveGpxSwitch = activity.findViewById(R.id.aboutAutoSaveGpxSwitch);

        assertEquals(
                activity.getString(R.string.label_auto_save_gpx_enabled),
                autoSaveGpxSwitch.getText().toString()
        );
        assertTrue(autoSaveGpxSwitch.isChecked());
    }

    @Test
    public void aboutPageAutoSaveGpxSwitchPersistsPreference() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch autoSaveGpxSwitch = activity.findViewById(R.id.aboutAutoSaveGpxSwitch);

        assertTrue(AppGpxSettings.isAutoSaveOnStopEnabled(activity));

        autoSaveGpxSwitch.performClick();

        assertFalse(autoSaveGpxSwitch.isChecked());
        assertTrue(AppGpxSettings.isAutoSaveOnStopEnabled(activity));
        idleDeferredSettingApply();

        assertFalse(AppGpxSettings.isAutoSaveOnStopEnabled(activity));
    }

    private static void idleDeferredSettingApply() {
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
    }
}
