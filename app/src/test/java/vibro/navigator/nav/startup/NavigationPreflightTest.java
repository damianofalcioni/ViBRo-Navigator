package vibro.navigator.nav.startup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class NavigationPreflightTest {

    @Test
    public void hasAnyLocationPermission_returnsTrueWhenOnlyCoarseIsGranted() {
        assertTrue(NavigationPreflight.hasAnyLocationPermission(false, true));
    }

    @Test
    public void hasAnyLocationPermission_returnsFalseWhenNoLocationPermissionIsGranted() {
        assertFalse(NavigationPreflight.hasAnyLocationPermission(false, false));
    }

    @Test
    public void statusHasLocationPermission_requiresEitherAndroidLocationPermission() {
        NavigationPreflight.Status missingLocation = NavigationPreflight.Status.create(
                Arrays.asList(
                        NavigationPreflight.PERMISSION_FINE_LOCATION,
                        NavigationPreflight.PERMISSION_COARSE_LOCATION
                ),
                false,
                true,
                true,
                false
        );
        NavigationPreflight.Status hasLocation = NavigationPreflight.Status.create(
                Collections.emptyList(),
                false,
                true,
                true,
                false
        );

        assertFalse(missingLocation.hasLocationPermission());
        assertTrue(hasLocation.hasLocationPermission());
    }

    @Test
    public void statusHasNotificationAccess_requiresPermissionAndEnabledAppNotifications() {
        NavigationPreflight.Status missingPermission = NavigationPreflight.Status.create(
                Collections.singletonList(NavigationPreflight.PERMISSION_POST_NOTIFICATIONS),
                false,
                true,
                true,
                false
        );
        NavigationPreflight.Status disabled = NavigationPreflight.Status.create(
                Collections.emptyList(),
                false,
                true,
                false,
                false
        );
        NavigationPreflight.Status allowed = NavigationPreflight.Status.create(
                Collections.emptyList(),
                false,
                true,
                true,
                false
        );

        assertFalse(missingPermission.hasNotificationAccess());
        assertFalse(disabled.hasNotificationAccess());
        assertTrue(allowed.hasNotificationAccess());
    }
}
