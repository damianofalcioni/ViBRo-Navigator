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
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppCompassSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutCompassStationaryFullRouteZoomSettingsRobolectricTest {
    private static final String PREFS_APP_SETTINGS = "vibro.navigator.settings";

    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        context.getSharedPreferences(PREFS_APP_SETTINGS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void stationaryFullRouteZoomSwitchStartsDisabledWithLabel() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch stationaryFullRouteZoomSwitch =
                activity.findViewById(R.id.aboutCompassStationaryFullRouteZoomSwitch);

        assertEquals(
                activity.getString(R.string.label_compass_stationary_full_route_zoom_enabled),
                stationaryFullRouteZoomSwitch.getText().toString()
        );
        assertFalse(stationaryFullRouteZoomSwitch.isChecked());
        assertFalse(AppCompassSettings.isStationaryFullRouteZoomEnabled(activity));
    }

    @Test
    public void stationaryFullRouteZoomSwitchPersistsPreference() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch stationaryFullRouteZoomSwitch =
                activity.findViewById(R.id.aboutCompassStationaryFullRouteZoomSwitch);

        stationaryFullRouteZoomSwitch.performClick();

        assertTrue(stationaryFullRouteZoomSwitch.isChecked());
        assertFalse(AppCompassSettings.isStationaryFullRouteZoomEnabled(activity));
        idleDeferredSettingApply();

        assertTrue(AppCompassSettings.isStationaryFullRouteZoomEnabled(activity));
    }

    private static void idleDeferredSettingApply() {
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
    }
}
