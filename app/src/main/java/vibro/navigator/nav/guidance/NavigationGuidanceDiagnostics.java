package vibro.navigator.nav.guidance;

import vibro.navigator.logging.AppLogger;

/** State-change diagnostics reuse location callbacks and never schedule a wake-up. */
final class NavigationGuidanceDiagnostics {
    private long lastAtMs = -1;
    private String lastKey;

    static String limitingIntervalReason(String routeReason, long routeIntervalMs, long qualityCapMs, long recoveredCapMs) {
        if (recoveredCapMs >= routeIntervalMs) {
            return routeReason;
        }
        return recoveredCapMs < qualityCapMs ? "quality_recovery" : "quality_margin";
    }

    void reset() {
        lastAtMs = -1;
        lastKey = null;
    }

    String record(NavigationGuidanceDecision decision, long intervalMs, String intervalReason,
            boolean trusted, int events, long nowMs) {
        String key = decision.hintTrackIndex + ":" + decision.command + ":" + decision.reason
                + ":" + intervalMs + ":" + intervalReason + ":" + trusted;
        if (key.equals(lastKey) && events == 0 && nowMs >= lastAtMs && nowMs - lastAtMs < 30_000L) {
            return null;
        }
        lastAtMs = nowMs;
        lastKey = key;
        String record = "Guidance evaluation hintTrackIndex=" + decision.hintTrackIndex + " command=" + decision.command
                + " remainingDistanceMeters=" + decision.distanceMeters + " timeToHintSeconds=" + decision.timeSeconds
                + " suggestedIntervalMs=" + intervalMs + " intervalReason=" + intervalReason
                + " trustworthyProgress=" + trusted + " events=" + events
                + " decision=" + decision.reasonText() + " nowElapsedMs=" + nowMs;
        AppLogger.d("NavGuidance", record);
        return record;
    }
}
