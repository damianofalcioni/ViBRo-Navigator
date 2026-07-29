package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
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
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void fullscreenRouteSwitchStartsEnabledWithLabel() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch fullscreenRouteSwitch = activity.findViewById(R.id.aboutCompassFullscreenRouteSwitch);

        assertEquals(
                activity.getString(R.string.label_compass_fullscreen_route_enabled),
                fullscreenRouteSwitch.getText().toString()
        );
        assertTrue(fullscreenRouteSwitch.isChecked());
    }

    @Test
    public void fullscreenRouteSwitchPersistsPreference() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch fullscreenRouteSwitch = activity.findViewById(R.id.aboutCompassFullscreenRouteSwitch);

        fullscreenRouteSwitch.performClick();

        assertFalse(fullscreenRouteSwitch.isChecked());
        assertTrue(AppCompassSettings.isFullscreenRouteEnabled(activity));
        idleDeferredSettingApply();

        assertFalse(AppCompassSettings.isFullscreenRouteEnabled(activity));
    }

    private static void idleDeferredSettingApply() {
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
    }
}
