package vibro.navigator.nav.location;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationMovingEvidenceRenewalTest {
    private final NavigationLocationMotionModel model = new NavigationLocationMotionModel();

    @Test
    public void repeatedPositionsCannotRenewEarlierWalkingEvenWithNonzeroUnverifiedSpeed() {
        walk();
        for (int i = 1; i <= 15; i++) {
            NavigationLocation repeated = fix(31_000 + i * 3_000L, 30);
            model.recordFilteredLocation(repeated);
            assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
            assertTrue(model.needsDepartureChecks());
            assertEquals(0f, model.displaySpeedMps(repeated), 0f);
        }
    }

    @Test
    public void filterDisplacementCannotRenewMovementAfterRawPositionsStop() {
        walk();
        NavigationLocation raw = fix(34_000, 30);
        NavigationLocation filtered = new NavigationLocation(raw);
        filtered.setLatitude(34 / 111_320.0);
        filtered.setFilteredAccuracy(20);
        model.recordLocation(raw, filtered);
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
    }

    @Test
    public void renewedWalkingNeedsIndependentEvidenceAfterContradictoryPositions() {
        walk();
        model.recordFilteredLocation(fix(34_000, 30));
        model.recordFilteredLocation(fix(37_000, 33));
        model.recordFilteredLocation(fix(40_000, 36));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        model.recordFilteredLocation(fix(43_000, 39));
        assertEquals(NavigationMotionState.MOVING, model.motionState());
    }

    @Test
    public void slowAndSparseWalkingRetainEvidenceButARepeatedSparsePositionDoesNot() {
        for (long interval : new long[] {3_000, 12_000, 15_000, 20_000, 30_000, 60_000}) {
            model.reset();
            for (int i = 0; i <= 10; i++) {
                NavigationLocation walking = fix(1_000 + i * interval, i * interval * 0.3 / 1_000);
                walking.setSpeed(0.3f);
                model.recordFilteredLocation(walking);
            }
            assertEquals("Walking interval " + interval, NavigationMotionState.MOVING, model.motionState());
            NavigationLocation repeated = fix(1_000 + 11 * interval, 10 * interval * 0.3 / 1_000);
            repeated.setSpeed(0.3f);
            model.recordFilteredLocation(repeated);
            assertNotEquals(NavigationMotionState.MOVING, model.motionState());
            assertTrue(model.needsDepartureChecks());
        }
    }

    @Test
    public void independentTrustedSpeedSupportsSparseCadenceAndSlowWalking() {
        NavigationLocation first = trusted(1_000);
        first.setSpeed(0.37f);
        model.recordFilteredLocation(first);
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        NavigationLocation second = new NavigationLocation(first);
        second.setTime(61_000, 61_000);
        model.recordFilteredLocation(second);
        assertEquals(NavigationMotionState.MOVING, model.motionState());
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState(71_001));
    }

    @Test
    public void onePreciseSpeedSpikeCannotReplaceAConfirmedStopWithMovement() {
        NavigationLocation stopped = fix(1_000, 0);
        stopped.setSpeed(0);
        model.recordFilteredLocation(stopped);
        stopped.setTime(4_000, 4_000);
        model.recordFilteredLocation(stopped);
        assertEquals(NavigationMotionState.STATIONARY, model.motionState());
        model.recordFilteredLocation(trusted(7_000));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        assertTrue(model.needsDepartureChecks());
        stopped.setTime(10_000, 10_000);
        model.recordFilteredLocation(stopped);
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        stopped.setTime(13_000, 13_000);
        model.recordFilteredLocation(stopped);
        assertEquals(NavigationMotionState.STATIONARY, model.motionState());
    }

    @Test
    public void trustedSpeedRequiresIndependentObservationsAndSupportsOneSecondCallbacks() {
        for (long time : new long[] {1_000, 1_001, 2_000, 2_999}) {
            model.recordFilteredLocation(trusted(time));
            assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        }
        for (long time : new long[] {3_000, 4_000, 5_000, 6_000}) {
            model.recordFilteredLocation(trusted(time));
            assertEquals(NavigationMotionState.MOVING, model.motionState());
        }
    }

    @Test
    public void providerChangesLongGapsAndResetDiscardNativeSpeedConfirmation() {
        model.recordFilteredLocation(trusted(1_000));
        model.recordFilteredLocation(trusted(4_000));
        assertEquals(NavigationMotionState.MOVING, model.motionState());
        NavigationLocation network = trusted(7_000);
        network.setProvider("network");
        model.recordFilteredLocation(network);
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        network.setTime(10_000, 10_000);
        model.recordFilteredLocation(network);
        assertEquals(NavigationMotionState.MOVING, model.motionState());
        network.setTime(80_000, 80_000);
        model.recordFilteredLocation(network);
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        model.reset();
        model.recordFilteredLocation(trusted(1_000));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
    }

    @Test
    public void untrustedSpeedBetweenTrustedReadingsRestartsConfirmation() {
        model.recordFilteredLocation(trusted(1_000));
        model.recordFilteredLocation(fix(4_000, 0));
        model.recordFilteredLocation(trusted(7_000));
        assertEquals(NavigationMotionState.UNKNOWN, model.motionState());
        model.recordFilteredLocation(trusted(10_000));
        assertEquals(NavigationMotionState.MOVING, model.motionState());
    }

    private void walk() {
        for (int i = 0; i <= 10; i++) {
            model.recordFilteredLocation(fix(1_000 + i * 3_000L, i * 3));
        }
        assertEquals(NavigationMotionState.MOVING, model.motionState());
    }

    private static NavigationLocation trusted(long time) {
        NavigationLocation location = fix(time, 0);
        location.setSpeed(2);
        location.setSpeedAccuracyMetersPerSecond(0.1f);
        return location;
    }

    private static NavigationLocation fix(long time, double north) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setLatitude(north / 111_320.0);
        location.setAccuracy(13);
        location.setSpeed(1);
        return location;
    }
}
