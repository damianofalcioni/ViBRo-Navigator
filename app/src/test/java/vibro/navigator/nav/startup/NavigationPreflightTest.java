package vibro.navigator.nav.startup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationPreflightTest {

    @Test
    public void hasAnyLocationPermission_returnsTrueWhenOnlyCoarseIsGranted() {
        assertTrue(NavigationPreflight.hasAnyLocationPermission(false, true));
    }

    @Test
    public void hasAnyLocationPermission_returnsFalseWhenNoLocationPermissionIsGranted() {
        assertFalse(NavigationPreflight.hasAnyLocationPermission(false, false));
    }
}
