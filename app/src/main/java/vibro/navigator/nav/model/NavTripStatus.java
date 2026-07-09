package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

public final class NavTripStatus {
    private static final long NO_ACCEPTED_FIX = -1L;
    public static final int UNKNOWN_BATTERY_DROP_PERCENT = -1;

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
    public final long screenOnDurationMs;
    public final long screenOffDurationMs;
    public final long lastScreenTransitionElapsedMs;
    public final boolean screenInteractive;
    public final float batteryUsedMilliAmpHours;
    public final int batteryDropPercent;

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
        this(
                started,
                startedAtElapsedMs,
                lastAcceptedFixElapsedMs,
                travelledDistanceMeters,
                movingDurationMs,
                stationaryDurationMs,
                maxSpeedMps,
                acceptedFixCount,
                activeMovingInterval,
                activeStationaryInterval,
                0L,
                0L,
                0L,
                true,
                Float.NaN,
                UNKNOWN_BATTERY_DROP_PERCENT
        );
    }

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
            boolean activeStationaryInterval,
            long screenOnDurationMs,
            long screenOffDurationMs,
            long lastScreenTransitionElapsedMs,
            boolean screenInteractive,
            float batteryUsedMilliAmpHours,
            int batteryDropPercent
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
        this.screenOnDurationMs = Math.max(0L, screenOnDurationMs);
        this.screenOffDurationMs = Math.max(0L, screenOffDurationMs);
        this.lastScreenTransitionElapsedMs = Math.max(0L, lastScreenTransitionElapsedMs);
        this.screenInteractive = screenInteractive;
        this.batteryUsedMilliAmpHours = sanitizeBatteryUsed(batteryUsedMilliAmpHours);
        this.batteryDropPercent = batteryDropPercent >= 0
                ? batteryDropPercent
                : UNKNOWN_BATTERY_DROP_PERCENT;
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

    public long screenOnDurationMs(long nowElapsedMs) {
        return screenOnDurationMs + (screenInteractive ? openScreenIntervalMs(nowElapsedMs) : 0L);
    }

    public long screenOffDurationMs(long nowElapsedMs) {
        return screenOffDurationMs + (screenInteractive ? 0L : openScreenIntervalMs(nowElapsedMs));
    }

    private long openAcceptedFixIntervalMs(long nowElapsedMs) {
        if (!started || lastAcceptedFixElapsedMs == NO_ACCEPTED_FIX) {
            return 0L;
        }
        return Math.max(0L, nowElapsedMs - lastAcceptedFixElapsedMs);
    }

    private long openScreenIntervalMs(long nowElapsedMs) {
        if (!started) {
            return 0L;
        }
        return Math.max(0L, nowElapsedMs - lastScreenTransitionElapsedMs);
    }

    private static float sanitizeBatteryUsed(float value) {
        return Float.isFinite(value) && value >= 0f ? value : Float.NaN;
    }
}
