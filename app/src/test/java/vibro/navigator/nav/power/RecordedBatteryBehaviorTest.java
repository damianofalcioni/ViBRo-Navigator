package vibro.navigator.nav.power;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecordedBatteryBehaviorTest {
    @Test
    public void recordedHuaweiDischargeLearnsUnitsAndDirectionWithoutAnIdentityRule() throws Exception {
        assertRecorded("huawei-discharge-20261027.csv", 218f);
        assertRecorded("huawei-discharge-20261030.csv", 94f);
    }

    private static void assertRecorded(String fixture, float expectedMah) throws Exception {
        NavigationBatteryBehaviorDetector detector = new NavigationBatteryBehaviorDetector();
        NavigationBatterySnapshot first = null;
        NavigationBatterySnapshot last = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                RecordedBatteryBehaviorTest.class.getResourceAsStream("/battery-replay/" + fixture),
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
                last = NavigationBatterySnapshot.observed(observation, detector.record(observation));
                if (first == null) {
                    first = last;
                }
            }
        }
        assertNotNull(last);
        assertEquals(NavigationBatteryBehavior.State.INFERRED, last.behavior.state);
        assertEquals(1_000, last.behavior.counterMultiplier);
        assertEquals(1_000, last.behavior.currentMultiplier);
        assertEquals(1, last.behavior.dischargeDirection);
        assertEquals(expectedMah, last.estimatedUsedMilliAmpHours(first), 0f);
    }
}
