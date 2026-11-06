package vibro.navigator.nav.session;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import vibro.navigator.nav.format.NavigationTripStatsFormatter;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.model.NavTripStatus;
import vibro.navigator.nav.power.NavigationBatteryBehaviorDetector;
import vibro.navigator.nav.power.NavigationBatteryObservation;
import vibro.navigator.nav.power.NavigationBatterySnapshot;
import static org.junit.Assert.*;

public class NavigationBatteryCounterFoldReplayTest {
    @Test
    public void recordedZeroCrossingShowsOnlyTheSupportedPostReversalWindow() throws Exception {
        NavigationBatteryBehaviorDetector detector = new NavigationBatteryBehaviorDetector();
        NavigationTripStatsTracker tracker = new NavigationTripStatsTracker();
        NavigationBatterySnapshot last = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/battery-replay/huawei-counter-fold-20261031.csv"),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split(",");
                NavigationBatteryObservation observation = new NavigationBatteryObservation(Integer.parseInt(fields[1]),
                        Integer.parseInt(fields[2]), Integer.parseInt(fields[3]), Long.parseLong(fields[0]),
                        NavigationBatteryObservation.PowerState.DISCHARGING);
                NavigationBatterySnapshot snapshot = NavigationBatterySnapshot.observed(observation,
                        detector.record(observation));
                if (last == null) {
                    tracker.start(observation.elapsedMs, true, snapshot);
                } else {
                    tracker.recordBatterySnapshot(snapshot);
                }
                last = snapshot;
            }
        }
        assertNotNull(last);
        assertTrue(last.behavior.isInferred());
        NavTripStatus status = tracker.snapshot();
        assertTrue(Float.isNaN(status.batteryUsedMilliAmpHours));
        assertEquals(4, status.batteryDropPercent);
        String details = NavigationTripStatsFormatter.formatDetails(TestNavigationTextResources.metric(),
                status, last.observedAtElapsedMs);
        assertTrue(details, details.contains("partial"));
        assertTrue(details, details.contains("≈38.0 mAh"));
        assertTrue(details, details.contains("5 min 25 s"));
    }
}
