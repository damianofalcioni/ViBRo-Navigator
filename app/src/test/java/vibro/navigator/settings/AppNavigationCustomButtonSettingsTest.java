package vibro.navigator.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;

@RunWith(RobolectricTestRunner.class)
public class AppNavigationCustomButtonSettingsTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void defaultsToHiddenDynamicGpsButton() {
        assertFalse(AppNavigationCustomButtonSettings.isEnabled(context));
        assertEquals(Target.DYNAMIC_GPS_INTERVAL, AppNavigationCustomButtonSettings.getTarget(context));
    }

    @Test
    public void persistsEnabledAndSelectedTarget() {
        AppNavigationCustomButtonSettings.setEnabled(context, true);
        AppNavigationCustomButtonSettings.setTarget(context, Target.SPEECH_DIRECTIONS);

        assertTrue(AppNavigationCustomButtonSettings.isEnabled(context));
        assertEquals(Target.SPEECH_DIRECTIONS, AppNavigationCustomButtonSettings.getTarget(context));
    }

    @Test
    public void unknownSavedTargetFallsBackToDynamicGps() {
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE)
                .edit()
                .putString("navigation_custom_button_target", "unknown")
                .commit();

        assertEquals(Target.DYNAMIC_GPS_INTERVAL, AppNavigationCustomButtonSettings.getTarget(context));
    }
}
