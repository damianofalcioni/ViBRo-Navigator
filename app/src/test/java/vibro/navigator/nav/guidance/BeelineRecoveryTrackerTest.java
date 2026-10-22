package vibro.navigator.nav.guidance;

import org.junit.Test;
import java.util.function.Consumer;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class BeelineRecoveryTrackerTest {
    private static final LatLon TARGET = new LatLon(0, 0);

    @Test
    public void walkingSizedGrowthNeedsTenSecondsBeyondUncertainty() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        assertFalse(observe(tracker, 135, 5, 1_000));
        assertFalse(observe(tracker, 100, 5, 2_000));
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
        assertFalse(observe(tracker, 170, 5, 19_000));
        assertFalse(observe(tracker, 140, 5, 22_000));
        assertTrue(observe(tracker, 170, 5, 25_000));
    }

    @Test
    public void cityDrivingCanProbeAfterDisplacementExceedsAccuracyMargins() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        assertFalse(observe(tracker, 100, 20, 1_000));
        assertFalse(observe(tracker, 125, 20, 2_000));
        assertFalse(observe(tracker, 150, 20, 4_000));
        assertTrue(observe(tracker, 165, 20, 5_500));
    }

    @Test
    public void fastDrivingStillNeedsThreeSecondsBeforeFirstProbe() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        assertFalse(observe(tracker, 100, 20, 1_000));
        assertFalse(observe(tracker, 170, 20, 3_000));
        assertTrue(observe(tracker, 185, 20, 4_000));
    }

    @Test
    public void drivingRetriesNeedFiveSecondsAndDisplacementFromTheLastAttempt() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        observe(tracker, 100, 20, 1_000);
        assertTrue(observe(tracker, 185, 20, 4_000));
        tracker.recordAttempt(fix(185, 20), 4_000);
        assertFalse(observe(tracker, 250, 20, 7_000));
        assertFalse(observe(tracker, 220, 20, 9_000));
        assertTrue(observe(tracker, 260, 20, 10_000));
    }

    @Test
    public void displacementInsideAccuracyMarginsDoesNotTriggerEarlyProbe() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        observe(tracker, 100, 20, 1_000);
        for (long now = 4_000; now < 25_000; now += 3_000) {
            assertFalse(observe(tracker, 150, 20, now));
        }
        assertFalse(observe(tracker, 150, 20, 25_000));
        assertFalse(observe(tracker, 110, 20, 28_000));
        assertFalse(observe(tracker, 150, 20, 30_000));
    }

    @Test
    public void walkingWithBroadAccuracyGetsProbeWithoutConfirmedDivergence() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        for (int step = 0; step < 10; step++) {
            assertFalse(observe(tracker, 100 + step * 4, 30, 1_000 + step * 3_000L));
        }
        assertTrue(observe(tracker, 140, 30, 31_000));
        tracker.recordAttempt(fix(140, 30), 31_000);
        for (long now = 34_000; now <= 91_000; now += 3_000) {
            assertFalse(observe(tracker, 180, 30, now));
        }
        assertTrue(observe(tracker, 230, 30, 94_000));
    }

    @Test
    public void probeWaitRestartsAfterStationarityPoorAccuracyOrExplicitReset() {
        assertProbeWaitRestarts((tracker) -> tracker.shouldRequest(TARGET, fix(100, 30), true, 25_000));
        assertProbeWaitRestarts((tracker) -> observe(tracker, 100, 80, 25_000));
        assertProbeWaitRestarts((tracker) -> tracker.resetEvidence());
    }

    @Test
    public void probeWaitRestartsAfterLongSampleGap() {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        for (long now = 1_000; now <= 22_000; now += 3_000) {
            assertFalse(observe(tracker, 100, 30, now));
        }
        for (long now = 40_000; now < 70_000; now += 3_000) {
            assertFalse(observe(tracker, 100, 30, now));
        }
        assertTrue(observe(tracker, 100, 30, 70_000));
    }

    private static void assertProbeWaitRestarts(Consumer<BeelineRecoveryTracker> interrupt) {
        BeelineRecoveryTracker tracker = new BeelineRecoveryTracker();
        for (long now = 1_000; now <= 22_000; now += 3_000) {
            assertFalse(observe(tracker, 100, 30, now));
        }
        interrupt.accept(tracker);
        for (long now = 28_000; now < 58_000; now += 3_000) {
            assertFalse(observe(tracker, 100, 30, now));
        }
        assertTrue(observe(tracker, 100, 30, 58_000));
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
