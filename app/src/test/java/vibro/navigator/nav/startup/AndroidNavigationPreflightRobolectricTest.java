package vibro.navigator.nav.startup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import vibro.navigator.android.startup.AndroidNavigationPreflight;
import vibro.navigator.settings.AppCompassSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O)
public class AndroidNavigationPreflightRobolectricTest {
    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppCompassSettings.setSurroundingStreetsEnabled(context, false);
    }

    @Test
    public void inspectRequestsLegacyStreetStorageWhenSurroundingStreetsAreEnabled() {
        Application context = ApplicationProvider.getApplicationContext();
        AppCompassSettings.setSurroundingStreetsEnabled(context, true);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        NavigationPreflight.Status status = AndroidNavigationPreflight.inspect(activity);

        assertTrue(status.missingPermissions.contains(NavigationPreflight.PERMISSION_READ_EXTERNAL_STORAGE));
    }

    @Test
    public void inspectSkipsLegacyStreetStorageWhenSurroundingStreetsAreDisabled() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        NavigationPreflight.Status status = AndroidNavigationPreflight.inspect(activity);

        assertFalse(status.missingPermissions.contains(NavigationPreflight.PERMISSION_READ_EXTERNAL_STORAGE));
    }
}
