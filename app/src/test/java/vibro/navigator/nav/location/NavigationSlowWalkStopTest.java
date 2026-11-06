package vibro.navigator.nav.location;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationSlowWalkStopTest {
    @Test
    public void frequentSpeedlessWalkingCannotRenewStationarityFromAdjacentFixes() {
        assertWalkingDoesNotHoldStop(0.6, false);
        assertWalkingDoesNotHoldStop(0.2, false);
    }

    @Test
    public void driftingZeroSpeedCannotRenewStationarityFromAdjacentFixes() {
        assertWalkingDoesNotHoldStop(0.6, true);
    }

    private static void assertWalkingDoesNotHoldStop(double speed, boolean reportedStop) {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        for (int i = 0; i <= 30; i++) {
            model.recordFilteredLocation(fix(1_000 + i * 1_000L, i * speed, reportedStop));
            if (i >= 6) {
                assertFalse("Stop at second " + i + " speed=" + speed, model.isLikelyStationary());
            }
        }
    }

    @Test
    public void nativeStopStillConfirmsPromptlyAfterWalking() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        NavigationLocation moving = fix(1_000, 0, true);
        moving.setSpeed(1);
        model.recordFilteredLocation(moving);
        model.recordFilteredLocation(fix(2_000, 1, true));
        model.recordFilteredLocation(fix(3_000, 1, true));
        assertTrue(model.isLikelyStationary());
    }

    @Test
    public void speedlessStopEventuallyConfirmsAfterWalkingLeavesTheEvidenceWindow() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        for (int i = 0; i <= 20; i++) {
            model.recordFilteredLocation(fix(1_000 + i * 1_000L, Math.min(i, 8), false));
        }
        assertTrue(model.isLikelyStationary());
    }

    private static NavigationLocation fix(long time, double north, boolean reportedStop) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setLatitude(north / 111_320.0);
        location.setAccuracy(5);
        if (reportedStop) {
            location.setSpeed(0);
        }
        return location;
    }
}
