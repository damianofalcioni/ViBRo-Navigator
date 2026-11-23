package vibro.navigator.brouter;

import android.net.Uri;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import vibro.navigator.logging.AppLogger;

/** Bounds waits for a document provider and gives a timed-out source time to recover. */
final class BRouterStreetProviderRead {
    private static final long TIMEOUT_MS = 750L;
    private static final long RETRY_MS = 5_000L;
    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(
            0, 4, 30, TimeUnit.SECONDS, new SynchronousQueue<>());
    private final HashMap<Uri, Long> unavailable = new HashMap<>();
    private boolean failed;

    void startLoad() {
        failed = false;
        long now = nowMs();
        var retries = unavailable.values().iterator();
        while (retries.hasNext()) {
            if (retries.next() <= now) {
                retries.remove();
            }
        }
    }

    boolean failed() {
        return failed;
    }

    void recordFailure() {
        failed = true;
    }

    <T> T run(Uri source, Callable<T> attempt) {
        BRouterStreetReadCancellation.check();
        if (unavailable.containsKey(source)) {
            recordFailure();
            return null;
        }
        try {
            return await(source, attempt);
        } catch (RejectedExecutionException | ExecutionException e) {
            recordFailure();
            AppLogger.d("BRouterSegments", "Unavailable street provider source=" + source);
            return null;
        }
    }

    private <T> T await(Uri source, Callable<T> attempt) throws ExecutionException {
        var task = WORKERS.submit(attempt);
        try {
            return task.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            unavailable.put(source, nowMs() + RETRY_MS);
            recordFailure();
            task.cancel(true);
            return null;
        } catch (InterruptedException e) {
            task.cancel(true);
            Thread.currentThread().interrupt();
            BRouterStreetReadCancellation.check();
            return null;
        }
    }

    private static long nowMs() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
}
