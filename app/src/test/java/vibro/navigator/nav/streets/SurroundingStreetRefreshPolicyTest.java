package vibro.navigator.nav.streets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocation;

public class SurroundingStreetRefreshPolicyTest {
    private final SurroundingStreetRefreshPolicy policy = new SurroundingStreetRefreshPolicy();

    @Test
    public void shouldRefresh_firstFixAndNotWhileInFlight() {
        NavigationLocation current = location(48.2082d, 16.3738d);

        assertTrue(policy.shouldRefresh(current, null, 0L, 1_000L, false));
        assertFalse(policy.shouldRefresh(current, null, 0L, 1_000L, true));
    }

    @Test
    public void shouldRefresh_waitsForCoarseDistanceAndIntervalUnlessMovementIsLarge() {
        NavigationLocation last = location(48.2082d, 16.3738d);
        NavigationLocation near = location(48.2120d, 16.3738d);
        NavigationLocation regular = location(48.2180d, 16.3738d);
        NavigationLocation far = location(48.2280d, 16.3738d);

        assertFalse(policy.shouldRefresh(near, last, 1_000L, 90_000L, false));
        assertFalse(policy.shouldRefresh(regular, last, 1_000L, 20_000L, false));
        assertTrue(policy.shouldRefresh(regular, last, 1_000L, 90_000L, false));
        assertTrue(policy.shouldRefresh(far, last, 1_000L, 20_000L, false));
    }

    private static NavigationLocation location(double lat, double lon) {
        NavigationLocation location = new NavigationLocation("test");
        location.setLatitude(lat);
        location.setLongitude(lon);
        return location;
    }
}
