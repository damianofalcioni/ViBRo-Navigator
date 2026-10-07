package vibro.navigator.nav.time;

/** Keeps calendar time for arrival labels separate from monotonic display policy time. */
public final class NavigationDisplayTime {
    public final long wallTimeMs;
    public final long elapsedRealtimeMs;

    public NavigationDisplayTime(long wallTimeMs, long elapsedRealtimeMs) {
        this.wallTimeMs = wallTimeMs;
        this.elapsedRealtimeMs = elapsedRealtimeMs;
    }
}
