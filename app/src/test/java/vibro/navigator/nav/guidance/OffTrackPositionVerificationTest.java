package vibro.navigator.nav.guidance;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OffTrackPositionVerificationTest {
    @Test
    public void octoberLogCorrectionDoesNotBecomeDepartureAfterSixSeconds() {
        OffTrackPositionVerification guard = new OffTrackPositionVerification();
        assertFalse(guard.shouldHold(fix(1_000), 4, 15, 1_000));
        assertTrue(guard.shouldHold(fix(63_820), 105.27, 21.939, 63_820));
        assertTrue(guard.shouldHold(fix(66_022), 72.57, 41.575, 66_022));
        assertTrue(guard.shouldHold(fix(67_023), 50.03, 31.009, 67_023));
        assertTrue(guard.shouldHold(fix(68_028), 40.92, 26.853, 68_028));
        assertTrue(guard.shouldHold(fix(69_842), 37.66, 20.815, 69_842));
        assertTrue(guard.shouldHold(fix(72_842), 18, 14, 72_842));
        assertFalse(guard.shouldHold(fix(75_842), 14, 13, 75_842));
    }

    @Test
    public void realRelocationCanSettleWhileDuplicatesCannotProvideConfirmation() {
        OffTrackPositionVerification guard = new OffTrackPositionVerification();
        guard.shouldHold(fix(1_000), 0, 5, 1_000);
        assertTrue(guard.shouldHold(fix(61_000), 100, 5, 61_000));
        assertTrue(guard.shouldHold(fix(61_000), 100, 5, 61_100));
        assertTrue(guard.shouldHold(fix(64_000), 102, 5, 64_000));
        assertTrue(guard.shouldHold(fix(67_000), 104, 5, 67_000));
        assertFalse(guard.shouldHold(fix(70_000), 105, 5, 70_000));
    }

    @Test
    public void expectedLongOnRouteIntervalsDoNotForceVerification() {
        OffTrackPositionVerification guard = new OffTrackPositionVerification();
        assertFalse(guard.shouldHold(fix(1_000), 0, 5, 1_000));
        assertFalse(guard.shouldHold(fix(61_000), 4, 5, 61_000));
        guard.reset();
        assertFalse(guard.shouldHold(fix(1_000), 0, 5, 1_000));
    }

    private static NavigationLocation fix(long time) {
        NavigationLocation location = new NavigationLocation("fused");
        location.setTime(time, time);
        return location;
    }
}
