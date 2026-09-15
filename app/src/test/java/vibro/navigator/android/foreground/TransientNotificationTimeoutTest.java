package vibro.navigator.android.foreground;

import static org.junit.Assert.assertEquals;

import vibro.navigator.dispatch.TaskScheduler;

import org.junit.Test;

public class TransientNotificationTimeoutTest {

    @Test
    public void removesNotificationAfterFiveSeconds() {
        TestScheduler scheduler = new TestScheduler();
        int[] removeCalls = {0};
        TransientNotificationTimeout timeout = new TransientNotificationTimeout(
                scheduler,
                () -> removeCalls[0]++
        );

        timeout.schedule();

        assertEquals(5_000L, scheduler.delayMs);
        scheduler.runDelayedTask();

        assertEquals(1, removeCalls[0]);
    }

    @Test
    public void olderTimeoutCannotRemoveNewerNotification() {
        TestScheduler scheduler = new TestScheduler();
        int[] removeCalls = {0};
        TransientNotificationTimeout timeout = new TransientNotificationTimeout(
                scheduler,
                () -> removeCalls[0]++
        );

        timeout.schedule();
        Runnable olderRemoval = scheduler.delayedTask;
        timeout.schedule();
        olderRemoval.run();

        assertEquals(0, removeCalls[0]);
        scheduler.runDelayedTask();
        assertEquals(1, removeCalls[0]);
    }

    private static final class TestScheduler implements TaskScheduler {
        private Runnable delayedTask;
        private long delayMs;

        @Override
        public void post(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void postDelayed(Runnable runnable, long delayMs) {
            delayedTask = runnable;
            this.delayMs = delayMs;
        }

        @Override
        public void removeCallbacks(Runnable runnable) {
            if (runnable.equals(delayedTask)) {
                delayedTask = null;
            }
        }

        private void runDelayedTask() {
            Runnable task = delayedTask;
            delayedTask = null;
            task.run();
        }
    }
}
