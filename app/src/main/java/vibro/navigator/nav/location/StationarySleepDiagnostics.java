package vibro.navigator.nav.location;

/** Report decisions on change and at most once per 30 seconds while the same condition persists. */
final class StationarySleepDiagnostics {
    interface Sink {
        void record(String message);
    }

    private final Sink sink;
    private String previousReason;
    private long lastReportMs = -1L;

    StationarySleepDiagnostics(Sink sink) {
        this.sink = sink;
    }

    void record(String reason, long nowMs, NavigationLocation raw, long stableMs, long gapMs, long gapLimitMs) {
        if (reason.equals(previousReason) && lastReportMs >= 0L && nowMs - lastReportMs < 30_000L) {
            return;
        }
        previousReason = reason;
        lastReportMs = nowMs;
        sink.record("Stationary acquisition decision reason=" + reason + " provider=" + raw.getProvider()
                + " nativeSpeed=" + (raw.hasSpeed() ? Float.toString(raw.getSpeed()) : "unknown")
                + " rawAccuracy=" + raw.getAccuracy()
                + " stableMs=" + stableMs + " gapMs=" + gapMs + " gapLimitMs=" + gapLimitMs);
    }
}
