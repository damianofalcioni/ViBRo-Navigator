package vibro.navigator.nav.startup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationFix;

public class NavigationStartupLocationSelectorTest {
    private static final String GPS_PROVIDER = "gps";
    private static final String NETWORK_PROVIDER = "network";

    @Test
    public void isUsable_rejectsStaleFix() {
        long nowMs = 100_000L;
        NavigationLocationFix location =
                location(GPS_PROVIDER, nowMs - 16_000L, 8f);

        assertFalse(NavigationStartupLocationSelector.isUsableFix(location, nowMs));
    }

    @Test
    public void isUsable_rejectsLowQualityFix() {
        long nowMs = 100_000L;
        NavigationLocationFix location =
                location(NETWORK_PROVIDER, nowMs - 2_000L, 75f);

        assertFalse(NavigationStartupLocationSelector.isUsableFix(location, nowMs));
    }

    @Test
    public void selectBest_prefersFreshUsableFix() {
        long nowMs = 100_000L;
        NavigationLocationFix staleGps =
                location(GPS_PROVIDER, nowMs - 30_000L, 5f);
        NavigationLocationFix freshNetwork =
                location(NETWORK_PROVIDER, nowMs - 1_000L, 20f);

        NavigationLocationFix selected =
                NavigationStartupLocationSelector.selectBestFix(staleGps, freshNetwork, nowMs);

        assertNotNull(selected);
        assertEquals(NETWORK_PROVIDER, selected.provider);
    }

    @Test
    public void selectBest_returnsNullWhenNoUsableFixExists() {
        long nowMs = 100_000L;
        NavigationLocationFix staleGps =
                location(GPS_PROVIDER, nowMs - 20_000L, 5f);
        NavigationLocationFix inaccurateNetwork =
                location(NETWORK_PROVIDER, nowMs - 1_000L, 120f);

        NavigationLocationFix selected =
                NavigationStartupLocationSelector.selectBestFix(staleGps, inaccurateNetwork, nowMs);

        assertNull(selected);
    }

    @Test
    public void isUsable_acceptsFreshAccurateFix() {
        long nowMs = 100_000L;
        NavigationLocationFix location =
                location(GPS_PROVIDER, nowMs - 5_000L, 12f);

        assertTrue(NavigationStartupLocationSelector.isUsableFix(location, nowMs));
    }

    @Test
    public void isUsableForRouteStart_rejectsFixAboveWarmupAccuracy() {
        long nowMs = 100_000L;
        NavigationLocationFix location =
                location(GPS_PROVIDER, nowMs - 2_000L, 30f);

        assertFalse(NavigationStartupLocationSelector.isUsableForRouteStartFix(location, nowMs));
    }

    @Test
    public void isUsableForRouteStart_acceptsFreshWarmupAccurateFix() {
        long nowMs = 100_000L;
        NavigationLocationFix location =
                location(GPS_PROVIDER, nowMs - 2_000L, 25f);

        assertTrue(NavigationStartupLocationSelector.isUsableForRouteStartFix(location, nowMs));
    }

    @Test
    public void isUsable_usesElapsedRealtimeWhenAvailable() {
        long nowElapsedMs = 100_000L;
        NavigationLocation location = new NavigationLocation(GPS_PROVIDER);
        location.setTime(1_000_000L, nowElapsedMs - 20_000L);
        location.setLatitude(48.2082);
        location.setLongitude(16.3738);
        location.setAccuracy(5f);

        assertFalse(NavigationStartupLocationSelector.isUsable(location, nowElapsedMs));
    }

    private static NavigationLocationFix location(
            String provider,
            long timeMs,
            float accuracyMeters
    ) {
        return NavigationLocationFix.qualityOnly(provider, timeMs, accuracyMeters);
    }
}
