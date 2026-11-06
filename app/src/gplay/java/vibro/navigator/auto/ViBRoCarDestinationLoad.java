package vibro.navigator.auto;

import java.util.concurrent.Future;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

/** Main-thread attempt lifecycle; a stalled worker must never hold the host UI in loading. */
final class ViBRoCarDestinationLoad {
    // DR-3 allows ten seconds. Leave one second for host delivery/rendering.
    static final long TIMEOUT_MS = 9_000L;
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

    int begin() {
        cancel();
        loading = true;
        deadlineMs = clock.elapsedRealtimeMs() + TIMEOUT_MS;
        int token = generation;
        timeout = () -> expire(token);
        scheduler.postDelayed(timeout, TIMEOUT_MS);
        return token;
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
