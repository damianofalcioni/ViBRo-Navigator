package vibro.navigator.nav.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.nav.startup.NavigationPreflight;
import vibro.navigator.settings.AppCompassSettings;

@RunWith(RobolectricTestRunner.class)
public class NavigationActivityPermissionResultHandlerTest {
    private Application context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        AppCompassSettings.setSurroundingStreetsEnabled(context, true);
    }

    @Test
    public void deniedLegacyStreetStorageDisablesSurroundingStreets() {
        boolean disabled = NavigationActivityPermissionResultHandler.disableSurroundingStreetsWhenStorageDenied(
                context,
                new String[]{NavigationPreflight.PERMISSION_READ_EXTERNAL_STORAGE},
                new int[]{PackageManager.PERMISSION_DENIED}
        );

        assertTrue(disabled);
        assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(context));
    }

    @Test
    public void grantedLegacyStreetStorageKeepsSurroundingStreetsEnabled() {
        boolean disabled = NavigationActivityPermissionResultHandler.disableSurroundingStreetsWhenStorageDenied(
                context,
                new String[]{NavigationPreflight.PERMISSION_READ_EXTERNAL_STORAGE},
                new int[]{PackageManager.PERMISSION_GRANTED}
        );

        assertFalse(disabled);
        assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(context));
    }

    @Test
    public void unrelatedDeniedPermissionKeepsSurroundingStreetsEnabled() {
        boolean disabled = NavigationActivityPermissionResultHandler.disableSurroundingStreetsWhenStorageDenied(
                context,
                new String[]{NavigationPreflight.PERMISSION_FINE_LOCATION},
                new int[]{PackageManager.PERMISSION_DENIED}
        );

        assertFalse(disabled);
        assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(context));
    }
}
