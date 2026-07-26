package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Application;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;

@RunWith(RobolectricTestRunner.class)
public class AboutNavigationCustomButtonSettingsRobolectricTest {
    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        AppNavigationCustomButtonSettings.setEnabled(context, false);
        AppNavigationCustomButtonSettings.setTarget(context, Target.DYNAMIC_GPS_INTERVAL);
    }

    @Test
    public void customButtonRowStartsDisabledWithConfigButton() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        TextView label = activity.findViewById(R.id.aboutNavigationCustomButtonLabel);
        ImageButton settingsButton = activity.findViewById(R.id.aboutNavigationCustomButtonSettingsButton);
        Switch enabledSwitch = activity.findViewById(R.id.aboutNavigationCustomButtonSwitch);

        assertEquals(activity.getString(R.string.label_navigation_custom_button), label.getText().toString());
        assertEquals(
                activity.getString(R.string.action_configure_navigation_custom_button),
                settingsButton.getContentDescription().toString()
        );
        assertFalse(enabledSwitch.isChecked());
    }

    @Test
    public void customButtonSwitchPersistsPreference() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Switch enabledSwitch = activity.findViewById(R.id.aboutNavigationCustomButtonSwitch);

        enabledSwitch.performClick();

        assertTrue(enabledSwitch.isChecked());
        assertFalse(AppNavigationCustomButtonSettings.isEnabled(activity));
        idleDeferredSettingApply();

        assertTrue(AppNavigationCustomButtonSettings.isEnabled(activity));
    }

    @Test
    public void customButtonConfigDialogPersistsSelectedTarget() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        ImageButton settingsButton = activity.findViewById(R.id.aboutNavigationCustomButtonSettingsButton);

        settingsButton.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(
                AboutDeferredDialogAction.OPEN_DELAY_MS + 50,
                TimeUnit.MILLISECONDS
        );
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        Spinner spinner = dialog.findViewById(R.id.aboutNavigationCustomButtonTargetSpinner);

        assertEquals(activity.getString(R.string.label_dynamic_gps_fix_interval_enabled), spinner.getItemAtPosition(0));
        assertEquals(activity.getString(R.string.label_light_theme_enabled), spinner.getItemAtPosition(1));
        assertEquals(activity.getString(R.string.label_compass_surrounding_streets_enabled), spinner.getItemAtPosition(2));
        assertEquals(activity.getString(R.string.label_compass_fullscreen_route_enabled), spinner.getItemAtPosition(3));
        assertEquals(activity.getString(R.string.label_navigation_notifications_enabled), spinner.getItemAtPosition(4));
        assertEquals(activity.getString(R.string.label_maneuver_voice), spinner.getItemAtPosition(5));

        spinner.setSelection(5);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(Target.SPEECH_DIRECTIONS, AppNavigationCustomButtonSettings.getTarget(activity));
    }

    private static void idleDeferredSettingApply() {
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
    }
}
