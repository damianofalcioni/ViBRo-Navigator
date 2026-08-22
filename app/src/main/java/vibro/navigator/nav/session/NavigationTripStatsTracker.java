package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
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
    private final PreviousLocation lastLocation = new PreviousLocation();
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
    @NonNull
    private final NavigationTripBatteryStats batteryStats = new NavigationTripBatteryStats();

    void reset() {
        started = false;
        startedAtElapsedMs = 0L;
        lastAcceptedFixElapsedMs = NO_ACCEPTED_FIX;
        lastScreenTransitionElapsedMs = NO_SCREEN_TRANSITION;
        lastLocation.reset();
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
        lastLocation.record(location);
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

    private long acceptedIntervalMs(long nowElapsedMs, boolean startNewSegment) {
        if (startNewSegment || lastAcceptedFixElapsedMs == NO_ACCEPTED_FIX) {
            return 0L;
        }
        return Math.max(0L, nowElapsedMs - lastAcceptedFixElapsedMs);
    }

    private double travelledDistanceMeters(@NonNull NavigationLocation location, boolean startNewSegment) {
        if (startNewSegment || !lastLocation.isAvailable()) {
            return 0.0;
        }
        double distanceMeters = GeoMath.distanceMeters(
                lastLocation.latitude,
                lastLocation.longitude,
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

    private static final class PreviousLocation {
        private double latitude = Double.NaN;
        private double longitude = Double.NaN;

        private void reset() {
            latitude = Double.NaN;
            longitude = Double.NaN;
        }

        private void record(@NonNull NavigationLocation location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }

        private boolean isAvailable() {
            return !Double.isNaN(latitude);
        }
    }
}
