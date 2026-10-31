package vibro.navigator.nav.session;

import org.junit.Test;
import vibro.navigator.nav.power.NavigationBatteryBehaviorDetector;
import vibro.navigator.nav.power.NavigationBatteryObservation;
import vibro.navigator.nav.power.NavigationBatterySnapshot;
import static org.junit.Assert.*;

public class NavigationTripBatteryBehaviorTest {
    private static final long MINUTE_MS = 60_000L;
    private final NavigationBatteryBehaviorDetector detector = new NavigationBatteryBehaviorDetector();
    private final NavigationTripBatteryStats stats = new NavigationTripBatteryStats();

    @Test
    public void learningKeepsMahUnavailableThenEstimatesTheWholeUninterruptedTrip() {
        stats.start(snapshot(100, 80, 0));
        for (int i = 1; i < 3; i++) {
            stats.recordSnapshot(snapshot(100 + 10 * i, 80 - i, i * MINUTE_MS));
            assertTrue(Float.isNaN(stats.batteryUsedMilliAmpHours()));
        }
        stats.recordSnapshot(snapshot(130, 77, 3 * MINUTE_MS));
        assertEquals(30f, stats.batteryUsedMilliAmpHours(), 0f);
        assertTrue(stats.estimate().wholeTrip);
        assertEquals(3, stats.batteryDropPercent());
    }

    @Test
    public void counterResetMakesTheWholeTripUnavailableEvenAfterRelearning() {
        stats.start(snapshot(100, 80, 0));
        for (int i = 1; i <= 3; i++) {
            stats.recordSnapshot(snapshot(100 + 10 * i, 80 - i, i * MINUTE_MS));
        }
        assertEquals(30f, stats.batteryUsedMilliAmpHours(), 0f);
        stats.recordSnapshot(snapshot(1, 77, 4 * MINUTE_MS));
        for (int i = 1; i <= 4; i++) {
            stats.recordSnapshot(snapshot(1 + 10 * i, 77 - i, (4 + i) * MINUTE_MS));
        }
        assertTrue(Float.isNaN(stats.batteryUsedMilliAmpHours()));
        assertEquals(7, stats.batteryDropPercent());
        assertEquals(40f, stats.estimate().milliAmpHours, 0f);
        assertFalse(stats.estimate().wholeTrip);
        assertEquals(4 * MINUTE_MS, stats.estimate().coveredFromMs);
        assertEquals(8 * MINUTE_MS, stats.estimate().coveredUntilMs);
    }

    @Test
    public void chargingBreaksAccountingAndCachedSnapshotsCannotRenewEvidence() {
        NavigationBatterySnapshot initial = snapshot(100, 80, 0);
        stats.start(initial);
        for (int i = 0; i < 10; i++) {
            stats.recordSnapshot(initial);
        }
        assertTrue(Float.isNaN(stats.batteryUsedMilliAmpHours()));
        NavigationBatteryObservation charging = new NavigationBatteryObservation(99, 600, 80, MINUTE_MS,
                NavigationBatteryObservation.PowerState.CHARGING);
        stats.recordSnapshot(NavigationBatterySnapshot.observed(charging, detector.record(charging)));
        assertTrue(Float.isNaN(stats.estimate().milliAmpHours));
        for (int i = 0; i <= 3; i++) {
            stats.recordSnapshot(snapshot(100 + 10 * i, 80 - i, (2 + i) * MINUTE_MS));
        }
        assertTrue(Float.isNaN(stats.batteryUsedMilliAmpHours()));
        assertEquals(-1, stats.batteryDropPercent());
        assertEquals(30f, stats.estimate().milliAmpHours, 0f);
        assertFalse(stats.estimate().wholeTrip);
    }

    @Test
    public void repeatedReversalsNeverCombineSeparatePartialWindows() {
        stats.start(snapshot(100, 80, 0));
        for (int i = 1; i <= 3; i++) {
            stats.recordSnapshot(snapshot(100 + 10 * i, 80 - i, i * MINUTE_MS));
        }
        stats.recordSnapshot(snapshot(1, 77, 4 * MINUTE_MS));
        for (int i = 1; i <= 3; i++) {
            stats.recordSnapshot(snapshot(1 + 10 * i, 77 - i, (4 + i) * MINUTE_MS));
        }
        assertEquals(30f, stats.estimate().milliAmpHours, 0f);
        stats.recordSnapshot(snapshot(0, 74, 8 * MINUTE_MS));
        assertTrue(Float.isNaN(stats.estimate().milliAmpHours));
        for (int i = 1; i <= 3; i++) {
            stats.recordSnapshot(snapshot(10 * i, 74 - i, (8 + i) * MINUTE_MS));
        }
        assertTrue(Float.isNaN(stats.batteryUsedMilliAmpHours()));
        assertEquals(30f, stats.estimate().milliAmpHours, 0f);
        assertEquals(8 * MINUTE_MS, stats.estimate().coveredFromMs);
    }

    @Test
    public void missingCounterClearsPartialCoverageAndCachedReadsCannotExtendIt() {
        stats.start(snapshot(100, 80, 0));
        stats.recordSnapshot(snapshot(0, 80, MINUTE_MS));
        for (int i = 1; i <= 4; i++) {
            stats.recordSnapshot(snapshot(10 * i, 80 - i, (1 + i) * MINUTE_MS));
        }
        assertFalse(stats.estimate().wholeTrip);
        NavigationBatterySnapshot cached = snapshot(50, 75, 6 * MINUTE_MS);
        stats.recordSnapshot(cached);
        long coveredUntil = stats.estimate().coveredUntilMs;
        for (int i = 0; i < 5; i++) {
            stats.recordSnapshot(cached);
        }
        assertEquals(coveredUntil, stats.estimate().coveredUntilMs);
        stats.recordSnapshot(snapshot(Integer.MIN_VALUE, 75, 7 * MINUTE_MS));
        assertTrue(Float.isNaN(stats.estimate().milliAmpHours));
    }

    private NavigationBatterySnapshot snapshot(int counter, int level, long time) {
        NavigationBatteryObservation observation = new NavigationBatteryObservation(counter, -600, level, time,
                NavigationBatteryObservation.PowerState.DISCHARGING);
        return NavigationBatterySnapshot.observed(observation, detector.record(observation));
    }
}
