package vibro.navigator.nav.startup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.location.LocationManager;

import org.junit.Test;

public class NavigationStartupLocationSelectorTest {

    @Test
    public void isUsable_rejectsStaleFix() {
        long nowMs = 100_000L;
        NavigationStartupLocationSelector.Fix location =
                location(LocationManager.GPS_PROVIDER, nowMs - 16_000L, 8f);

        assertFalse(NavigationStartupLocationSelector.isUsableFix(location, nowMs));
    }

    @Test
    public void isUsable_rejectsLowQualityFix() {
        long nowMs = 100_000L;
        NavigationStartupLocationSelector.Fix location =
                location(LocationManager.NETWORK_PROVIDER, nowMs - 2_000L, 75f);

        assertFalse(NavigationStartupLocationSelector.isUsableFix(location, nowMs));
    }

    @Test
    public void selectBest_prefersFreshUsableFix() {
        long nowMs = 100_000L;
        NavigationStartupLocationSelector.Fix staleGps =
                location(LocationManager.GPS_PROVIDER, nowMs - 30_000L, 5f);
        NavigationStartupLocationSelector.Fix freshNetwork =
                location(LocationManager.NETWORK_PROVIDER, nowMs - 1_000L, 20f);

        NavigationStartupLocationSelector.Fix selected =
                NavigationStartupLocationSelector.selectBestFix(staleGps, freshNetwork, nowMs);

        assertNotNull(selected);
        assertEquals(LocationManager.NETWORK_PROVIDER, selected.provider);
    }

    @Test
    public void selectBest_returnsNullWhenNoUsableFixExists() {
        long nowMs = 100_000L;
        NavigationStartupLocationSelector.Fix staleGps =
                location(LocationManager.GPS_PROVIDER, nowMs - 20_000L, 5f);
        NavigationStartupLocationSelector.Fix inaccurateNetwork =
                location(LocationManager.NETWORK_PROVIDER, nowMs - 1_000L, 120f);

        NavigationStartupLocationSelector.Fix selected =
                NavigationStartupLocationSelector.selectBestFix(staleGps, inaccurateNetwork, nowMs);

        assertNull(selected);
    }

    @Test
    public void isUsable_acceptsFreshAccurateFix() {
        long nowMs = 100_000L;
        NavigationStartupLocationSelector.Fix location =
                location(LocationManager.GPS_PROVIDER, nowMs - 5_000L, 12f);

        assertTrue(NavigationStartupLocationSelector.isUsableFix(location, nowMs));
    }

    @Test
    public void isUsableForRouteStart_rejectsFixAboveWarmupAccuracy() {
        long nowMs = 100_000L;
        NavigationStartupLocationSelector.Fix location =
                location(LocationManager.GPS_PROVIDER, nowMs - 2_000L, 30f);

        assertFalse(NavigationStartupLocationSelector.isUsableForRouteStartFix(location, nowMs));
    }

    @Test
    public void isUsableForRouteStart_acceptsFreshWarmupAccurateFix() {
        long nowMs = 100_000L;
        NavigationStartupLocationSelector.Fix location =
                location(LocationManager.GPS_PROVIDER, nowMs - 2_000L, 25f);

        assertTrue(NavigationStartupLocationSelector.isUsableForRouteStartFix(location, nowMs));
    }

    private static NavigationStartupLocationSelector.Fix location(
            String provider,
            long timeMs,
            float accuracyMeters
    ) {
        return new NavigationStartupLocationSelector.Fix(provider, timeMs, accuracyMeters);
    }
}
