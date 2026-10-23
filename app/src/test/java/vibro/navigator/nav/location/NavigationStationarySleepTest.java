package vibro.navigator.nav.location;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationStationarySleepTest {
    @Test
    public void requiresTwentySecondsOfFreshStoppedFixesAndAnArmedTrigger() {
        Fixture fixture = new Fixture();
        for (long time = 1_000L; time < 21_000L; time += 3_000L) {
            assertFalse(fixture.observe(time, 0d));
        }
        assertTrue(fixture.observe(22_000L, 0d));
        assertEquals(1, fixture.trigger.registrations);
        fixture.sleep.beginSleep(22_000L);
        assertTrue(fixture.sleep.isSleeping());
        assertEquals(52_000L, fixture.sleep.deadlineMs());
    }

    @Test
    public void missingTriggerOrDisabledPolicyNeverPermitsSleep() {
        Fixture fixture = new Fixture();
        fixture.trigger.available = false;
        for (long time = 1_000L; time <= 31_000L; time += 3_000L) {
            assertFalse(fixture.observe(time, 0d));
        }
        fixture.trigger.available = true;
        fixture.confirmStop();
        NavigationLocation fix = fix(25_000L, 0d);
        assertFalse(fixture.sleep.observe(fix, fix, false, 25_000L));
        assertFalse(fixture.observe(28_000L, 0d));
    }

    @Test
    public void walkingCannotCreepStopOriginWithRepeatedZeroSpeedFixes() {
        Fixture fixture = new Fixture();
        for (int index = 0; index < 20; index++) {
            assertFalse(fixture.observe(1_000L + index * 3_000L, index * 0.7d));
        }
    }

    @Test
    public void stalePoorOrMovingFixesCannotConfirmAStop() {
        Fixture fixture = new Fixture();
        fixture.confirmStop();
        NavigationLocation fix = fix(25_000L, 0d);
        fix.setSpeed(0.8f);
        assertFalse(fixture.sleep.observe(fix, fix, true, 25_000L));
        fix.setSpeed(0f);
        fix.setAccuracy(40f);
        assertFalse(fixture.sleep.observe(fix, fix, true, 28_000L));
        fix.setAccuracy(5f);
        assertFalse(fixture.sleep.observe(fix, fix, true, 31_000L));
        assertFalse(fixture.observe(34_000L, 0d));
    }

    @Test
    public void cachedOrClusteredCallbacksCannotProvideStopDuration() {
        Fixture fixture = new Fixture();
        NavigationLocation fix = fix(1_000L, 0d);
        for (long time = 1_000L; time <= 25_000L; time += 1_000L) {
            assertFalse(fixture.sleep.observe(fix, fix, true, time));
        }
    }

    @Test
    public void motionCancelsSleepAndRequiresFullConfirmationAgain() {
        Fixture fixture = new Fixture();
        fixture.confirmStop();
        fixture.sleep.beginSleep(22_000L);
        fixture.trigger.fire();
        assertFalse(fixture.sleep.isSleeping());
        assertEquals(1, fixture.wakes);
        assertFalse(fixture.observe(25_000L, 0d));
        assertFalse(fixture.observe(28_000L, 0d));
        assertFalse(fixture.observe(31_000L, 0d));
    }

    @Test
    public void periodicCheckNeedsThreeIndependentFixesAtTheOriginalStop() {
        Fixture fixture = new Fixture();
        fixture.confirmStop();
        fixture.sleep.beginSleep(22_000L);
        fixture.sleep.beginCheck();
        assertFalse(fixture.observe(52_000L, 0d));
        assertFalse(fixture.observe(54_000L, 0d));
        assertTrue(fixture.observe(56_000L, 0d));
        assertEquals(1, fixture.trigger.registrations);
    }

    @Test
    public void missedDepartureOrLongCheckGapRequiresFullStopConfirmation() {
        Fixture fixture = new Fixture();
        fixture.confirmStop();
        fixture.sleep.beginSleep(22_000L);
        fixture.sleep.beginCheck();
        assertFalse(fixture.observe(52_000L, 8d));
        assertFalse(fixture.observe(54_000L, 8d));
        assertFalse(fixture.observe(56_000L, 8d));
        assertFalse(fixture.observe(66_000L, 8d));
        assertFalse(fixture.observe(69_000L, 8d));
    }

    private static NavigationLocation fix(long timeMs, double meters) {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setTime(timeMs, timeMs);
        fix.setLatitude(meters / 111_195d);
        fix.setAccuracy(5f);
        fix.setSpeed(0f);
        return fix;
    }

    private static final class Fixture {
        private final Trigger trigger = new Trigger();
        private int wakes;
        private final NavigationStationarySleep sleep = new NavigationStationarySleep(trigger, () -> wakes++);

        boolean observe(long time, double meters) {
            NavigationLocation fix = fix(time, meters);
            return sleep.observe(fix, fix, true, time);
        }

        void confirmStop() {
            for (long time = 1_000L; time <= 22_000L; time += 3_000L) {
                observe(time, 0d);
            }
        }
    }

    private static final class Trigger implements NavigationMotionTrigger {
        private boolean available = true;
        private Runnable callback;
        private int registrations;
        @Override public boolean arm(Runnable onMotion) {
            registrations++;
            callback = onMotion;
            return available;
        }
        @Override public void cancel() { callback = null; }
        void fire() { callback.run(); }
    }
}
