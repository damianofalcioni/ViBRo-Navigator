package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavTripStatus;

final class NavigationTripStatsTracker {
    private static final float STATIONARY_SPEED_THRESHOLD_MPS = 0.35f;
    private static final long NO_ACCEPTED_FIX = -1L;

    private boolean started;
    private long startedAtElapsedMs;
    private long lastAcceptedFixElapsedMs = NO_ACCEPTED_FIX;
    @Nullable
    private NavigationLocation lastLocation;
    private double travelledDistanceMeters;
    private long movingDurationMs;
    private long stationaryDurationMs;
    private float maxSpeedMps = Float.NaN;
    private int acceptedFixCount;
    private boolean activeMovingInterval;
    private boolean activeStationaryInterval;

    void reset() {
        started = false;
        startedAtElapsedMs = 0L;
        lastAcceptedFixElapsedMs = NO_ACCEPTED_FIX;
        lastLocation = null;
        travelledDistanceMeters = 0.0;
        movingDurationMs = 0L;
        stationaryDurationMs = 0L;
        maxSpeedMps = Float.NaN;
        acceptedFixCount = 0;
        activeMovingInterval = false;
        activeStationaryInterval = false;
    }

    void start(long nowElapsedMs) {
        reset();
        started = true;
        startedAtElapsedMs = nowElapsedMs;
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
                activeStationaryInterval
        );
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
}
