package vibro.navigator.nav.streets;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;

final class SurroundingStreetLoadExecutor {
    // Reserve room for a current load when two obsolete reads are still in provider calls.
    private static final int MAX_ACTIVE_SOURCES = 3;
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_ACTIVE_SOURCES);
    private final Map<Integer, Work> running = new HashMap<>();
    private int source;
    private Runnable pending;
    private boolean shutdown;

    synchronized void execute(Runnable runnable) {
        pending = runnable;
        startPending();
    }

    synchronized void cancel() {
        pending = null;
        Work current = running.get(source);
        if (current != null) {
            current.interrupt();
        }
    }

    synchronized void changeSource() {
        cancel();
        source++;
    }

    synchronized void shutdown() {
        shutdown = true;
        pending = null;
        for (Work work : running.values()) {
            work.interrupt();
        }
        executor.shutdownNow();
    }

    private void startPending() {
        if (shutdown || pending == null || running.size() >= MAX_ACTIVE_SOURCES || running.containsKey(source)) {
            return;
        }
        Work work = new Work(source, pending);
        pending = null;
        running.put(source, work);
        executor.execute(() -> run(work));
    }

    private void run(Work work) {
        try {
            synchronized (this) {
                if (work.cancelled) {
                    return;
                }
                work.thread = Thread.currentThread();
            }
            work.action.run();
        } finally {
            synchronized (this) {
                running.remove(work.source);
                startPending();
            }
        }
    }

    private static final class Work {
        private final int source;
        private final Runnable action;
        private boolean cancelled;
        private Thread thread;

        Work(int source, Runnable action) {
            this.source = source;
            this.action = action;
        }

        void interrupt() {
            cancelled = true;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }
}
