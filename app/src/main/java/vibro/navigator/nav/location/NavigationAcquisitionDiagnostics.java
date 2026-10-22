package vibro.navigator.nav.location;

import java.util.LinkedHashMap;
import java.util.Map;

import vibro.navigator.logging.AppLogger;

/** Requested subscription durations do not imply actual receiver hardware on-time. */
final class NavigationAcquisitionDiagnostics {
    private final Map<String, Long> requestedDurationMs = new LinkedHashMap<>();
    private String summary;
    private long gpsIntervalMs;
    private long networkIntervalMs;
    private long sinceMs;
    private int changes;

    void requested(String providers, long intervalMs, long networkMinTimeMs, long nowMs) {
        if (providers.equals(summary) && intervalMs == gpsIntervalMs && networkMinTimeMs == networkIntervalMs) {
            return;
        }
        accumulate(nowMs);
        summary = providers;
        gpsIntervalMs = intervalMs;
        networkIntervalMs = networkMinTimeMs;
        sinceMs = nowMs;
        changes++;
    }

    void stop(long nowMs) {
        accumulate(nowMs);
        if (AppLogger.isLoggingEnabled() && summary != null) {
            AppLogger.i("NavLocation", "Acquisition summary requestChanges=" + changes
                    + " requestedSubscriptionDurationMs=" + requestedDurationMs);
        }
        summary = null;
        requestedDurationMs.clear();
        changes = 0;
    }

    private void accumulate(long nowMs) {
        if (summary == null) {
            return;
        }
        long durationMs = Math.max(0L, nowMs - sinceMs);
        for (String provider : summary.split("\\+")) {
            long interval = NavigationLocationProviders.NETWORK_PROVIDER.equals(provider) ? networkIntervalMs : gpsIntervalMs;
            String key = provider + "@" + interval;
            Long previous = requestedDurationMs.get(key);
            requestedDurationMs.put(key, (previous == null ? 0L : previous) + durationMs);
        }
    }
}
