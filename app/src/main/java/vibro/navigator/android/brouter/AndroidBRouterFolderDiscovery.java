package vibro.navigator.android.brouter;

import android.os.CancellationSignal;
import android.os.OperationCanceledException;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;

final class AndroidBRouterFolderDiscovery {
    private static final long TIMEOUT_MS = 1000;
    private static final ExecutorService WORKER = new ThreadPoolExecutor(
            0, 8, 30, TimeUnit.SECONDS, new SynchronousQueue<>());

    interface Query<T> {
        T run(CancellationSignal signal);
    }

    private AndroidBRouterFolderDiscovery() {
    }

    static <T> void find(Query<T> work, @Nullable T fallback, Consumer<T> onResult) {
        new Request<>(work, fallback, onResult).start();
    }

    private static void cancelTask(FutureTask<Void> task, CancellationSignal signal) {
        if (task.cancel(true)) {
            // Cancellation can call a remote provider. Keep it off the main thread.
            new Thread(signal::cancel, "BRouter query cancellation").start();
        }
    }

    private static <T> T resolve(Query<T> work, CancellationSignal signal,
                                 @Nullable T fallback) {
        try {
            return work.run(signal);
        } catch (OperationCanceledException e) {
            return fallback;
        } catch (RuntimeException e) {
            AppLogger.w("BRouterFolderDiscovery", "Unable to inspect BRouter folders", e);
            return fallback;
        }
    }

    private static final class Request<T> {
        private final TaskScheduler main = AndroidTaskScheduler.main();
        private final CancellationSignal signal = new CancellationSignal();
        private final Runnable timeout = this::onTimeout;
        private final FutureTask<Void> task;
        private final T fallback;
        private final Consumer<T> onResult;
        private boolean delivered;

        Request(Query<T> work, @Nullable T fallback, Consumer<T> onResult) {
            this.fallback = fallback;
            this.onResult = onResult;
            task = new FutureTask<>(() -> {
                T result = resolve(work, signal, fallback);
                postResult(() -> deliver(result));
            }, null);
        }

        void start() {
            main.postDelayed(timeout, TIMEOUT_MS);
            try {
                execute(task);
            } catch (RejectedExecutionException e) {
                AppLogger.d("BRouterFolderDiscovery", "Folder discovery workers are busy");
                postResult(() -> deliver(fallback));
            }
        }

        private void onTimeout() {
            cancelTask(task, signal);
            deliver(fallback);
        }

        private void deliver(@Nullable T result) {
            if (!delivered) {
                delivered = true;
                main.removeCallbacks(timeout);
                onResult.accept(result);
            }
        }
    }

    static void execute(Runnable work) {
        WORKER.execute(work);
    }

    static void postResult(Runnable callback) {
        AndroidTaskScheduler.main().post(callback);
    }
}
