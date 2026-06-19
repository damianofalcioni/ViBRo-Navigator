package vibro.navigator.android.theme;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.settings.AppThemeSettings;

@RunWith(RobolectricTestRunner.class)
public class AndroidAppThemeTest {

    @Before
    public void setUp() {
        AppThemeSettings.setLightThemeEnabled(ApplicationProvider.getApplicationContext(), false);
    }

    @Test
    public void recreateIfThemeChangedIgnoresMatchingAppliedTheme() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        assertFalse(AndroidAppTheme.recreateIfThemeChanged(activity, false));
    }

    @Test
    public void recreateIfThemeChangedRecreatesWhenPreferenceChanged() {
        Application context = ApplicationProvider.getApplicationContext();
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AppThemeSettings.setLightThemeEnabled(context, true);

        assertTrue(AndroidAppTheme.recreateIfThemeChanged(activity, false));
    }
}
