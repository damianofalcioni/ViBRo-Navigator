package vibro.navigator.nav.power;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationChargeCounterValidationTest {
    @Test
    public void recordedImplausibleScaleNeverBecomesAUsableChargeCounter() {
        NavigationChargeCounterValidation validation = new NavigationChargeCounterValidation();
        for (int value : new int[] {103, 188, 257, 310, 321}) {
            assertEquals("implausible_scale", validation.record(value, false));
            assertFalse(NavigationBatterySnapshot.of(value, 69).hasChargeCounter());
        }
    }

    @Test
    public void dischargeIncreaseChargingAndUnsupportedReadingsHaveDistinctReasons() {
        NavigationChargeCounterValidation validation = new NavigationChargeCounterValidation();
        assertEquals("plausible_unvalidated", validation.record(2_100_000, false));
        assertEquals("plausible_unvalidated", validation.record(2_099_000, false));
        assertEquals("counter_increased", validation.record(2_101_000, false));
        assertEquals("charging", validation.record(2_102_000, true));
        assertEquals("unsupported", validation.record(Integer.MIN_VALUE, false));
    }
}
