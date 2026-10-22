package vibro.navigator.nav.session;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import vibro.navigator.logging.AppLogger;

/** Aggregate quality counters alongside detailed logs, without another timer or wake-up. */
final class NavigationLocationDiagnostics {
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private long lastSummaryMs = -1L;
    private String lastAcceptedProvider;

    void reset() {
        report();
        counts.clear();
        lastSummaryMs = -1L;
        lastAcceptedProvider = null;
    }

    void incoming(String provider, long nowMs) {
        increment("callbacks_" + provider);
        if (lastSummaryMs < 0L) {
            lastSummaryMs = nowMs;
        }
        if (nowMs - lastSummaryMs >= 60_000L) {
            report();
            lastSummaryMs = nowMs;
        }
    }

    void dropped(boolean corrected) {
        increment("dropped");
        if (corrected) {
            increment("delayed_position_corrections");
        }
    }

    void accepted(String provider) {
        increment("accepted_" + provider);
        if (lastAcceptedProvider != null && !Objects.equals(lastAcceptedProvider, provider)) {
            increment("provider_switches");
        }
        lastAcceptedProvider = provider;
    }

    private void increment(String key) {
        Integer previous = counts.get(key);
        counts.put(key, (previous == null ? 0 : previous) + 1);
    }

    private void report() {
        if (AppLogger.isLoggingEnabled() && !counts.isEmpty()) {
            AppLogger.d("NavSessionLocation", "Location quality summary cumulative=" + counts);
        }
    }
}
