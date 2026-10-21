package vibro.navigator.nav.kalman;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DelayedNetworkCorrectionTest {
    private static final String GPS = "gps";
    private static final String NETWORK = "network";
    private static final long NOW = 100_000L;

    @Test
    public void accurateDelayedNetworkRefinesNextGpsWithoutRewindingOrInventingMotion() {
        LatLonKalmanFilter filter = initialized();
        NavigationLocation network = fix(NETWORK, NOW - 2_027, 20, 20, 0);
        assertTrue(filter.correctDelayedNetworkPosition(network, NOW + 600));
        assertFalse(filter.correctDelayedNetworkPosition(network, NOW + 700));
        assertNull(filter.update(network));
        NavigationLocation next = filter.update(fix(GPS, NOW + 3_000, 0, 62, 0));
        assertNotNull(next);
        assertTrue(next.getLongitude() > 0);
        assertTrue(next.getLongitude() < network.getLongitude());
        assertEquals(NOW + 3_000, next.getElapsedRealtimeOrTimeMs());
        assertEquals("gps", next.getProvider());
        assertFalse(next.hasBearing());
        assertEquals(0f, next.getSpeed(), 0f);
    }

    @Test
    public void staleFastMovingInvalidAndInconsistentCorrectionsAreExcluded() {
        NavigationLocation[] excluded = {
                fix(NETWORK, NOW - 3_001, 20, 10, 0),
                fix(NETWORK, NOW - 2_000, 20, 55, 0),
                fix(NETWORK, NOW - 2_000, 1_000, 10, 0),
                fix(NETWORK, NOW + 1, 20, 10, 0),
                fix(NETWORK, NOW - 2_000, 20, Float.NaN, 0),
                fix(GPS, NOW - 2_000, 20, 10, 0)
        };
        for (NavigationLocation location : excluded) {
            assertFalse(initialized().correctDelayedNetworkPosition(location, NOW));
        }
        assertFalse(initialized().correctDelayedNetworkPosition(fix(NETWORK, NOW - 2_000, 20, 20, 0), NOW + 4_000));
        LatLonKalmanFilter moving = new LatLonKalmanFilter();
        moving.update(fix(GPS, NOW, 0, 62, 30));
        assertFalse(moving.correctDelayedNetworkPosition(fix(NETWORK, NOW - 2_000, 20, 20, 0), NOW));
    }

    @Test
    public void resetAndUninitializedFiltersDoNotReuseOldPositionOrCorrectionHistory() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        NavigationLocation network = fix(NETWORK, NOW - 2_000, 20, 20, 0);
        assertFalse(filter.correctDelayedNetworkPosition(network, NOW));
        filter.update(fix(GPS, NOW, 0, 62, 0));
        assertTrue(filter.correctDelayedNetworkPosition(network, NOW));
        filter.reset();
        assertFalse(filter.correctDelayedNetworkPosition(network, NOW));
        filter.update(fix(GPS, NOW, 0, 62, 0));
        assertTrue(filter.correctDelayedNetworkPosition(network, NOW));
    }

    private static LatLonKalmanFilter initialized() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(GPS, NOW, 0, 62, 0));
        return filter;
    }

    private static NavigationLocation fix(String provider, long time, double east, float accuracy, float speed) {
        NavigationLocation location = new NavigationLocation(provider);
        location.setTime(time, time);
        location.setLatitude(0);
        location.setLongitude(east / 111320);
        location.setAccuracy(accuracy);
        location.setSpeed(speed);
        return location;
    }
}
