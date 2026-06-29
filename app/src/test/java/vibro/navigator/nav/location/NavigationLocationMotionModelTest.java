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

    @Test
    public void speedMps_returnsZeroWhenFastReportedSpeedContradictsStationaryJitter() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        NavigationLocation first = location(1_000L, 1_000L, 48.2082000, 16.3738000, 21f);
        NavigationLocation second = location(2_000L, 2_000L, 48.2082003, 16.3738002, 19f);
        NavigationLocation third = location(3_000L, 3_000L, 48.2082001, 16.3738001, 22f);

        model.recordFilteredLocation(first);
        model.recordFilteredLocation(second);
        model.recordFilteredLocation(third);

        assertTrue(model.isLikelyStationary());
        assertEquals(0.0f, model.speedMps(third), 0.0f);
    }

    @Test
    public void displaySpeedMps_returnsZeroForLowReportedSpeedWithoutAccuracyAwareMovementEvidence() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        NavigationLocation first = location(1_000L, 1_000L, 48.2082000, 16.3738000);
        NavigationLocation second = location(4_000L, 4_000L, 48.2082270, 16.3738000, 1.2f);
        first.setAccuracy(8f);
        second.setAccuracy(8f);

        model.recordFilteredLocation(first);
        model.recordFilteredLocation(second);

        assertEquals(0.0f, model.displaySpeedMps(second), 0.0f);
    }

    @Test
    public void displaySpeedMps_usesLowReportedSpeedWithAccuracyAwareMovementEvidence() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        NavigationLocation first = location(1_000L, 1_000L, 48.2082000, 16.3738000);
        NavigationLocation second = location(4_000L, 4_000L, 48.2082540, 16.3738000, 1.2f);
        first.setAccuracy(5f);
        second.setAccuracy(5f);

        model.recordFilteredLocation(first);
        model.recordFilteredLocation(second);

        assertEquals(1.2f, model.displaySpeedMps(second), 0.0f);
    }

    @Test
    public void displaySpeedMps_returnsZeroForFirstReportedSpeedSample() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        NavigationLocation location = location(1_000L, 1_000L, 48.2082000, 16.3738000, 20f);

        model.recordFilteredLocation(location);

        assertEquals(0.0f, model.displaySpeedMps(location), 0.0f);
    }

    @Test
    public void displaySpeedMps_returnsZeroForFallbackSpeedFromInaccurateProviderJump() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        NavigationLocation first = location(1_000L, 1_000L, 48.2082000, 16.3738000);
        NavigationLocation second = location(6_000L, 6_000L, 48.2099000, 16.3738000);
        first.setAccuracy(60f);
        second.setAccuracy(55f);

        model.recordFilteredLocation(first);
        model.recordFilteredLocation(second);

        assertEquals(0.0f, model.displaySpeedMps(second), 0.0f);
    }

    @Test
    public void displaySpeedMps_returnsZeroForFallbackSpeedWithoutEnoughElapsedTime() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        NavigationLocation first = location(1_000L, 1_000L, 48.2082000, 16.3738000);
        NavigationLocation second = location(1_000L, 1_000L, 48.2082400, 16.3738000);

        model.recordFilteredLocation(first);
        model.recordFilteredLocation(second);

        assertEquals(0.0f, model.displaySpeedMps(second), 0.0f);
    }

    @Test
    public void displaySpeedMps_usesFallbackSpeedForAccurateConfirmedMovement() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        NavigationLocation first = location(1_000L, 1_000L, 48.2082000, 16.3738000);
        NavigationLocation second = location(4_000L, 4_000L, 48.2083080, 16.3738000);

        model.recordFilteredLocation(first);
        model.recordFilteredLocation(second);

        assertTrue(model.displaySpeedMps(second) > 3.5f);
        assertTrue(model.displaySpeedMps(second) < 4.5f);
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

    private static NavigationLocation location(
            long wallTimeMs,
            long elapsedRealtimeMs,
            double lat,
            double lon,
            float speedMps
    ) {
        NavigationLocation location = location(wallTimeMs, elapsedRealtimeMs, lat, lon);
        location.setSpeed(speedMps);
        return location;
    }
}
