package vibro.navigator.nav.location;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.location.LocationManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NavigationLocationControllerTest {
    private static final String GPS_AND_NETWORK = "gps+network";

    @Test
    public void shouldReuseActiveLocationRequest_returnsTrueForMatchingIntervalAndProviders() {
        assertTrue(NavigationLocationController.shouldReuseActiveLocationRequest(
                1_000L,
                GPS_AND_NETWORK,
                1_000L,
                GPS_AND_NETWORK
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseForChangedInterval() {
        assertFalse(NavigationLocationController.shouldReuseActiveLocationRequest(
                2_000L,
                GPS_AND_NETWORK,
                1_000L,
                GPS_AND_NETWORK
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseForChangedProviders() {
        assertFalse(NavigationLocationController.shouldReuseActiveLocationRequest(
                1_000L,
                "gps",
                1_000L,
                GPS_AND_NETWORK
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseWhenNoProviderSummaryExists() {
        assertFalse(NavigationLocationController.shouldReuseActiveLocationRequest(
                1_000L,
                null,
                1_000L,
                GPS_AND_NETWORK
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
