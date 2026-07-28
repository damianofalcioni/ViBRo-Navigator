package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.os.Looper;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.settings.AppThemeSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutThemeSettingsRobolectricTest {

    @Before
    public void setUp() {
        AppThemeSettings.setLightThemeEnabled(ApplicationProvider.getApplicationContext(), false);
    }

    @Test
    public void aboutPageShowsLightThemeSwitchDisabledByDefault() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch lightThemeSwitch = activity.findViewById(R.id.aboutLightThemeSwitch);

        assertEquals(activity.getString(R.string.label_light_theme_enabled), lightThemeSwitch.getText().toString());
        assertFalse(lightThemeSwitch.isChecked());
    }

    @Test
    public void aboutPageLightThemeSwitchPersistsPreference() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch lightThemeSwitch = activity.findViewById(R.id.aboutLightThemeSwitch);

        assertFalse(AppThemeSettings.isLightThemeEnabled(activity));

        lightThemeSwitch.performClick();

        assertTrue(lightThemeSwitch.isChecked());
        assertFalse(AppThemeSettings.isLightThemeEnabled(activity));
        idleDeferredSettingApply();

        assertTrue(AppThemeSettings.isLightThemeEnabled(activity));
    }

    @Test
    public void aboutPageAppliesSavedLightTheme() {
        Application context = ApplicationProvider.getApplicationContext();
        AppThemeSettings.setLightThemeEnabled(context, true);

        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        TextView title = activity.findViewById(R.id.aboutTitle);
        Switch lightThemeSwitch = activity.findViewById(R.id.aboutLightThemeSwitch);

        assertTrue(lightThemeSwitch.isChecked());
        assertEquals(ContextCompat.getColor(activity, R.color.light_text_primary), title.getCurrentTextColor());
    }

    private static void idleDeferredSettingApply() {
        shadowOf(Looper.getMainLooper()).idleFor(350, TimeUnit.MILLISECONDS);
    }
}
