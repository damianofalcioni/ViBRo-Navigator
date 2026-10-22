package vibro.navigator.nav.location;

import java.util.List;

import vibro.navigator.nav.time.ElapsedRealtimeClock;

/** Legacy acquisition health is independent of fused-stream and navigation-dispatch health. */
public final class NavigationLocationAcquisition {
    interface IntervalRequester {
        void request(long intervalMs);
    }
    private final NavigationLocationProvider providers;
    private final ElapsedRealtimeClock clock;
    private final IntervalRequester intervalRequester;
    private final LegacyLocationAcquisitionPolicy policy = new LegacyLocationAcquisitionPolicy();
    private final NavigationAcquisitionDiagnostics diagnostics = new NavigationAcquisitionDiagnostics();
    private Runnable stateListener = () -> { };
    private String activeProviders;
    private long gpsIntervalMs = -1L;
    private long networkIntervalMs = -1L;

    NavigationLocationAcquisition(NavigationLocationProvider providers, ElapsedRealtimeClock clock,
            IntervalRequester intervalRequester) {
        this.providers = providers;
        this.clock = clock;
        this.intervalRequester = intervalRequester;
    }

    void reset() {
        diagnostics.stop(clock.elapsedRealtimeMs());
        policy.reset();
        activeProviders = null;
        gpsIntervalMs = -1L;
        networkIntervalMs = -1L;
    }

    long intervalForRequest(boolean preferFused, long intervalMs) {
        List<String> enabled = providers.enabledPermittedProviders(
                providers.hasFineLocationPermission(), providers.hasCoarseLocationPermission());
        if (preferFused || !enabled.contains(NavigationLocationProviders.GPS_PROVIDER)) {
            policy.reset();
        }
        return policy.networkIntervalMs(clock.elapsedRealtimeMs(), intervalMs);
    }

    void requested(String summary, long intervalMs, long networkMinTimeMs) {
        activeProviders = summary;
        gpsIntervalMs = intervalMs;
        networkIntervalMs = networkMinTimeMs;
        diagnostics.requested(summary, intervalMs, networkMinTimeMs, clock.elapsedRealtimeMs());
        stateListener.run();
    }

    long lastNetworkIntervalMs() {
        return networkIntervalMs;
    }

    public void setStateListener(Runnable listener) {
        stateListener = listener;
    }

    public void recordRawLocation(NavigationLocation location) {
        if (!isLegacyGpsActive()) {
            return;
        }
        policy.record(location, clock.elapsedRealtimeMs(), gpsIntervalMs);
        checkHealth();
    }

    public void checkHealth() {
        if (!isLegacyGpsActive()) {
            return;
        }
        long intervalMs = policy.networkIntervalMs(clock.elapsedRealtimeMs(), gpsIntervalMs);
        if (intervalMs != networkIntervalMs) {
            intervalRequester.request(gpsIntervalMs);
        }
        stateListener.run();
    }

    public long recoveryDeadlineMs() {
        // No extra alarm when network is absent or already running at the GNSS cadence.
        return isLegacyGpsActive() && activeProviders.contains(NavigationLocationProviders.NETWORK_PROVIDER)
                && networkIntervalMs > gpsIntervalMs ? policy.recoveryDeadlineMs(gpsIntervalMs) : -1L;
    }

    private boolean isLegacyGpsActive() {
        return activeProviders != null && activeProviders.contains(NavigationLocationProviders.GPS_PROVIDER);
    }
}
