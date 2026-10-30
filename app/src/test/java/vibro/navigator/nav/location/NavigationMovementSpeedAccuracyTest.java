package vibro.navigator.nav.location;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationMovementSpeedAccuracyTest {
    @Test
    public void preciseSlowWalkingCanBeCorroboratedByIndependentPositions() {
        for (float speed : new float[] {0.25f, 0.3f, 0.35f}) {
            NavigationLocationMotionModel model = new NavigationLocationMotionModel();
            for (int i = 0; i <= 30; i++) {
                NavigationLocation location = slowFix(1_000 + i * 3_000L, i * speed * 3, speed);
                model.recordFilteredLocation(location);
                if (i >= 15) {
                    assertEquals("Corroborated speed=" + speed, NavigationMotionState.MOVING, model.motionState());
                    assertFalse(model.needsDepartureChecks());
                }
            }
        }
    }

    @Test
    public void preciseSlowSpeedAloneCannotEstablishMovement() {
        NavigationMovementEvidence evidence = new NavigationMovementEvidence();
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        for (int i = 0; i < 12; i++) {
            NavigationLocation location = slowFix(1_000 + i * 3_000L, 0, 0.3f);
            assertFalse(evidence.record(location));
            model.recordFilteredLocation(location);
            assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
            assertTrue(model.needsDepartureChecks());
        }
    }

    @Test
    public void preciseSlowDepartureCannotRenewAnEarlierStopBeforePositionCorroboration() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        model.recordFilteredLocation(slowFix(1_000, 0, 0));
        model.recordFilteredLocation(slowFix(2_000, 0, 0));
        assertEquals(NavigationMotionState.STATIONARY, model.motionState());
        for (int i = 0; i <= 32; i++) {
            model.recordFilteredLocation(slowFix(3_000 + i * 1_000L, i * 0.25, 0.25f));
            assertNotEquals("Slow departure at second " + i, NavigationMotionState.STATIONARY, model.motionState());
            if (i == 0) {
                assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
                assertTrue(model.needsDepartureChecks());
            }
        }
        assertEquals(NavigationMotionState.MOVING, model.motionState());
    }

    @Test
    public void slowSpeedWhoseUncertaintyIncludesZeroCanStillSupportIndependentStopPositions() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        for (long time : new long[] {1_000, 4_000}) {
            NavigationLocation uncertain = slowFix(time, 0, 0.25f);
            uncertain.setSpeedAccuracyMetersPerSecond(0.15f);
            model.recordFilteredLocation(uncertain);
        }
        assertEquals(NavigationMotionState.STATIONARY, model.motionState());
    }

    @Test
    public void zeroOverlappingSlowSpeedCannotBeBypassedByPositionProgress() {
        NavigationMovementEvidence evidence = new NavigationMovementEvidence();
        for (int i = 0; i < 15; i++) {
            NavigationLocation location = slowFix(1_000 + i * 3_000L, i * 0.9, 0.3f);
            location.setSpeedAccuracyMetersPerSecond(0.15f);
            assertFalse(evidence.record(location));
        }
    }

    private static NavigationLocation slowFix(long time, double north, float speed) {
        NavigationLocation location = fix(time, north, 0.01f);
        location.setSpeed(speed);
        return location;
    }

    @Test
    public void trustworthySpeedStillNeedsIndependentDirectionalProgress() {
        NavigationMovementEvidence evidence = new NavigationMovementEvidence();
        assertFalse(evidence.record(fix(1_000, 0, 0.1f)));
        assertFalse(evidence.record(fix(4_000, 3, 0.1f)));
        assertFalse(evidence.record(fix(4_001, 3, 0.1f)));
        assertFalse(evidence.record(fix(7_000, 6, 0.1f)));
        assertTrue(evidence.record(fix(10_000, 9, 0.1f)));
        assertFalse(evidence.record(fix(13_000, 9, 0.1f)));
    }

    @Test
    public void invalidOrZeroOverlappingSpeedAccuracyCannotCorroborateTravel() {
        for (float accuracy : new float[] {Float.NaN, Float.POSITIVE_INFINITY, -1, 0.5f}) {
            NavigationMovementEvidence evidence = new NavigationMovementEvidence();
            for (int i = 0; i < 10; i++) {
                assertFalse(evidence.record(fix(1_000 + i * 3_000L, i * 3, accuracy)));
            }
        }
    }

    @Test
    public void providerChangesAndUntrustworthyReadingsClearCorroboration() {
        NavigationMovementEvidence evidence = new NavigationMovementEvidence();
        for (int i = 0; i <= 3; i++) {
            evidence.record(fix(1_000 + i * 3_000L, i * 3, 0.1f));
        }
        NavigationLocation handover = fix(13_000, 100, 0.1f);
        handover.setProvider("network");
        assertFalse(evidence.record(handover));
        assertFalse(evidence.record(fix(16_000, 12, 0.1f)));
        assertFalse(evidence.record(fix(19_000, 15, 0.1f)));
        assertFalse(evidence.record(fix(22_000, 18, 0.5f)));
        assertFalse(evidence.record(fix(25_000, 21, 0.1f)));
        assertFalse(evidence.record(fix(28_000, 24, 0.1f)));
        assertFalse(evidence.record(fix(31_000, 27, 0.1f)));
        assertTrue(evidence.record(fix(34_000, 30, 0.1f)));
    }

    private static NavigationLocation fix(long timeMs, double northMeters, float speedAccuracy) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(timeMs, timeMs);
        location.setLatitude(northMeters / 111_320.0);
        location.setAccuracy(13);
        location.setSpeed(1);
        location.setSpeedAccuracyMetersPerSecond(speedAccuracy);
        return location;
    }
}
