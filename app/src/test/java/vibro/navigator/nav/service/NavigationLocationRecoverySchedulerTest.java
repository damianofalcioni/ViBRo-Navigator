package vibro.navigator.nav.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocationRecoveryAlarm;

public class NavigationLocationRecoverySchedulerTest {
    @Test
    public void duplicateDeadlineIsScheduledOnlyOnce() {
        FakeAlarm alarm = new FakeAlarm();
        NavigationLocationRecoveryScheduler scheduler = new NavigationLocationRecoveryScheduler(alarm);

        scheduler.schedule(45_000L);
        scheduler.schedule(45_000L);

        assertEquals(1, alarm.scheduleCalls);
    }

    @Test
    public void missingDeadlineDoesNotReachAndroidAlarm() {
        FakeAlarm alarm = new FakeAlarm();
        NavigationLocationRecoveryScheduler scheduler = new NavigationLocationRecoveryScheduler(alarm);

        scheduler.schedule(-1L);

        assertEquals(0, alarm.scheduleCalls);
    }

    @Test
    public void triggeredOrFailedAlarmCanBeScheduledAgain() {
        FakeAlarm alarm = new FakeAlarm();
        NavigationLocationRecoveryScheduler scheduler = new NavigationLocationRecoveryScheduler(alarm);

        alarm.scheduleSucceeds = false;
        scheduler.schedule(45_000L);
        alarm.scheduleSucceeds = true;
        scheduler.schedule(45_000L);
        scheduler.onAlarmTriggered();
        scheduler.schedule(45_000L);

        assertEquals(3, alarm.scheduleCalls);
    }

    private static final class FakeAlarm implements NavigationLocationRecoveryAlarm {
        private int scheduleCalls;
        private boolean scheduleSucceeds = true;

        @Override
        public boolean schedule(long triggerElapsedRealtimeMs) {
            scheduleCalls++;
            return scheduleSucceeds;
        }

        @Override
        public void cancel() {
        }
    }
}
