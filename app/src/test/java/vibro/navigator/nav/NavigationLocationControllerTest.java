package vibro.navigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.location.Location;
import android.location.LocationManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NavigationLocationControllerTest {

    @Test
    public void isUsableStartupLastKnownLocation_rejectsStaleFix() {
        long nowMs = 100_000L;
        Location location = location(LocationManager.GPS_PROVIDER, nowMs - 16_000L, 8f);

        assertFalse(NavigationLocationController.isUsableStartupLastKnownLocation(location, nowMs));
    }

    @Test
    public void isUsableStartupLastKnownLocation_rejectsLowQualityFix() {
        long nowMs = 100_000L;
        Location location = location(LocationManager.NETWORK_PROVIDER, nowMs - 2_000L, 75f);

        assertFalse(NavigationLocationController.isUsableStartupLastKnownLocation(location, nowMs));
    }

    @Test
    public void selectBestStartupLastKnownLocation_prefersFreshUsableFix() {
        long nowMs = 100_000L;
        Location staleGps = location(LocationManager.GPS_PROVIDER, nowMs - 30_000L, 5f);
        Location freshNetwork = location(LocationManager.NETWORK_PROVIDER, nowMs - 1_000L, 20f);

        Location selected =
                NavigationLocationController.selectBestStartupLastKnownLocation(staleGps, freshNetwork, nowMs);

        assertNotNull(selected);
        assertEquals(LocationManager.NETWORK_PROVIDER, selected.getProvider());
    }

    @Test
    public void selectBestStartupLastKnownLocation_returnsNullWhenNoUsableFixExists() {
        long nowMs = 100_000L;
        Location staleGps = location(LocationManager.GPS_PROVIDER, nowMs - 20_000L, 5f);
        Location inaccurateNetwork = location(LocationManager.NETWORK_PROVIDER, nowMs - 1_000L, 120f);

        Location selected =
                NavigationLocationController.selectBestStartupLastKnownLocation(staleGps, inaccurateNetwork, nowMs);

        assertNull(selected);
    }

    @Test
    public void isUsableStartupLastKnownLocation_acceptsFreshAccurateFix() {
        long nowMs = 100_000L;
        Location location = location(LocationManager.GPS_PROVIDER, nowMs - 5_000L, 12f);

        assertTrue(NavigationLocationController.isUsableStartupLastKnownLocation(location, nowMs));
    }

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
    public void countSatellitesUsedInFix_countsOnlyTrueFlags() {
        assertEquals(3, NavigationLocationController.countSatellitesUsedInFix(
                true,
                false,
                true,
                false,
                true
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

    private static Location location(String provider, long timeMs, float accuracyMeters) {
        Location location = new Location(provider);
        location.setLatitude(48.2082d);
        location.setLongitude(16.3738d);
        location.setTime(timeMs);
        location.setAccuracy(accuracyMeters);
        return location;
    }
}
