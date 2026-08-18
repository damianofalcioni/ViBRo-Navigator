package vibro.navigator.nav.power;

import androidx.annotation.NonNull;

import vibro.navigator.nav.time.ElapsedRealtimeClock;

/**
 * Retains the latest battery reading while allowing an adapter to recover if change broadcasts stop.
 */
public final class NavigationBatterySnapshotCache {
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    private final long maximumAgeMs;
    @NonNull
    private NavigationBatterySnapshot snapshot = NavigationBatterySnapshot.unavailable();
    private long refreshedAtElapsedMs;
    private boolean initialized;

    public NavigationBatterySnapshotCache(
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock,
            long maximumAgeMs
    ) {
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        this.maximumAgeMs = Math.max(1L, maximumAgeMs);
    }

    public synchronized boolean needsRefresh() {
        if (!initialized) {
            return true;
        }
        long nowElapsedMs = elapsedRealtimeClock.elapsedRealtimeMs();
        return nowElapsedMs < refreshedAtElapsedMs
                || nowElapsedMs - refreshedAtElapsedMs >= maximumAgeMs;
    }

    public synchronized void record(@NonNull NavigationBatterySnapshot snapshot) {
        this.snapshot = snapshot;
        refreshedAtElapsedMs = elapsedRealtimeClock.elapsedRealtimeMs();
        initialized = true;
    }

    @NonNull
    public synchronized NavigationBatterySnapshot current() {
        return snapshot;
    }
}
