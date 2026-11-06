package vibro.navigator.logging;

import java.util.concurrent.atomic.AtomicLong;

/** Callback-scoped correlation; never leaks a fix identity to asynchronous or later work. */
public final class NavigationEvaluationTrace {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private NavigationEvaluationTrace() { }

    public static String suffix() {
        String id = CURRENT.get();
        return id == null ? "" : " traceId=" + id;
    }

    public static void run(String provider, long measurementMs, Runnable work) {
        if (!AppLogger.isLoggingEnabled()) {
            work.run();
            return;
        }
        String previous = CURRENT.get();
        CURRENT.set(provider + ":" + measurementMs + ":" + SEQUENCE.incrementAndGet());
        try {
            work.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
