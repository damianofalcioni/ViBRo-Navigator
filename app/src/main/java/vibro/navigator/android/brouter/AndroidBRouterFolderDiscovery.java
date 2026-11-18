package vibro.navigator.android.brouter;

import android.os.CancellationSignal;
import android.os.OperationCanceledException;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Supplier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private static final ExecutorService PROFILE_WORKER = new ThreadPoolExecutor(
            0, 2, 30, TimeUnit.SECONDS, new SynchronousQueue<>());

    private static final ExecutorService CHILD_WORKER = new ThreadPoolExecutor(
            0, 4, 30, TimeUnit.SECONDS, new SynchronousQueue<>());
    private static final ExecutorService BUNDLED_WORKER = Executors.newSingleThreadExecutor();

    interface Query<T> {
        T run(CancellationSignal signal);
    }

    private AndroidBRouterFolderDiscovery() {
    }

    static <T> void find(Query<T> work, @Nullable T fallback, Consumer<T> onResult) {
        findWithFallback(work, () -> fallback, onResult);
    }

    static <T> void findWithFallback(Query<T> work, Supplier<T> fallback, Consumer<T> onResult) {
        new Request<>(work, fallback, onResult).start(AndroidBRouterFolderDiscovery::execute);
    }

    static <T> void listProfiles(Query<T> work, Supplier<T> fallback, Consumer<T> onResult) {
        new Request<>(work, fallback, onResult).start(AndroidBRouterFolderDiscovery::executeProfileLookup);
    }

    static void cancelTask(FutureTask<Void> task, CancellationSignal signal) {
        if (task.cancel(true)) {
            // Cancellation can call a remote provider. Keep it off the main thread.
            new Thread(signal::cancel, "BRouter query cancellation").start();
        }
    }

    private static <T> T resolve(Query<T> work, CancellationSignal signal,
                                 Supplier<T> fallback) {
        try {
            return work.run(signal);
        } catch (OperationCanceledException e) {
            return fallback.get();
        } catch (RuntimeException e) {
            AppLogger.w("BRouterFolderDiscovery", "Unable to inspect BRouter folders", e);
            return fallback.get();
        }
    }

    private static final class Request<T> {
        private final TaskScheduler main = AndroidTaskScheduler.main();
        private final CancellationSignal signal = new CancellationSignal();
        private final Runnable timeout = this::onTimeout;
        private final FutureTask<Void> task;
        private final Supplier<T> fallback;
        private final Consumer<T> onResult;
        private boolean delivered;

        Request(Query<T> work, Supplier<T> fallback, Consumer<T> onResult) {
            this.fallback = fallback;
            this.onResult = onResult;
            task = new FutureTask<>(() -> {
                T result = resolve(work, signal, fallback);
                postResult(() -> deliver(result));
            }, null);
        }

        void start(Consumer<Runnable> submit) {
            main.postDelayed(timeout, TIMEOUT_MS);
            try {
                submit.accept(task);
            } catch (RejectedExecutionException e) {
                AppLogger.d("BRouterFolderDiscovery", "Folder discovery workers are busy");
                postResult(() -> deliver(fallback.get()));
            }
        }

        private void onTimeout() {
            cancelTask(task, signal);
            deliver(fallback.get());
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

    static boolean executeChildLookup(Runnable work) {
        try {
            CHILD_WORKER.execute(work);
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    static void executeBundledLookup(Runnable work) {
        BUNDLED_WORKER.execute(work);
    }

    static void executeProfileLookup(Runnable work) {
        PROFILE_WORKER.execute(work);
    }

    static void postResult(Runnable callback) {
        AndroidTaskScheduler.main().post(callback);
    }
}
