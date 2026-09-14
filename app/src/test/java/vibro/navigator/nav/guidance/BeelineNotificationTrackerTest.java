package vibro.navigator.nav.guidance;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.geo.LatLon;

public class BeelineNotificationTrackerTest {
    private static final LatLon TARGET = new LatLon(0.0, 0.0);

    @Test
    public void notifiesAtTenSecondChecksOnlyWhenDistanceExceedsLastNotification() {
        BeelineNotificationTracker tracker = new BeelineNotificationTracker();

        assertFalse(tracker.shouldNotify(TARGET, 100.0, 1_000L));
        assertFalse(tracker.shouldNotify(TARGET, 120.0, 10_999L));
        assertTrue(tracker.shouldNotify(TARGET, 120.0, 11_000L));
        assertFalse(tracker.shouldNotify(TARGET, 130.0, 20_999L));
        assertFalse(tracker.shouldNotify(TARGET, 110.0, 21_000L));
        assertTrue(tracker.shouldNotify(TARGET, 121.0, 31_000L));
    }

    @Test
    public void targetChangeStartsANewLegBaselineAndCadence() {
        BeelineNotificationTracker tracker = new BeelineNotificationTracker();
        LatLon nextTarget = new LatLon(0.0, 0.001);

        assertFalse(tracker.shouldNotify(TARGET, 100.0, 1_000L));
        assertTrue(tracker.shouldNotify(TARGET, 120.0, 11_000L));
        assertFalse(tracker.shouldNotify(nextTarget, 50.0, 12_000L));
        assertFalse(tracker.shouldNotify(nextTarget, 70.0, 21_999L));
        assertTrue(tracker.shouldNotify(nextTarget, 70.0, 22_000L));
    }

    @Test
    public void directGuidancePollingIsCappedAtTenSeconds() {
        assertEquals(10_000L, BeelineNotificationTracker.limitSuggestedUpdateInterval(60_000L));
        assertEquals(3_000L, BeelineNotificationTracker.limitSuggestedUpdateInterval(3_000L));
        assertEquals(10_000L, BeelineNotificationTracker.limitSuggestedUpdateInterval(-1L));
    }
}
