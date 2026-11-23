package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void replacementRunsWhileCancelledLookupStillOccupiesAWorker() throws InterruptedException {
        assertReplacementRuns(false);
    }

    @Test
    public void retryRunsWhileTimedOutLookupStillOccupiesAWorker() throws InterruptedException {
        assertReplacementRuns(true);
    }

    @Test
    public void rejectedWorkLeavesLoadingImmediately() {
        ExecutorService worker = ViBRoCarDestinationLoad.newWorker();
        worker.shutdownNow();
        load.submit(load.begin(), worker, () -> { throw new AssertionError(); });
        assertFalse(load.isLoading());
        assertEquals(1, expirations);
        assertTrue(scheduler.pending.isEmpty());
    }

    private void assertReplacementRuns(boolean expireFirst) throws InterruptedException {
        ExecutorService worker = ViBRoCarDestinationLoad.newWorker();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch latestStarted = new CountDownLatch(1);
        Semaphore releaseFirst = new Semaphore(0);
        try {
            int first = load.begin();
            load.submit(first, worker, () -> {
                firstStarted.countDown();
                // Emulate an I/O call that cannot be stopped by interrupting its thread.
                releaseFirst.acquireUninterruptibly();
            });
            assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
            if (expireFirst) {
                nowMs = 9_000;
                load.refresh();
            }
            int latest = load.begin();
            load.submit(latest, worker, latestStarted::countDown);
            assertTrue(latestStarted.await(5, TimeUnit.SECONDS));
            assertTrue(load.accept(latest));
            assertFalse(load.accept(first));
        } finally {
            releaseFirst.release();
            worker.shutdownNow();
        }
    }

    private static final class ManualScheduler implements TaskScheduler {
        private final ArrayDeque<Runnable> pending = new ArrayDeque<>();
        @Override public void post(Runnable runnable) { runnable.run(); }
        @Override public void postDelayed(Runnable runnable, long delayMs) { pending.add(runnable); }
        @Override public void removeCallbacks(Runnable runnable) { pending.remove(runnable); }
    }
}
