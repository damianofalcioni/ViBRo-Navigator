package vibro.navigator.nav.location;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationStationarySleepTimingTest {
    @Test
    public void deliveryDelayCannotSupplyMissingStopObservationTime() {
        NavigationStationarySleep sleep = sleep();
        for (long receipt = 1_000; receipt <= 22_000; receipt += 3_000) {
            long measurement = 1_000 + (receipt - 1_000) * 4 / 5;
            assertFalse("Delivery time must not stretch a shorter measurement window into a stop",
                    observe(sleep, measurement, receipt, 0));
        }
        assertTrue(observe(sleep, 25_000, 25_000, 0));
    }

    @Test
    public void delayedClusteredMeasurementsCannotCompleteFallbackVerification() {
        NavigationStationarySleep sleep = sleeping();
        sleep.beginCheck();
        assertFalse(observe(sleep, 52_000, 52_000, 0));
        assertFalse(observe(sleep, 52_001, 54_000, 0));
        assertFalse(observe(sleep, 52_002, 56_000, 0));
        assertFalse("Only two independent observations have arrived",
                observe(sleep, 58_000, 58_000, 0));
        assertTrue(observe(sleep, 60_000, 60_000, 0));
    }

    @Test
    public void clusteredReceiptsCannotCompleteFallbackEvenWhenMeasurementsAreSpaced() {
        NavigationStationarySleep sleep = sleeping();
        sleep.beginCheck();
        assertFalse(observe(sleep, 48_000, 52_000, 0));
        assertFalse(observe(sleep, 50_000, 52_100, 0));
        assertFalse(observe(sleep, 52_000, 52_200, 0));
        assertFalse(observe(sleep, 56_000, 56_000, 0));
        assertTrue(observe(sleep, 58_000, 58_000, 0));
    }

    @Test
    public void relocationInsideSampleSpacingRestartsTheStopWindow() {
        NavigationStationarySleep sleep = sleep();
        for (long time = 1_000; time <= 19_000; time += 3_000) {
            assertFalse(observe(sleep, time, time, 0));
        }
        assertFalse(observe(sleep, 19_500, 19_500, 5));
        for (long time = 22_000; time <= 40_000; time += 3_000) {
            assertFalse("A return to the origin must gather a new complete stop window",
                    observe(sleep, time, time, 0));
        }
        assertTrue(observe(sleep, 43_000, 43_000, 0));
    }

    @Test
    public void relocationInsideFallbackSpacingRequiresFullConfirmationAgain() {
        NavigationStationarySleep sleep = sleeping();
        sleep.beginCheck();
        assertFalse(observe(sleep, 52_000, 52_000, 0));
        assertFalse(observe(sleep, 53_000, 53_000, 5));
        assertFalse(observe(sleep, 54_000, 54_000, 0));
        assertFalse(observe(sleep, 56_000, 56_000, 0));
        for (long time = 59_000; time <= 71_000; time += 3_000) {
            assertFalse(observe(sleep, time, time, 0));
        }
        assertTrue(observe(sleep, 74_000, 74_000, 0));
    }

    private static NavigationStationarySleep sleeping() {
        NavigationStationarySleep sleep = sleep();
        for (long time = 1_000; time < 22_000; time += 3_000) {
            assertFalse(observe(sleep, time, time, 0));
        }
        assertTrue(observe(sleep, 22_000, 22_000, 0));
        sleep.beginSleep(22_000);
        return sleep;
    }

    private static NavigationStationarySleep sleep() {
        return new NavigationStationarySleep(new NavigationMotionTrigger() {
            @Override public boolean arm(Runnable onMotion) { return true; }
            @Override public void cancel() { }
        }, () -> { });
    }

    private static boolean observe(NavigationStationarySleep sleep, long measurementMs,
            long receiptMs, double meters) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(measurementMs, measurementMs);
        location.setLatitude(meters / 111_195d);
        location.setAccuracy(5);
        // Speedless fixes are eligible only through independent fixed-origin evidence.
        return sleep.observe(location, new NavigationLocation(location), true, receiptMs);
    }
}
