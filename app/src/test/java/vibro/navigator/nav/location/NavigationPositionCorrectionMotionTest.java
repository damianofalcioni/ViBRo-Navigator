package vibro.navigator.nav.location;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationPositionCorrectionMotionTest {
    private final NavigationLocationMotionModel model = new NavigationLocationMotionModel();

    @Test
    public void zeroSpeedCorrectionsCannotBecomeTravelAfterAConfirmedStop() {
        model.recordFilteredLocation(fix(1_000, 0, 0f));
        model.recordFilteredLocation(fix(4_000, 0, 0f));
        assertEquals(NavigationMotionState.STATIONARY, model.motionState());
        for (int i = 0; i < 12; i++) {
            model.recordFilteredLocation(fix(7_000 + i * 3_000L, i % 2 == 0 ? 12 : 0, 0f));
            assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
            assertTrue(model.needsDepartureChecks());
        }
    }

    @Test
    public void oneSpeedlessCorrectionCannotEstablishMovement() {
        model.recordFilteredLocation(fix(1_000, 0, null));
        model.recordFilteredLocation(fix(4_000, 0, null));
        model.recordFilteredLocation(fix(7_000, 12, null));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        assertTrue(model.needsDepartureChecks());
    }

    @Test
    public void speedlessWalkingRequiresIndependentProgressBeyondPositionUncertainty() {
        for (long time : new long[] {1_000, 4_000, 4_001, 4_100}) {
            model.recordFilteredLocation(fix(time, time == 1_000 ? 0 : 6, null));
            assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        }
        model.recordFilteredLocation(fix(7_000, 12, null));
        assertEquals(NavigationMotionState.MOVING, model.motionState());
        assertFalse(model.needsDepartureChecks());
    }

    @Test
    public void poorSpeedlessPositionsAndRepeatedCorrectionsRemainUnknown() {
        for (int i = 0; i < 10; i++) {
            NavigationLocation poor = fix(1_000 + i * 3_000L, i * 10, null);
            poor.setAccuracy(40);
            model.recordFilteredLocation(poor);
            assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        }
        model.reset();
        for (int i = 0; i < 10; i++) {
            model.recordFilteredLocation(fix(1_000 + i * 3_000L, i % 2 == 0 ? 0 : 12, null));
            assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        }
    }

    @Test
    public void speedlessHandoverAndSilenceRequireNewIndependentEvidence() {
        model.recordFilteredLocation(fix(1_000, 0, null));
        model.recordFilteredLocation(fix(4_000, 6, null));
        model.recordFilteredLocation(fix(7_000, 12, null));
        assertEquals(NavigationMotionState.MOVING, model.motionState());
        NavigationLocation handover = fix(10_000, 100, null);
        handover.setProvider("network");
        model.recordFilteredLocation(handover);
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        model.recordFilteredLocation(fix(80_000, 200, null));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
    }

    private static NavigationLocation fix(long time, double east, Float speed) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setLongitude(east / 111_320.0);
        location.setAccuracy(5);
        if (speed != null) {
            location.setSpeed(speed);
        }
        return location;
    }
}
