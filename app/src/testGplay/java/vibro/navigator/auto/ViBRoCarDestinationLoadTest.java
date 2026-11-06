package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.concurrent.FutureTask;

import vibro.navigator.dispatch.TaskScheduler;

public class ViBRoCarDestinationLoadTest {
    private final ManualScheduler scheduler = new ManualScheduler();
    private long nowMs;
    private int expirations;
    private final ViBRoCarDestinationLoad load = new ViBRoCarDestinationLoad(scheduler, () -> nowMs,
            () -> expirations++);

    @Test
    public void timeoutCancelsWorkerAndOnlyExpiresOnce() {
        int token = load.begin();
        FutureTask<Void> work = new FutureTask<>(() -> null);
        load.submitted(token, work);
        Runnable timeout = scheduler.pending.peek();
        nowMs = 9_000;
        timeout.run();
        timeout.run();
        assertTrue(work.isCancelled());
        assertFalse(load.accept(token));
        assertFalse(load.isLoading());
        assertEquals(1, expirations);
        assertTrue(scheduler.pending.isEmpty());
    }

    @Test
    public void lateCallbackCannotExpireANewerAttempt() {
        load.begin();
        Runnable old = scheduler.pending.peek();
        int latest = load.begin();
        old.run();
        assertTrue(load.accept(latest));
        assertEquals(0, expirations);
        assertEquals(1, scheduler.pending.size());
    }

    @Test
    public void completionRemovesDeadlineAndRejectsDuplicateDelivery() {
        int token = load.begin();
        nowMs = 8_999;
        assertTrue(load.accept(token));
        load.finish();
        assertFalse(load.accept(token));
        assertTrue(scheduler.pending.isEmpty());
        assertEquals(0, expirations);
    }

    @Test
    public void delayedResultExpiresBeforeItCanBeUsed() {
        int token = load.begin();
        nowMs = 9_000;
        assertFalse(load.accept(token));
        assertEquals(1, expirations);
        assertTrue(scheduler.pending.isEmpty());
    }

    @Test
    public void workSubmittedAfterCancellationIsCancelledImmediately() {
        int token = load.begin();
        load.cancel();
        FutureTask<Void> work = new FutureTask<>(() -> null);
        load.submitted(token, work);
        assertTrue(work.isCancelled());
        assertEquals(0, expirations);
    }

    private static final class ManualScheduler implements TaskScheduler {
        private final ArrayDeque<Runnable> pending = new ArrayDeque<>();
        @Override public void post(Runnable runnable) { runnable.run(); }
        @Override public void postDelayed(Runnable runnable, long delayMs) { pending.add(runnable); }
        @Override public void removeCallbacks(Runnable runnable) { pending.remove(runnable); }
    }
}
