package vibro.navigator.nav.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.pm.PackageManager;

import org.junit.Test;

import vibro.navigator.nav.startup.NavigationPreflight;

public class NavigationActivityPermissionResultHandlerTest {
    private boolean surroundingStreetsEnabled = true;

    @Test
    public void deniedLegacyStreetStorageDisablesSurroundingStreets() {
        boolean disabled = NavigationActivityPermissionResultHandler.disableSurroundingStreetsWhenStorageDenied(
                new String[]{NavigationPreflight.PERMISSION_READ_EXTERNAL_STORAGE},
                new int[]{PackageManager.PERMISSION_DENIED},
                () -> surroundingStreetsEnabled = false
        );

        assertTrue(disabled);
        assertFalse(surroundingStreetsEnabled);
    }

    @Test
    public void grantedLegacyStreetStorageKeepsSurroundingStreetsEnabled() {
        boolean disabled = NavigationActivityPermissionResultHandler.disableSurroundingStreetsWhenStorageDenied(
                new String[]{NavigationPreflight.PERMISSION_READ_EXTERNAL_STORAGE},
                new int[]{PackageManager.PERMISSION_GRANTED},
                () -> surroundingStreetsEnabled = false
        );

        assertFalse(disabled);
        assertTrue(surroundingStreetsEnabled);
    }

    @Test
    public void unrelatedDeniedPermissionKeepsSurroundingStreetsEnabled() {
        boolean disabled = NavigationActivityPermissionResultHandler.disableSurroundingStreetsWhenStorageDenied(
                new String[]{NavigationPreflight.PERMISSION_FINE_LOCATION},
                new int[]{PackageManager.PERMISSION_DENIED},
                () -> surroundingStreetsEnabled = false
        );

        assertFalse(disabled);
        assertTrue(surroundingStreetsEnabled);
    }
}
