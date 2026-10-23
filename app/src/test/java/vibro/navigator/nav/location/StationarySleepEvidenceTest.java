package vibro.navigator.nav.location;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StationarySleepEvidenceTest {
    @Test
    public void missingSpeedStillRequiresTwentySecondsOfQuietAndFreshFixes() {
        Fixture fixture = new Fixture();
        for (long time = 1_000L; time < 22_000L; time += 3_000L) {
            assertFalse(fixture.observe(time, 0d));
        }
        assertTrue(fixture.observe(22_000L, 0d));
        fixture.trigger.callback.run();
        assertEquals(1, fixture.wakes);
        assertFalse(fixture.observe(25_000L, 0d));
        assertFalse(fixture.observe(28_000L, 0d));
        assertFalse(fixture.observe(31_000L, 0d));
    }

    @Test
    public void speedlessWalkingCannotMoveTheStopOriginAlongItsPath() {
        Fixture fixture = new Fixture();
        for (int index = 0; index < 30; index++) {
            assertFalse(fixture.observe(1_000L + index * 3_000L, index * 0.7d));
        }
    }

    @Test
    public void oneUnexpectedTenSecondGapRestartsConfirmation() {
        Fixture fixture = new Fixture();
        assertFalse(fixture.observe(1_000L, 0d));
        assertFalse(fixture.observe(4_000L, 0d));
        assertFalse(fixture.observe(7_000L, 0d));
        assertFalse(fixture.observe(17_000L, 0d));
        assertFalse(fixture.observe(20_000L, 0d));
        assertFalse(fixture.observe(23_000L, 0d));
        assertTrue(fixture.decisions.stream().anyMatch(message -> message.contains("reason=callback_gap")));
    }

    @Test
    public void sustainedGapsAboveFifteenSecondsNeverConfirmAStop() {
        Fixture fixture = new Fixture();
        for (long time = 1_000L; time <= 200_000L; time += 16_000L) {
            assertFalse(fixture.observe(time, 0d));
        }
    }

    @Test
    public void staleOrInaccurateSpeedlessFixesRemainBlockedAndExplainWhy() {
        Fixture fixture = new Fixture();
        NavigationLocation fix = fix(1_000L, 0d);
        assertFalse(fixture.sleep.observe(fix, fix, true, 7_000L));
        assertTrue(fixture.lastDecision().contains("reason=measurement_not_fresh"));
        fix.setTime(8_000L, 8_000L);
        fix.setAccuracy(25f);
        assertFalse(fixture.sleep.observe(fix, fix, true, 8_000L));
        assertTrue(fixture.lastDecision().contains("reason=position_accuracy"));
        fix.setAccuracy(5f);
        fix.setSpeed(0.8f);
        assertFalse(fixture.sleep.observe(fix, fix, true, 8_000L));
        assertTrue(fixture.lastDecision().contains("reason=native_speed_moving_or_invalid"));
    }

    @Test
    public void unavailableSensorIsExplainedAndRepeatedBlockMessagesAreThrottled() {
        Fixture fixture = new Fixture();
        fixture.trigger.available = false;
        for (long time = 1_000L; time <= 28_000L; time += 3_000L) {
            assertFalse(fixture.observe(time, 0d));
        }
        long blocked = fixture.decisions.stream().filter(message -> message.contains(
                "reason=motion_trigger_unavailable_or_registration_failed")).count();
        assertEquals(1L, blocked);
    }

    private static NavigationLocation fix(long time, double meters) {
        NavigationLocation fix = new NavigationLocation("fused");
        fix.setTime(time, time);
        fix.setLatitude(meters / 111_195d);
        fix.setAccuracy(5f);
        return fix;
    }

    private static final class Fixture {
        private final Trigger trigger = new Trigger();
        private final List<String> decisions = new ArrayList<>();
        private int wakes;
        private final NavigationStationarySleep sleep = new NavigationStationarySleep(
                trigger, () -> wakes++, decisions::add);

        boolean observe(long time, double meters) {
            NavigationLocation fix = fix(time, meters);
            return sleep.observe(fix, fix, true, time);
        }

        String lastDecision() { return decisions.get(decisions.size() - 1); }
    }

    private static final class Trigger implements NavigationMotionTrigger {
        private Runnable callback;
        private boolean available = true;
        @Override public boolean arm(Runnable onMotion) { callback = onMotion; return available; }
        @Override public void cancel() { callback = null; }
    }
}
