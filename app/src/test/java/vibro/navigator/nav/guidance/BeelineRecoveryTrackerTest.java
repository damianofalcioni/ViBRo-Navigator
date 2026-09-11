package vibro.navigator.nav.guidance;

import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class BeelineRecoveryTrackerTest {
    private static final LatLon TARGET = new LatLon(0, 0);

    @Test
    public void walkingSizedGrowthNeedsTenSecondsBeyondUncertainty() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        assertFalse(observe(tracker, 100, 5, 1_000));
        for (long now = 4_000; now < 16_000; now += 3_000) {
            assertFalse(observe(tracker, 135, 5, now));
        }
        assertTrue(observe(tracker, 135, 5, 16_000));
        // Evaluating eligibility alone must not consume the attempt/cooldown.
        assertTrue(observe(tracker, 135, 5, 19_000));
    }

    @Test
    public void unsuccessfulAttemptRequiresTimeAndFurtherMovementBeforeRetry() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        observe(tracker, 100, 5, 1_000);
        for (long now = 4_000; now <= 16_000; now += 3_000) {
            observe(tracker, 135, 5, now);
        }
        tracker.recordAttempt(fix(135, 5), 16_000);
        for (long now = 19_000; now < 46_000; now += 3_000) {
            assertFalse(observe(tracker, 170, 5, now));
        }
        assertFalse(observe(tracker, 140, 5, 46_000));
        assertTrue(observe(tracker, 170, 5, 49_000));
    }

    @Test
    public void uncertaintyAndShortDetoursDoNotConfirmGrowth() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        observe(tracker, 100, 20, 1_000);
        for (long now = 4_000; now < 25_000; now += 3_000) {
            assertFalse(observe(tracker, 150, 20, now));
        }
        observe(tracker, 200, 20, 25_000);
        assertFalse(observe(tracker, 110, 20, 28_000));
        assertFalse(observe(tracker, 200, 20, 31_000));
    }

    @Test
    public void stationarityPauseAndLongGapClearEvidence() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        observe(tracker, 100, 5, 1_000);
        observe(tracker, 135, 5, 4_000);
        assertFalse(tracker.shouldRequest(TARGET, fix(135, 5), true, 7_000));
        assertFalse(observe(tracker, 135, 5, 10_000));
        assertFalse(observe(tracker, 200, 5, 25_000));
        tracker.resetEvidence();
        assertFalse(observe(tracker, 250, 5, 28_000));
        assertFalse(observe(tracker, 400, 80, 31_000));
    }

    @Test
    public void stationaryFixDoesNotRequestRecovery() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        assertFalse(tracker.shouldRequest(TARGET, fix(100, 5), true, 1_000));
        assertFalse(tracker.shouldRequest(TARGET, fix(150, 5), true, 40_000));
    }

    private static boolean observe(BeelineRecoveryTracker tracker, double distance, float accuracy, long now) {
        return tracker.shouldRequest(TARGET, fix(distance, accuracy), false, now);
    }

    private static NavigationLocation fix(double distance, float accuracy) {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setLatitude(0);
        fix.setLongitude(distance / 111_195.0);
        fix.setAccuracy(accuracy);
        return fix;
    }
}
