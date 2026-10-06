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
public class AboutCompassDistanceCirclesSettingsRobolectricTest {
    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void switchDefaultsEnabledAndPersistsBothDirections() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch switchView = activity.findViewById(R.id.aboutCompassDistanceCirclesSwitch);
        assertEquals("Distance circles", switchView.getText().toString());
        assertTrue(switchView.isChecked());
        assertTrue(AppCompassSettings.isDistanceCirclesEnabled(activity));

        switchView.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
        assertFalse(AppCompassSettings.isDistanceCirclesEnabled(activity));

        AboutActivity reopened = AboutActivityTestSupport.setupWithSettings();
        Switch reopenedSwitch = reopened.findViewById(R.id.aboutCompassDistanceCirclesSwitch);
        assertFalse(reopenedSwitch.isChecked());
        reopenedSwitch.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
        assertTrue(AppCompassSettings.isDistanceCirclesEnabled(reopened));
    }
}
