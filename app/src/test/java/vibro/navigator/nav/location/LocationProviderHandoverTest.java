package vibro.navigator.nav.location;

import org.junit.Test;
import vibro.navigator.nav.session.NavigationSessionLocationState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocationProviderHandoverTest {
    private static final String FUSED = "fused";
    private static final String GPS = "gps";

    @Test
    public void startupSeedCanUseLegacyCacheWithoutRetainingAnInactiveCandidate() {
        for (String provider : new String[] {GPS, "network"}) {
            NavigationSessionLocationState state = new NavigationSessionLocationState();
            state.setActiveProviders(FUSED);
            assertFalse(state.onStartupLocationSeed(fix(provider, 1_000), 1_000, 3_000).isDropped());
            NavigationLocation fused = fix(FUSED, 5_000);
            fused.setAccuracy(20);
            assertFalse(state.onRawLocationChanged(fused, 5_000, false, 3_000).isDropped());
            assertEquals(FUSED, state.getLastFilteredLocation().getProvider());
            assertTrue(state.onRawLocationChanged(fix(provider, 6_000), 6_000).isDropped());
            assertTrue(state.onStartupLocationSeed(fix(provider, 7_000), 7_000, 3_000).isDropped());
        }
    }

    @Test
    public void invalidOrOldStartupSeedDoesNotBypassQualityChecksOrBlockLiveFixes() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        state.setActiveProviders(FUSED);
        assertTrue(state.onStartupLocationSeed(fix(GPS, 1_000), 20_000, 3_000).isDropped());
        NavigationLocation poor = fix(GPS, 20_000);
        poor.setAccuracy(100);
        assertTrue(state.onStartupLocationSeed(poor, 20_000, 3_000).isDropped());
        assertFalse(state.onRawLocationChanged(fix(FUSED, 21_000), 21_000).isDropped());
    }
    @Test
    public void retiredGpsCannotBlockNewFusedFixAndCachedMeasurementRemainsDuplicate() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        state.setActiveProviders("gps+network");
        NavigationLocation gps = fix("gps", 1_000);
        state.onRawLocationChanged(gps, 1_000, false, 3_000);
        state.setActiveProviders(FUSED);
        assertTrue(state.onRawLocationChanged(fix(FUSED, 1_000), 1_100, false, 3_000).isDropped());
        assertFalse(state.onRawLocationChanged(fix(FUSED, 5_000), 5_000, false, 3_000).isDropped());
        assertTrue(state.onRawLocationChanged(fix("gps", 6_000), 6_000, false, 3_000).isDropped());
        assertEquals(FUSED, state.getLastFilteredLocation().getProvider());
    }

    @Test
    public void fallbackRetiresFusedAndRetainsNetworkAlongsideGps() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        state.onRawLocationChanged(fix(FUSED, 1_000), 1_000, false, 3_000);
        state.setActiveProviders("gps+network");
        assertFalse(state.onRawLocationChanged(fix("network", 5_000), 5_000, false, 3_000).isDropped());
        assertTrue(state.onRawLocationChanged(fix(FUSED, 6_000), 6_000, false, 3_000).isDropped());
        assertEquals("network", state.getLastFilteredLocation().getProvider());
    }

    private static NavigationLocation fix(String provider, long time) {
        NavigationLocation location = new NavigationLocation(provider);
        location.setTime(time, time);
        location.setAccuracy(9);
        return location;
    }
}
