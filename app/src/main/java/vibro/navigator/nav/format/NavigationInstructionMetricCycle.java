package vibro.navigator.nav.format;

/** Alternates metrics without resetting when the live distance or time changes. */
public final class NavigationInstructionMetricCycle {
    public static final long INTERVAL_MS = 3000;

    private String instructionKey = "";
    private long startedAtMs = -1;

    public String prefix(NavigationInstructionParts parts, long nowMs) {
        String key = parts.symbol + parts.instruction;
        if (startedAtMs < 0 || !instructionKey.equals(key)) {
            instructionKey = key;
            startedAtMs = nowMs;
        }
        boolean showTime = parts.alternatesMetrics() && (elapsed(nowMs) / INTERVAL_MS) % 2 == 1;
        return showTime ? parts.timePrefix : parts.distancePrefix;
    }

    public long nextSwitchDelayMs(long nowMs) {
        return INTERVAL_MS - elapsed(nowMs) % INTERVAL_MS;
    }

    public void reset() {
        startedAtMs = -1;
    }

    private long elapsed(long nowMs) {
        return Math.max(0L, nowMs - startedAtMs);
    }
}
