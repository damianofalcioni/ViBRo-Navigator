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
        for (int i = 0; i <= 3; i++) {
            stats.recordSnapshot(snapshot(100 + 10 * i, 80 - i, (2 + i) * MINUTE_MS));
        }
        assertTrue(Float.isNaN(stats.batteryUsedMilliAmpHours()));
        assertEquals(-1, stats.batteryDropPercent());
    }

    private NavigationBatterySnapshot snapshot(int counter, int level, long time) {
        NavigationBatteryObservation observation = new NavigationBatteryObservation(counter, -600, level, time,
                NavigationBatteryObservation.PowerState.DISCHARGING);
        return NavigationBatterySnapshot.observed(observation, detector.record(observation));
    }
}
