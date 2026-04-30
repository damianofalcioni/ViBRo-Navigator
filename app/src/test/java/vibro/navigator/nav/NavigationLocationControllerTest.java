package vibro.navigator.nav;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.location.LocationManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NavigationLocationControllerTest {

    @Test
    public void shouldReuseActiveLocationRequest_returnsTrueForMatchingIntervalAndProviders() {
        assertTrue(NavigationLocationController.shouldReuseActiveLocationRequest(
                1_000L,
                "gps+network",
                1_000L,
                "gps+network"
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseForChangedInterval() {
        assertFalse(NavigationLocationController.shouldReuseActiveLocationRequest(
                2_000L,
                "gps+network",
                1_000L,
                "gps+network"
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseForChangedProviders() {
        assertFalse(NavigationLocationController.shouldReuseActiveLocationRequest(
                1_000L,
                "gps",
                1_000L,
                "gps+network"
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseWhenNoProviderSummaryExists() {
        assertFalse(NavigationLocationController.shouldReuseActiveLocationRequest(
                1_000L,
                null,
                1_000L,
                "gps+network"
        ));
    }

    @Test
    public void canUseProvider_requiresFinePermissionForGps() {
        assertFalse(NavigationLocationController.canUseProvider(
                LocationManager.GPS_PROVIDER,
                false,
                true
        ));
        assertTrue(NavigationLocationController.canUseProvider(
                LocationManager.GPS_PROVIDER,
                true,
                false
        ));
    }

    @Test
    public void canUseProvider_allowsNetworkWithCoarsePermission() {
        assertTrue(NavigationLocationController.canUseProvider(
                LocationManager.NETWORK_PROVIDER,
                false,
                true
        ));
        assertTrue(NavigationLocationController.canUseProvider(
                LocationManager.PASSIVE_PROVIDER,
                false,
                true
        ));
    }

}
