package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
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
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppCompassSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutCompassFullscreenRouteSettingsRobolectricTest {
    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        AppCompassSettings.setFullscreenRouteEnabled(context, false);
    }

    @Test
    public void fullscreenRouteSwitchStartsDisabledWithLabel() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch fullscreenRouteSwitch = activity.findViewById(R.id.aboutCompassFullscreenRouteSwitch);

        assertEquals(
                activity.getString(R.string.label_compass_fullscreen_route_enabled),
                fullscreenRouteSwitch.getText().toString()
        );
        assertFalse(fullscreenRouteSwitch.isChecked());
    }

    @Test
    public void fullscreenRouteSwitchPersistsPreference() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch fullscreenRouteSwitch = activity.findViewById(R.id.aboutCompassFullscreenRouteSwitch);

        fullscreenRouteSwitch.performClick();

        assertTrue(fullscreenRouteSwitch.isChecked());
        assertFalse(AppCompassSettings.isFullscreenRouteEnabled(activity));
        idleDeferredSettingApply();

        assertTrue(AppCompassSettings.isFullscreenRouteEnabled(activity));
    }

    private static void idleDeferredSettingApply() {
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
    }
}
