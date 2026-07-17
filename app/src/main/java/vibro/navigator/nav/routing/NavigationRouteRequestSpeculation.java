package vibro.navigator.nav.routing;

final class NavigationRouteRequestSpeculation {
    private boolean activeRequestSpeculative;
    private boolean deferredThrottleActive;
    private boolean hasThrottleBaseline;
    private long lastRerouteMsBeforeSpeculative;

    void reset() {
        activeRequestSpeculative = false;
        deferredThrottleActive = false;
        hasThrottleBaseline = false;
        lastRerouteMsBeforeSpeculative = 0L;
    }

    CancelResult cancel(boolean routeCalculationInProgress, long lastRerouteMs) {
        if (!deferredThrottleActive && (!routeCalculationInProgress || !activeRequestSpeculative)) {
            return new CancelResult(false, 0, lastRerouteMs);
        }
        int requestTokenIncrement = routeCalculationInProgress && activeRequestSpeculative ? 1 : 0;
        long restored = restoreThrottle(lastRerouteMs);
        reset();
        return new CancelResult(true, requestTokenIncrement, restored);
    }

    long restoreDeferredThrottleIfNeeded(long lastRerouteMs) {
        return deferredThrottleActive ? cancel(false, lastRerouteMs).lastRerouteMs : lastRerouteMs;
    }

    void onRequestStarted(boolean speculative, long lastRerouteMs) {
        reset();
        activeRequestSpeculative = speculative;
        if (speculative) {
            hasThrottleBaseline = true;
            lastRerouteMsBeforeSpeculative = lastRerouteMs;
        }
    }

    void onRequestCompleted() {
        activeRequestSpeculative = false;
        deferredThrottleActive = false;
        hasThrottleBaseline = false;
        lastRerouteMsBeforeSpeculative = 0L;
    }

    long onSpeculativeRequestFinished(boolean speculative, boolean deferred, long lastRerouteMs) {
        activeRequestSpeculative = false;
        if (!speculative) {
            return lastRerouteMs;
        }
        if (deferred) {
            deferredThrottleActive = true;
            return lastRerouteMs;
        }
        long restored = restoreThrottle(lastRerouteMs);
        reset();
        return restored;
    }

    void onDeferredSpeculativeRouteApplied() {
        deferredThrottleActive = false;
        hasThrottleBaseline = false;
        lastRerouteMsBeforeSpeculative = 0L;
    }

    private long restoreThrottle(long lastRerouteMs) {
        return hasThrottleBaseline ? lastRerouteMsBeforeSpeculative : lastRerouteMs;
    }

    static final class CancelResult {
        final boolean canceled;
        final int requestTokenIncrement;
        final long lastRerouteMs;

        CancelResult(boolean canceled, int requestTokenIncrement, long lastRerouteMs) {
            this.canceled = canceled;
            this.requestTokenIncrement = requestTokenIncrement;
            this.lastRerouteMs = lastRerouteMs;
        }
    }
}
