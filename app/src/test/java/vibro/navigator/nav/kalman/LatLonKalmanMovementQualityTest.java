package vibro.navigator.nav.kalman;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class LatLonKalmanMovementQualityTest {
    @Test
    public void preciseSlowWalkingRetainsTheCorroboratedLagBound() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        for (int i = 0; i <= 40; i++) {
            NavigationLocation raw = walking(1_000 + i * 3_000L, i * 0.9);
            raw.setSpeed(0.3f);
            raw.setSpeedAccuracyMetersPerSecond(0.01f);
            NavigationLocation filtered = filter.update(raw);
            assertNotNull(filtered);
            if (i >= 20) {
                assertTrue("Lag at sample " + i, raw.distanceTo(filtered) <= 3.1);
            }
            assertEquals(30 + raw.distanceTo(filtered), filtered.getAccuracy(), 0.001);
            assertFalse(filtered.hasBearing());
        }
    }

    @Test
    public void trustworthySpeedWithoutCourseRetainsTheCorroboratedWalkingLagBound() {
        for (long intervalMs : new long[] {1_000, 3_000, 12_000}) {
            assertWalkingLag(intervalMs, false);
            assertWalkingLag(intervalMs, true);
        }
    }

    private static void assertWalkingLag(long intervalMs, boolean speedAccuracy) {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        for (int i = 0; i <= 90_000 / intervalMs; i++) {
            NavigationLocation raw = walking(1_000 + i * intervalMs, i * intervalMs / 1_000.0);
            if (speedAccuracy) {
                raw.setSpeedAccuracyMetersPerSecond(0.1f);
            }
            NavigationLocation filtered = filter.update(raw);
            assertNotNull(filtered);
            if (i * intervalMs >= 30_000) {
                assertTrue("Lag with speed accuracy=" + speedAccuracy + " interval=" + intervalMs,
                        raw.distanceTo(filtered) <= 3.1);
            }
            assertEquals(30 + raw.distanceTo(filtered), filtered.getAccuracy(), 0.001);
            assertEquals(raw.hasSpeedAccuracy(), filtered.hasSpeedAccuracy());
            assertFalse(filtered.hasBearing());
        }
    }

    @Test
    public void trustworthySpeedDoesNotAuthorizePredictionUsingUncertainCourse() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        for (int i = 0; i < 30; i++) {
            NavigationLocation raw = walking(1_000 + i * 3_000L, i * 3);
            raw.setSpeedAccuracyMetersPerSecond(0.1f);
            raw.setBearing(180);
            raw.setBearingAccuracyDegrees(90);
            NavigationLocation filtered = filter.update(raw);
            assertNotNull(filtered);
            assertTrue(filtered.getLatitude() <= raw.getLatitude());
            assertEquals(0, filtered.getLongitude(), 0);
            if (i >= 10) {
                assertTrue(raw.distanceTo(filtered) <= 3.1);
            }
        }
    }

    private static NavigationLocation walking(long timeMs, double northMeters) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(1_700_000_000_000L + timeMs, timeMs);
        location.setLatitude(northMeters / 111_320.0);
        location.setAccuracy(30);
        location.setSpeed(1);
        return location;
    }
}
