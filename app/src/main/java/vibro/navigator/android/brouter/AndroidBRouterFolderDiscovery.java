package vibro.navigator.android.brouter;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Supplier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;

final class AndroidBRouterFolderDiscovery {
    private static final long TIMEOUT_MS = 1000;
    private static final ExecutorService WORKER = Executors.newFixedThreadPool(2);

    private AndroidBRouterFolderDiscovery() {
    }

    static <T> void find(Supplier<T> work, @Nullable T fallback, Consumer<T> onResult) {
        TaskScheduler main = AndroidTaskScheduler.main();
        AtomicBoolean delivered = new AtomicBoolean();
        Runnable timeout = () -> deliver(delivered, onResult, fallback);
        main.postDelayed(timeout, TIMEOUT_MS);
        execute(() -> {
            if (delivered.get()) {
                return;
            }
            T result = resolve(work, fallback);
            postResult(() -> {
                main.removeCallbacks(timeout);
                deliver(delivered, onResult, result);
            });
        });
    }

    private static <T> T resolve(Supplier<T> work, @Nullable T fallback) {
        try {
            return work.get();
        } catch (RuntimeException e) {
            AppLogger.w("BRouterFolderDiscovery", "Unable to inspect BRouter folders", e);
            return fallback;
        }
    }

    private static <T> void deliver(AtomicBoolean delivered, Consumer<T> onResult, @Nullable T result) {
        if (delivered.compareAndSet(false, true)) {
            onResult.accept(result);
        }
    }

    static void execute(Runnable work) {
        WORKER.execute(work);
    }

    static void postResult(Runnable callback) {
        AndroidTaskScheduler.main().post(callback);
    }
}
