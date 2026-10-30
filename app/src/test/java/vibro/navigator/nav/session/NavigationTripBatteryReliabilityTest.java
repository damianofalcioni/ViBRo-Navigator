package vibro.navigator.nav.session;

import org.junit.Test;
import vibro.navigator.nav.power.NavigationBatterySnapshot;
import vibro.navigator.nav.power.NavigationChargeCounterValidation;
import static org.junit.Assert.*;

public class NavigationTripBatteryReliabilityTest {
    private final NavigationTripBatteryStats stats = new NavigationTripBatteryStats();

    @Test
    public void downwardGaugeResetInvalidatesConsumptionForTheRestOfTheTrip() {
        stats.start(fix(3_000_000, 80, 1_000));
        stats.recordSnapshot(fix(1_000_000, 80, 61_000));
        assertTrue(Float.isNaN(stats.batteryUsedMilliAmpHours()));
        stats.recordSnapshot(fix(999_000, 79, 121_000));
        assertTrue(Float.isNaN(stats.batteryUsedMilliAmpHours()));
        assertEquals(1, stats.batteryDropPercent());
    }

    @Test
    public void unchangedFreshReadingsStillAdvanceTheDiscontinuityTimeAnchor() {
        stats.start(fix(3_000_000, 80, 1_000));
        stats.recordSnapshot(fix(3_000_000, 80, 3_601_000));
        stats.recordSnapshot(fix(2_800_000, 75, 3_602_000));
        assertTrue(Float.isNaN(stats.batteryUsedMilliAmpHours()));
    }

    @Test
    public void plausibleDischargeAndPercentagePlateauRemainUsable() {
        stats.start(fix(3_000_000, 80, 1_000));
        stats.recordSnapshot(fix(2_990_000, 80, 61_000));
        stats.recordSnapshot(fix(2_987_500, 80, 121_000));
        assertEquals(12.5f, stats.batteryUsedMilliAmpHours(), 0f);
        assertEquals(0, stats.batteryDropPercent());
    }

    @Test
    public void longObservedDischargeCanConsumeMoreThanAShortResetThreshold() {
        stats.start(fix(3_000_000, 80, 1_000));
        stats.recordSnapshot(fix(2_100_000, 56, 21_601_000));
        assertEquals(900f, stats.batteryUsedMilliAmpHours(), 0f);
        assertEquals(24, stats.batteryDropPercent());
    }

    @Test
    public void unknownTimesAndClockRegressionCannotValidateLargeLosses() {
        stats.start(NavigationBatterySnapshot.of(3_000_000, 80));
        stats.recordSnapshot(NavigationBatterySnapshot.of(1_000_000, 80));
        assertTrue(Float.isNaN(stats.batteryUsedMilliAmpHours()));
        stats.start(fix(3_000_000, 80, 61_000));
        stats.recordSnapshot(fix(2_990_000, 79, 1_000));
        assertTrue(Float.isNaN(stats.batteryUsedMilliAmpHours()));
    }

    @Test
    public void diagnosticsUseTheSameResetRuleAsTripConsumption() {
        NavigationChargeCounterValidation validation = new NavigationChargeCounterValidation();
        assertEquals("plausible_unvalidated", validation.record(fix(3_000_000, 80, 1_000), false));
        assertEquals("counter_discontinuity", validation.record(fix(1_000_000, 80, 61_000), false));
        assertEquals("charging", validation.record(fix(1_000_001, 80, 62_000), true));
        assertEquals("plausible_unvalidated", validation.record(fix(1_000_000, 80, 63_000), false));
    }

    private static NavigationBatterySnapshot fix(int counter, int level, long time) {
        return NavigationBatterySnapshot.of(counter, level, time);
    }
}
