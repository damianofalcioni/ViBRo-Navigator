package vibro.navigator.nav.location;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationMotionConfidenceTest {
    private final NavigationLocationMotionModel model = new NavigationLocationMotionModel();

    @Test
    public void aClusterAfterOneDisplacementCannotSupplyTheThirdMovementObservation() {
        model.recordFilteredLocation(movingFix(1_000, 0));
        model.recordFilteredLocation(movingFix(7_000, 8));
        for (long time : new long[] {7_001, 7_100, 8_999}) {
            model.recordFilteredLocation(movingFix(time, 8));
            assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
            assertTrue(model.needsDepartureChecks());
        }
        model.recordFilteredLocation(movingFix(9_000, 10));
        assertEquals(NavigationMotionState.MOVING, model.motionState());
    }

    @Test
    public void oneSecondWalkingCallbacksRetainMovementAfterIndependentConfirmation() {
        for (int i = 0; i <= 12; i++) {
            model.recordFilteredLocation(movingFix(1_000 + i * 1_000L, i * 1.5));
            if (i >= 6) {
                assertEquals(NavigationMotionState.MOVING, model.motionState());
            }
        }
    }

    private static NavigationLocation movingFix(long time, double north) {
        NavigationLocation location = fix(time, 1);
        location.setLatitude(north / 111_320.0);
        location.setAccuracy(13);
        return location;
    }

    @Test
    public void clusteredStoppedObservationsWaitForIndependentEvidence() {
        for (long time : new long[] {1_000, 1_001, 1_100, 1_999}) {
            model.recordFilteredLocation(fix(time, 0));
            assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        }
        model.recordFilteredLocation(fix(2_000, 0));
        assertEquals(NavigationMotionState.STATIONARY, model.motionState());
    }

    @Test
    public void conflictingNativeSpeedCannotBeReinterpretedAsAConfirmedStop() {
        model.recordFilteredLocation(fix(1_000, 2));
        model.recordFilteredLocation(fix(4_000, 2));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        assertTrue(model.needsDepartureChecks());
        assertEquals(0f, model.speedMps(model.getLastFilteredLocation()), 0f);
    }

    @Test
    public void longGapsCannotCompleteAStopWindow() {
        model.recordFilteredLocation(fix(1_000, 0));
        model.recordFilteredLocation(fix(121_000, 0));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        model.recordFilteredLocation(fix(124_000, 0));
        assertEquals(NavigationMotionState.STATIONARY, model.motionState());
    }

    @Test
    public void stopConfidenceExpiresOnReadAndRequiresFreshEvidenceAfterSilence() {
        model.recordFilteredLocation(fix(1_000, 0));
        model.recordFilteredLocation(fix(4_000, 0));
        assertEquals(NavigationMotionState.STATIONARY, model.motionState(14_000));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState(14_001));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState(3_999));
        model.recordFilteredLocation(fix(20_000, 0));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState(20_000));
        model.recordFilteredLocation(fix(23_000, 0));
        assertEquals(NavigationMotionState.STATIONARY, model.motionState(23_000));
    }

    @Test
    public void speedlessStopStillNeedsAccurateIndependentPositions() {
        NavigationLocation first = fix(1_000, Float.NaN);
        NavigationLocation second = fix(4_000, Float.NaN);
        model.recordFilteredLocation(first);
        model.recordFilteredLocation(second);
        assertEquals(NavigationMotionState.STATIONARY, model.motionState());
    }

    private static NavigationLocation fix(long time, float speed) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setAccuracy(5);
        if (Float.isFinite(speed)) {
            location.setSpeed(speed);
        }
        return location;
    }
}
