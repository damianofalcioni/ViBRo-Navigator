package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationMotionState;
import vibro.navigator.nav.model.NavTripStatus;
import vibro.navigator.nav.power.NavigationBatterySnapshot;

final class NavigationTripStatsTracker {
    private static final float STATIONARY_SPEED_THRESHOLD_MPS = 0.35f;
    private static final long NO_ACCEPTED_FIX = -1L;
    private static final long NO_SCREEN_TRANSITION = -1L;

    private boolean started;
    private long startedAtElapsedMs;
    private long lastAcceptedFixElapsedMs = NO_ACCEPTED_FIX;
    private long lastScreenTransitionElapsedMs = NO_SCREEN_TRANSITION;
    private final NavigationTripMotionStats motionStats = new NavigationTripMotionStats();
    private long screenOnDurationMs;
    private long screenOffDurationMs;
    private float maxSpeedMps = Float.NaN;
    private int acceptedFixCount;
    private boolean screenInteractive = true;
    @NonNull
    private final NavigationTripBatteryStats batteryStats = new NavigationTripBatteryStats();

    void reset() {
        started = false;
        startedAtElapsedMs = 0L;
        lastAcceptedFixElapsedMs = NO_ACCEPTED_FIX;
        lastScreenTransitionElapsedMs = NO_SCREEN_TRANSITION;
        motionStats.reset();
        screenOnDurationMs = 0L;
        screenOffDurationMs = 0L;
        maxSpeedMps = Float.NaN;
        acceptedFixCount = 0;
        screenInteractive = true;
        batteryStats.reset();
    }

    void start(long nowElapsedMs) {
        start(nowElapsedMs, true, NavigationBatterySnapshot.unavailable());
    }

    void start(
            long nowElapsedMs,
            boolean screenInteractive,
            @NonNull NavigationBatterySnapshot batterySnapshot
    ) {
        reset();
        started = true;
        startedAtElapsedMs = nowElapsedMs;
        lastScreenTransitionElapsedMs = nowElapsedMs;
        this.screenInteractive = screenInteractive;
        batteryStats.start(batterySnapshot);
    }

    void recordAcceptedLocation(
            @NonNull NavigationLocation location,
            long nowElapsedMs,
            float displaySpeedMps,
            boolean likelyStationary,
            boolean startNewSegment
    ) {
        recordAcceptedLocation(location, nowElapsedMs, displaySpeedMps, likelyStationary, startNewSegment, -1L);
    }

    void recordAcceptedLocation(NavigationLocation location, long nowElapsedMs, float displaySpeedMps,
            boolean likelyStationary, boolean startNewSegment, long stationarySleepEndedAtMs) {
        recordAcceptedLocation(location, nowElapsedMs, displaySpeedMps, isStationary(displaySpeedMps, likelyStationary),
                startNewSegment, stationarySleepEndedAtMs, true);
    }

    void recordAcceptedLocation(NavigationLocation location, long nowElapsedMs, float displaySpeedMps,
            boolean likelyStationary, boolean startNewSegment, long stationarySleepEndedAtMs, boolean motionKnown) {
        if (!started) {
            return;
        }
        motionStats.record(location, nowElapsedMs, likelyStationary, startNewSegment,
                stationarySleepEndedAtMs, motionKnown);
        if (location.getElapsedRealtimeMs() < 0
                || NavigationMotionState.isFresh(location.getElapsedRealtimeMs(), nowElapsedMs)) {
            maxSpeedMps = maxSpeed(maxSpeedMps, displaySpeedMps);
        }
        acceptedFixCount++;
        lastAcceptedFixElapsedMs = nowElapsedMs;
    }

    void recordScreenInteractive(boolean interactive, long nowElapsedMs) {
        if (!started || screenInteractive == interactive) {
            return;
        }
        recordOpenScreenInterval(nowElapsedMs);
        screenInteractive = interactive;
        lastScreenTransitionElapsedMs = nowElapsedMs;
    }

    void recordBatterySnapshot(@NonNull NavigationBatterySnapshot batterySnapshot) {
        if (started) {
            batteryStats.recordSnapshot(batterySnapshot);
        }
    }

    @NonNull
    NavTripStatus snapshot() {
        if (!started) {
            return NavTripStatus.unavailable();
        }
        return new NavTripStatus(
                true,
                startedAtElapsedMs,
                lastAcceptedFixElapsedMs,
                motionStats.distanceMeters(),
                motionStats.movingMs(),
                motionStats.stationaryMs(),
                maxSpeedMps,
                acceptedFixCount,
                motionStats.moving(),
                motionStats.stationary(),
                motionStats.expiresAtMs(),
                screenOnDurationMs,
                screenOffDurationMs,
                lastScreenTransitionElapsedMs,
                screenInteractive,
                batteryStats.batteryUsedMilliAmpHours(),
                batteryStats.batteryDropPercent()
        );
    }

    private void recordOpenScreenInterval(long nowElapsedMs) {
        long intervalMs = screenIntervalMs(nowElapsedMs);
        if (screenInteractive) {
            screenOnDurationMs += intervalMs;
        } else {
            screenOffDurationMs += intervalMs;
        }
    }

    private static boolean isStationary(float displaySpeedMps, boolean likelyStationary) {
        return likelyStationary
                || !Float.isFinite(displaySpeedMps)
                || displaySpeedMps <= STATIONARY_SPEED_THRESHOLD_MPS;
    }

    private static float maxSpeed(float currentMaxSpeedMps, float displaySpeedMps) {
        if (!Float.isFinite(displaySpeedMps) || displaySpeedMps < 0f) {
            return currentMaxSpeedMps;
        }
        return Float.isFinite(currentMaxSpeedMps)
                ? Math.max(currentMaxSpeedMps, displaySpeedMps)
                : displaySpeedMps;
    }

    private long screenIntervalMs(long nowElapsedMs) {
        if (lastScreenTransitionElapsedMs == NO_SCREEN_TRANSITION) {
            return 0L;
        }
        return Math.max(0L, nowElapsedMs - lastScreenTransitionElapsedMs);
    }

}
