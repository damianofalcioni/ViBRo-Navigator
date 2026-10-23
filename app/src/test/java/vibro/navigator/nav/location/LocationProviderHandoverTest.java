package vibro.navigator.nav.location;

import org.junit.Test;
import vibro.navigator.nav.session.NavigationSessionLocationState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocationProviderHandoverTest {
    private static final String FUSED = "fused";
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
