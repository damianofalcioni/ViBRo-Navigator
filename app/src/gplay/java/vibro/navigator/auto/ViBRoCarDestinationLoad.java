package vibro.navigator.auto;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

/** Main-thread attempt lifecycle; a stalled worker must never hold the host UI in loading. */
final class ViBRoCarDestinationLoad {
    // DR-3 allows ten seconds. Leave one second for host delivery/rendering.
    static final long TIMEOUT_MS = 9_000L;
    private static final int MAX_CONCURRENT_ATTEMPTS = 4;
    private final TaskScheduler scheduler;
    private final ElapsedRealtimeClock clock;
    private final Runnable expired;
    private Future<?> pending;
    private Runnable timeout;
    private long deadlineMs;
    private int generation;
    private boolean loading;

    ViBRoCarDestinationLoad(TaskScheduler scheduler, ElapsedRealtimeClock clock, Runnable expired) {
        this.scheduler = scheduler;
        this.clock = clock;
        this.expired = expired;
    }

    static ExecutorService newWorker() {
        // Blocking provider calls may outlive cancellation. Reserve capacity for replacements without queuing them.
        return new ThreadPoolExecutor(0, MAX_CONCURRENT_ATTEMPTS, 30, TimeUnit.SECONDS, new SynchronousQueue<>());
    }

    int begin() {
        cancel();
        loading = true;
        deadlineMs = clock.elapsedRealtimeMs() + TIMEOUT_MS;
        int token = generation;
        timeout = () -> expire(token);
        scheduler.postDelayed(timeout, TIMEOUT_MS);
        return token;
    }

    void submit(int token, ExecutorService worker, Runnable task) {
        try {
            submitted(token, worker.submit(task));
        } catch (RejectedExecutionException e) {
            // All slots are still busy. Leave loading immediately and keep the request available for retry.
            expire(token);
        }
    }

    void submitted(int token, Future<?> future) {
        if (token == generation && loading) { pending = future; }
        else { future.cancel(true); }
    }

    boolean accept(int token) {
        if (token != generation || !loading) { return false; }
        refresh();
        return loading;
    }

    void refresh() {
        if (loading && clock.elapsedRealtimeMs() >= deadlineMs) { expire(generation); }
    }

    boolean isLoading() { return loading; }

    int revision() { return generation; }

    boolean matches(int revision) { return revision == generation; }

    void finish() {
        if (timeout != null) { scheduler.removeCallbacks(timeout); }
        timeout = null;
        pending = null;
        loading = false;
    }

    void cancel() {
        generation++;
        if (pending != null) { pending.cancel(true); }
        finish();
    }

    private void expire(int token) {
        if (token != generation || !loading) { return; }
        cancel();
        expired.run();
    }
}
