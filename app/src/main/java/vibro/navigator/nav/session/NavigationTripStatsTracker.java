package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.nav.location.NavigationLocation;
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
    @Nullable
    private NavigationLocation lastLocation;
    private double travelledDistanceMeters;
    private long movingDurationMs;
    private long stationaryDurationMs;
    private long screenOnDurationMs;
    private long screenOffDurationMs;
    private float maxSpeedMps = Float.NaN;
    private int acceptedFixCount;
    private boolean activeMovingInterval;
    private boolean activeStationaryInterval;
    private boolean screenInteractive = true;
    private boolean chargeCounterReliable;
    private boolean levelPercentReliable;
    @NonNull
    private NavigationBatterySnapshot initialBatterySnapshot = NavigationBatterySnapshot.unavailable();
    @NonNull
    private NavigationBatterySnapshot latestBatterySnapshot = NavigationBatterySnapshot.unavailable();

    void reset() {
        started = false;
        startedAtElapsedMs = 0L;
        lastAcceptedFixElapsedMs = NO_ACCEPTED_FIX;
        lastScreenTransitionElapsedMs = NO_SCREEN_TRANSITION;
        lastLocation = null;
        travelledDistanceMeters = 0.0;
        movingDurationMs = 0L;
        stationaryDurationMs = 0L;
        screenOnDurationMs = 0L;
        screenOffDurationMs = 0L;
        maxSpeedMps = Float.NaN;
        acceptedFixCount = 0;
        activeMovingInterval = false;
        activeStationaryInterval = false;
        screenInteractive = true;
        chargeCounterReliable = false;
        levelPercentReliable = false;
        initialBatterySnapshot = NavigationBatterySnapshot.unavailable();
        latestBatterySnapshot = NavigationBatterySnapshot.unavailable();
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
        initialBatterySnapshot = batterySnapshot;
        latestBatterySnapshot = batterySnapshot;
        chargeCounterReliable = batterySnapshot.hasChargeCounter();
        levelPercentReliable = batterySnapshot.hasLevelPercent();
    }

    void recordAcceptedLocation(
            @NonNull NavigationLocation location,
            long nowElapsedMs,
            float displaySpeedMps,
            boolean likelyStationary,
            boolean startNewSegment
    ) {
        if (!started) {
            return;
        }
        boolean stationary = isStationary(displaySpeedMps, likelyStationary);
        long intervalMs = acceptedIntervalMs(nowElapsedMs, startNewSegment);
        if (stationary) {
            stationaryDurationMs += intervalMs;
        } else {
            movingDurationMs += intervalMs;
            travelledDistanceMeters += travelledDistanceMeters(location, startNewSegment);
        }
        maxSpeedMps = maxSpeed(maxSpeedMps, displaySpeedMps);
        acceptedFixCount++;
        lastAcceptedFixElapsedMs = nowElapsedMs;
        lastLocation = new NavigationLocation(location);
        activeMovingInterval = !stationary;
        activeStationaryInterval = stationary;
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
            chargeCounterReliable = chargeCounterReliable && batterySnapshot.hasChargeCounter();
            levelPercentReliable = levelPercentReliable && batterySnapshot.hasLevelPercent();
            latestBatterySnapshot = batterySnapshot;
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
                travelledDistanceMeters,
                movingDurationMs,
                stationaryDurationMs,
                maxSpeedMps,
                acceptedFixCount,
                activeMovingInterval,
                activeStationaryInterval,
                screenOnDurationMs,
                screenOffDurationMs,
                lastScreenTransitionElapsedMs,
                screenInteractive,
                batteryUsedMilliAmpHours(),
                batteryDropPercent()
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

    private long acceptedIntervalMs(long nowElapsedMs, boolean startNewSegment) {
        if (startNewSegment || lastAcceptedFixElapsedMs == NO_ACCEPTED_FIX) {
            return 0L;
        }
        return Math.max(0L, nowElapsedMs - lastAcceptedFixElapsedMs);
    }

    private double travelledDistanceMeters(@NonNull NavigationLocation location, boolean startNewSegment) {
        if (startNewSegment || lastLocation == null) {
            return 0.0;
        }
        double distanceMeters = GeoMath.distanceMeters(
                lastLocation.getLatitude(),
                lastLocation.getLongitude(),
                location.getLatitude(),
                location.getLongitude()
        );
        return Double.isFinite(distanceMeters) ? Math.max(0.0, distanceMeters) : 0.0;
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

    private float batteryUsedMilliAmpHours() {
        if (!chargeCounterReliable || !initialBatterySnapshot.hasChargeCounter()
                || !latestBatterySnapshot.hasChargeCounter()) {
            return Float.NaN;
        }
        long usedMicroAmpHours = (long) initialBatterySnapshot.chargeCounterMicroAmpHours
                - latestBatterySnapshot.chargeCounterMicroAmpHours;
        return usedMicroAmpHours >= 0L ? usedMicroAmpHours / 1000f : Float.NaN;
    }

    private int batteryDropPercent() {
        if (!levelPercentReliable || !initialBatterySnapshot.hasLevelPercent()
                || !latestBatterySnapshot.hasLevelPercent()) {
            return NavTripStatus.UNKNOWN_BATTERY_DROP_PERCENT;
        }
        int drop = initialBatterySnapshot.levelPercent - latestBatterySnapshot.levelPercent;
        return drop >= 0 ? drop : NavTripStatus.UNKNOWN_BATTERY_DROP_PERCENT;
    }
}
