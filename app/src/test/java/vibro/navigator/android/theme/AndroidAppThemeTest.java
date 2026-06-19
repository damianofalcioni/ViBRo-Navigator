package vibro.navigator.android.theme;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.R;
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

    @Test
    public void darkThemeDoesNotUseSuccessAsPlatformAccent() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidAppTheme.apply(activity);

        assertNotEquals(ContextCompat.getColor(activity, R.color.success), platformThemeColor(
                activity,
                android.R.attr.colorAccent
        ));
    }

    @Test
    public void darkThemeDoesNotUseSuccessAsPlatformLinkColor() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidAppTheme.apply(activity);

        assertNotEquals(ContextCompat.getColor(activity, R.color.success), platformThemeColor(
                activity,
                android.R.attr.textColorLink
        ));
    }

    @Test
    public void lightThemeDoesNotUseSuccessAsPlatformAccent() {
        Application context = ApplicationProvider.getApplicationContext();
        AppThemeSettings.setLightThemeEnabled(context, true);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidAppTheme.apply(activity);

        assertNotEquals(ContextCompat.getColor(activity, R.color.success), platformThemeColor(
                activity,
                android.R.attr.colorAccent
        ));
    }

    @Test
    public void lightThemeDoesNotUseSuccessAsPlatformLinkColor() {
        Application context = ApplicationProvider.getApplicationContext();
        AppThemeSettings.setLightThemeEnabled(context, true);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidAppTheme.apply(activity);

        assertNotEquals(ContextCompat.getColor(activity, R.color.success), platformThemeColor(
                activity,
                android.R.attr.textColorLink
        ));
    }

    private static int platformThemeColor(Activity activity, int attrResId) {
        TypedValue value = new TypedValue();
        assertTrue(activity.getTheme().resolveAttribute(attrResId, value, true));
        if (value.resourceId != 0) {
            return ContextCompat.getColor(activity, value.resourceId);
        }
        return value.data;
    }
}
