package vibro.navigator.nav.streets;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

final class SurroundingStreetLoadExecutor {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private FutureTask<Void> task;

    void execute(Runnable runnable) {
        FutureTask<Void> next = new FutureTask<>(runnable, null);
        task = next;
        executor.execute(next);
    }

    void cancel() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }

    void shutdown() {
        cancel();
        executor.shutdownNow();
    }
}
