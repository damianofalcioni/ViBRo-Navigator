package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

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
        AppGpxSettings.setAutoSaveOnStopEnabled(
                ApplicationProvider.getApplicationContext(),
                false
        );
    }

    @Test
    public void aboutPageShowsAutoSaveGpxSwitchDisabledByDefault() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch autoSaveGpxSwitch = activity.findViewById(R.id.aboutAutoSaveGpxSwitch);

        assertEquals(
                activity.getString(R.string.label_auto_save_gpx_enabled),
                autoSaveGpxSwitch.getText().toString()
        );
        assertFalse(autoSaveGpxSwitch.isChecked());
    }

    @Test
    public void aboutPageAutoSaveGpxSwitchPersistsPreference() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch autoSaveGpxSwitch = activity.findViewById(R.id.aboutAutoSaveGpxSwitch);

        assertFalse(AppGpxSettings.isAutoSaveOnStopEnabled(activity));

        autoSaveGpxSwitch.performClick();

        assertTrue(autoSaveGpxSwitch.isChecked());
        assertFalse(AppGpxSettings.isAutoSaveOnStopEnabled(activity));
        idleDeferredSettingApply();

        assertTrue(AppGpxSettings.isAutoSaveOnStopEnabled(activity));
    }

    private static void idleDeferredSettingApply() {
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
    }
}
