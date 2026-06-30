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
import vibro.navigator.settings.AppNotificationSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutNotificationSettingsRobolectricTest {

    @Before
    public void setUp() {
        AppNotificationSettings.setNavigationNotificationsEnabled(
                ApplicationProvider.getApplicationContext(),
                true
        );
    }

    @Test
    public void aboutPageShowsNavigationNotificationsSwitchEnabledByDefault() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Switch notificationsSwitch = activity.findViewById(R.id.aboutNavigationNotificationsSwitch);

        assertEquals(
                activity.getString(R.string.label_navigation_notifications_enabled),
                notificationsSwitch.getText().toString()
        );
        assertTrue(notificationsSwitch.isChecked());
    }

    @Test
    public void aboutPageNavigationNotificationsSwitchPersistsPreference() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Switch notificationsSwitch = activity.findViewById(R.id.aboutNavigationNotificationsSwitch);

        assertTrue(AppNotificationSettings.areNavigationNotificationsEnabled(activity));

        notificationsSwitch.performClick();

        assertFalse(notificationsSwitch.isChecked());
        assertTrue(AppNotificationSettings.areNavigationNotificationsEnabled(activity));
        idleDeferredSettingApply();

        assertFalse(AppNotificationSettings.areNavigationNotificationsEnabled(activity));
    }

    private static void idleDeferredSettingApply() {
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
    }
}
