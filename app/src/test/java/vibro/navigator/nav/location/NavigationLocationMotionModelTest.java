package vibro.navigator.nav.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationLocationMotionModelTest {
    private static final String GPS_PROVIDER = "gps";

    @Test
    public void speedMps_usesElapsedRealtimeWhenWallClockJumps() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        NavigationLocation first = location(10_000L, 1_000L, 48.2082000, 16.3738000);
        NavigationLocation second = location(5_000L, 2_000L, 48.2082900, 16.3738000);

        model.recordFilteredLocation(first);
        model.recordFilteredLocation(second);

        float speedMps = model.speedMps(second);

        assertTrue(speedMps > 9f);
        assertTrue(speedMps < 11f);
    }

    @Test
    public void movementBearingDegrees_usesElapsedRealtimeWhenWallClockJumps() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        NavigationLocation first = location(10_000L, 1_000L, 48.2082000, 16.3738000);
        NavigationLocation second = location(5_000L, 4_000L, 48.2082600, 16.3738000);

        model.recordFilteredLocation(first);
        model.recordFilteredLocation(second);

        Double bearing = model.movementBearingDegrees(second);

        assertNotNull(bearing);
        assertEquals(0.0, bearing, 1.0);
    }

    private static NavigationLocation location(
            long wallTimeMs,
            long elapsedRealtimeMs,
            double lat,
            double lon
    ) {
        NavigationLocation location = new NavigationLocation(GPS_PROVIDER);
        location.setTime(wallTimeMs, elapsedRealtimeMs);
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAccuracy(5f);
        return location;
    }
}
