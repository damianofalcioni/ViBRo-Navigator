package vibro.navigator.nav.location;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationLocationStallMonitorTest {
    @Test
    public void shouldRecover_waitsForRequestedIntervalPlusSlack() {
        NavigationLocationStallMonitor monitor = new NavigationLocationStallMonitor();
        monitor.start(10_000L);

        assertEquals(85_000L, monitor.recoveryDeadlineMs(60_000L));
        assertFalse(monitor.shouldRecover(84_999L, 60_000L));
        assertTrue(monitor.shouldRecover(85_000L, 60_000L));
    }

    @Test
    public void recordAcceptedFix_movesRecoveryDeadlineForward() {
        NavigationLocationStallMonitor monitor = new NavigationLocationStallMonitor();
        monitor.start(10_000L);
        monitor.recordAcceptedFix(70_000L);

        assertEquals(145_000L, monitor.recoveryDeadlineMs(60_000L));
    }

    @Test
    public void shortIntervals_useThirtySecondMinimumStallWindow() {
        assertEquals(30_000L, NavigationLocationStallMonitor.stallTimeoutMs(3_000L));
    }

    @Test
    public void recoveryAttemptPreventsImmediateRepeatedRestart() {
        NavigationLocationStallMonitor monitor = new NavigationLocationStallMonitor();
        monitor.start(10_000L);
        monitor.recordRecoveryAttempt(85_000L);

        assertFalse(monitor.shouldRecover(100_000L, 60_000L));
        assertTrue(monitor.shouldRecover(160_000L, 60_000L));
    }
}
