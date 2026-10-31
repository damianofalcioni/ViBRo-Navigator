package vibro.navigator.nav.location;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationStationarySleepHandoverTest {
    private static final String GPS = "gps";
    private static final String FUSED = "fused";

    @Test
    public void nearbyProviderHandoverStartsAnIndependentStopWindow() {
        Trigger trigger = new Trigger();
        NavigationStationarySleep sleep = new NavigationStationarySleep(trigger, () -> { });
        for (long time = 1_000; time <= 19_000; time += 3_000) {
            assertFalse(observe(sleep, fix(FUSED, time), time));
        }
        assertFalse("A new provider cannot finish the old provider's stop window",
                observe(sleep, fix(GPS, 22_000), 22_000));
        for (long time = 25_000; time <= 40_000; time += 3_000) {
            assertFalse(observe(sleep, fix(GPS, time), time));
        }
        assertTrue(observe(sleep, fix(GPS, 43_000), 43_000));
    }

    @Test
    public void rawCallbackFromAnotherProviderCannotVerifyTheSelectedPosition() {
        Trigger trigger = new Trigger();
        NavigationStationarySleep sleep = new NavigationStationarySleep(trigger, () -> { });
        for (long time = 1_000; time <= 25_000; time += 3_000) {
            assertFalse(sleep.observe(fix(GPS, time), fix(FUSED, time), true, time));
        }
        assertEquals(0, trigger.arms);
    }

    @Test
    public void rawAndFilteredPositionsMustDescribeTheSameObservation() {
        Trigger trigger = new Trigger();
        NavigationStationarySleep sleep = new NavigationStationarySleep(trigger, () -> { });
        for (long time = 1_000; time <= 25_000; time += 3_000) {
            assertFalse(sleep.observe(fix(GPS, time), fix(GPS, time - 500), true, time));
        }
        assertEquals(0, trigger.arms);
    }

    @Test
    public void clusteredHandoverCannotLeaveTheEarlierStopWindowIntact() {
        NavigationStationarySleep sleep = new NavigationStationarySleep(new Trigger(), () -> { });
        for (long time = 1_000; time <= 19_000; time += 3_000) {
            assertFalse(observe(sleep, fix(FUSED, time), time));
        }
        assertFalse(observe(sleep, fix(GPS, 19_500), 19_500));
        assertFalse(observe(sleep, fix(FUSED, 22_000), 22_000));
    }

    @Test
    public void providerChangeDuringFallbackCheckRequiresTheFullStopWindow() {
        NavigationStationarySleep sleep = new NavigationStationarySleep(new Trigger(), () -> { });
        for (long time = 1_000; time <= 19_000; time += 3_000) {
            assertFalse(observe(sleep, fix(FUSED, time), time));
        }
        assertTrue(observe(sleep, fix(FUSED, 22_000), 22_000));
        sleep.beginSleep(22_000);
        sleep.beginCheck();
        assertFalse(observe(sleep, fix(GPS, 52_000), 52_000));
        assertFalse(observe(sleep, fix(GPS, 54_000), 54_000));
        assertFalse(observe(sleep, fix(GPS, 56_000), 56_000));
        for (long time = 59_000; time <= 71_000; time += 3_000) {
            assertFalse(observe(sleep, fix(GPS, time), time));
        }
        assertTrue(observe(sleep, fix(GPS, 74_000), 74_000));
    }

    private static boolean observe(NavigationStationarySleep sleep, NavigationLocation raw, long nowMs) {
        return sleep.observe(raw, new NavigationLocation(raw), true, nowMs);
    }

    private static NavigationLocation fix(String provider, long time) {
        NavigationLocation location = new NavigationLocation(provider);
        location.setTime(time, time);
        location.setAccuracy(5);
        location.setSpeed(0);
        return location;
    }

    private static final class Trigger implements NavigationMotionTrigger {
        int arms;
        @Override public boolean arm(Runnable callback) { arms++; return true; }
        @Override public void cancel() { }
    }
}
