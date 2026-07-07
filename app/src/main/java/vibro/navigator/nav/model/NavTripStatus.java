package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

public final class NavTripStatus {
    private static final long NO_ACCEPTED_FIX = -1L;

    public final boolean started;
    public final long startedAtElapsedMs;
    public final long lastAcceptedFixElapsedMs;
    public final double travelledDistanceMeters;
    public final long movingDurationMs;
    public final long stationaryDurationMs;
    public final float maxSpeedMps;
    public final int acceptedFixCount;
    public final boolean activeMovingInterval;
    public final boolean activeStationaryInterval;

    public NavTripStatus(
            boolean started,
            long startedAtElapsedMs,
            long lastAcceptedFixElapsedMs,
            double travelledDistanceMeters,
            long movingDurationMs,
            long stationaryDurationMs,
            float maxSpeedMps,
            int acceptedFixCount,
            boolean activeMovingInterval,
            boolean activeStationaryInterval
    ) {
        this.started = started;
        this.startedAtElapsedMs = startedAtElapsedMs;
        this.lastAcceptedFixElapsedMs = lastAcceptedFixElapsedMs;
        this.travelledDistanceMeters = Math.max(0.0, travelledDistanceMeters);
        this.movingDurationMs = Math.max(0L, movingDurationMs);
        this.stationaryDurationMs = Math.max(0L, stationaryDurationMs);
        this.maxSpeedMps = maxSpeedMps;
        this.acceptedFixCount = Math.max(0, acceptedFixCount);
        this.activeMovingInterval = activeMovingInterval;
        this.activeStationaryInterval = activeStationaryInterval;
    }

    @NonNull
    public static NavTripStatus unavailable() {
        return new NavTripStatus(false, 0L, NO_ACCEPTED_FIX, 0.0, 0L, 0L, Float.NaN, 0, false, false);
    }

    public long elapsedDurationMs(long nowElapsedMs) {
        if (!started) {
            return 0L;
        }
        return Math.max(0L, nowElapsedMs - startedAtElapsedMs);
    }

    public long movingDurationMs(long nowElapsedMs) {
        return movingDurationMs + (activeMovingInterval ? openAcceptedFixIntervalMs(nowElapsedMs) : 0L);
    }

    public long stationaryDurationMs(long nowElapsedMs) {
        return stationaryDurationMs + (activeStationaryInterval ? openAcceptedFixIntervalMs(nowElapsedMs) : 0L);
    }

    private long openAcceptedFixIntervalMs(long nowElapsedMs) {
        if (!started || lastAcceptedFixElapsedMs == NO_ACCEPTED_FIX) {
            return 0L;
        }
        return Math.max(0L, nowElapsedMs - lastAcceptedFixElapsedMs);
    }
}
