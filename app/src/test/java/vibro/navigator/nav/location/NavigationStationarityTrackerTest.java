package vibro.navigator.nav.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationStationarityTrackerTest {
    private final NavigationLocationMotionModel model = new NavigationLocationMotionModel();

    @Test
    public void noisySpeedsAndPoorPositionsPreserveStopAcrossSixtySecondIntervals() {
        record(1_000L, 0.0, 0f, 5f);
        NavigationLocation last = null;
        for (int i = 1; i <= 4; i++) {
            last = record(1_000L + i * 60_000L, i % 2 == 0 ? -12.0 : 15.0, 0.6f, 60f);
            assertTrue(model.isLikelyStationary());
            assertEquals(0f, model.speedMps(last), 0f);
            assertEquals(0f, model.displaySpeedMps(last), 0f);
        }
    }

    @Test
    public void shortIntervalDriftWithinAccuracyDoesNotReleaseStop() {
        record(1_000L, 0.0, 0f, 5f);
        record(4_000L, 2.0, 0.5f, 5f);
        record(7_000L, -3.0, 0.7f, 5f);
        assertTrue(model.isLikelyStationary());
    }

    @Test
    public void slowMovementAccumulatesFromStopOriginUntilBeyondUncertainty() {
        record(1_000L, 0.0, 0f, 5f);
        for (int i = 1; i <= 5; i++) {
            record(1_000L + i * 3_000L, i * 1.8, 0.6f, 5f);
            assertTrue(model.isLikelyStationary());
        }
        NavigationLocation moving = record(19_000L, 10.8, 0.6f, 5f);
        assertFalse(model.isLikelyStationary());
        assertEquals(0.6f, model.speedMps(moving), 0f);
    }

    @Test
    public void poorPositionJumpDoesNotReleaseStopButGoodDisplacementDoes() {
        record(1_000L, 0.0, 0f, 5f);
        record(61_000L, 200.0, 0.5f, 100f);
        assertTrue(model.isLikelyStationary());
        record(121_000L, 200.0, 0.5f, 5f);
        assertFalse(model.isLikelyStationary());
    }

    @Test
    public void accurateNativeSpeedResumesSlowMovementWithoutWaitingForDisplacement() {
        record(1_000L, 0.0, 0f, 5f);
        NavigationLocation moving = location(61_000L, 0.2, 0.5f, 80f);
        moving.setSpeedAccuracyMetersPerSecond(0.05f);
        model.recordFilteredLocation(moving);
        assertFalse(model.isLikelyStationary());
        assertEquals(0.5f, model.speedMps(moving), 0f);
    }

    @Test
    public void uncertainOrInvalidSpeedAccuracyCannotReleaseStop() {
        record(1_000L, 0.0, 0f, 5f);
        float[] uncertainties = {0.3f, Float.NaN, Float.POSITIVE_INFINITY, -0.1f};
        for (int i = 0; i < uncertainties.length; i++) {
            NavigationLocation noise = location(61_000L + i * 60_000L, 1.0, 0.5f, 40f);
            noise.setSpeedAccuracyMetersPerSecond(uncertainties[i]);
            model.recordFilteredLocation(noise);
            assertTrue(model.isLikelyStationary());
        }
    }

    @Test
    public void invalidPositionAccuracyCannotConfirmMovement() {
        record(1_000L, 0.0, 0f, 5f);
        float[] uncertainties = {Float.NaN, Float.POSITIVE_INFINITY, -1f};
        for (int i = 0; i < uncertainties.length; i++) {
            record(61_000L + i * 60_000L, 30.0, 0.5f, uncertainties[i]);
            assertTrue(model.isLikelyStationary());
        }
    }

    @Test
    public void morePreciseFixCanEstablishReferenceWithoutLeakingItsInputChanges() {
        record(1_000L, 0.0, 0f, 80f);
        NavigationLocation precise = record(61_000L, 1.0, 0.5f, 3f);
        assertTrue(model.isLikelyStationary());
        precise.setLatitude(1.0);
        record(121_000L, 8.0, 0.5f, 3f);
        assertFalse(model.isLikelyStationary());
    }

    @Test
    public void duplicateOrOlderFixCannotResetStationaryTracking() {
        record(1_000L, 0.0, 0f, 5f);
        record(1_000L, 30.0, 10f, 5f);
        record(900L, 40.0, 10f, 5f);
        assertTrue(model.isLikelyStationary());
    }

    @Test
    public void resetClearsStopReferenceForNextSession() {
        record(1_000L, 0.0, 0f, 5f);
        model.reset();
        assertFalse(model.isLikelyStationary());
        record(500L, 0.0, 15f, 5f);
        assertFalse(model.isLikelyStationary());
    }

    @Test
    public void poorInitialFixWithSmallUnconfirmedVelocityAllowsStartupCompass() {
        record(1_000L, 0.0, 0.5f, 80f);
        assertTrue(model.isLikelyStationary());
    }

    private NavigationLocation record(long timeMs, double distanceMeters, float speed, float accuracy) {
        NavigationLocation location = location(timeMs, distanceMeters, speed, accuracy);
        model.recordFilteredLocation(location);
        return location;
    }

    private static NavigationLocation location(long timeMs, double distanceMeters, float speed, float accuracy) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(timeMs, timeMs);
        location.setLatitude(distanceMeters / 111_320.0);
        location.setLongitude(0.0);
        location.setSpeed(speed);
        location.setAccuracy(accuracy);
        return location;
    }
}
