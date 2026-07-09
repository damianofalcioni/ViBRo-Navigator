package vibro.navigator.nav.format;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavTripStatus;

public final class NavigationTripStatsFormatter {
    private NavigationTripStatsFormatter() {
    }

    @NonNull
    public static String formatDetails(
            @NonNull Context context,
            @NonNull NavTripStatus status,
            long nowElapsedMs
    ) {
        return formatDetails(new AndroidNavigationTextResources(context), status, nowElapsedMs);
    }

    @NonNull
    public static String formatDetails(
            @NonNull NavigationTextResources resources,
            @NonNull NavTripStatus status,
            long nowElapsedMs
    ) {
        return resources.getString(
                R.string.format_nav_trip_stats_details,
                formatElapsed(resources, status, nowElapsedMs),
                formatDistance(resources, status),
                formatDurationField(resources, status, status.movingDurationMs(nowElapsedMs)),
                formatDurationField(resources, status, status.stationaryDurationMs(nowElapsedMs)),
                formatOverallAverageSpeed(resources, status, nowElapsedMs),
                formatMovingAverageSpeed(resources, status, nowElapsedMs),
                formatSpeedField(resources, status, status.maxSpeedMps),
                formatDurationField(resources, status, status.screenOnDurationMs(nowElapsedMs)),
                formatDurationField(resources, status, status.screenOffDurationMs(nowElapsedMs)),
                formatBatteryUsed(resources, status),
                formatBatteryDrop(resources, status)
        );
    }

    @NonNull
    private static String formatElapsed(
            @NonNull NavigationTextResources resources,
            @NonNull NavTripStatus status,
            long nowElapsedMs
    ) {
        return status.started
                ? formatDuration(resources, status.elapsedDurationMs(nowElapsedMs))
                : resources.getString(R.string.nav_status_unavailable);
    }

    @NonNull
    private static String formatDistance(@NonNull NavigationTextResources resources, @NonNull NavTripStatus status) {
        return status.started
                ? NavigationMeasurementFormatter.formatDistance(resources, status.travelledDistanceMeters)
                : resources.getString(R.string.nav_status_unavailable);
    }

    @NonNull
    private static String formatOverallAverageSpeed(
            @NonNull NavigationTextResources resources,
            @NonNull NavTripStatus status,
            long nowElapsedMs
    ) {
        return formatSpeedField(
                resources,
                status,
                averageSpeedMps(status.travelledDistanceMeters, status.elapsedDurationMs(nowElapsedMs))
        );
    }

    @NonNull
    private static String formatMovingAverageSpeed(
            @NonNull NavigationTextResources resources,
            @NonNull NavTripStatus status,
            long nowElapsedMs
    ) {
        return formatSpeedField(
                resources,
                status,
                averageSpeedMps(status.travelledDistanceMeters, status.movingDurationMs(nowElapsedMs))
        );
    }

    @NonNull
    private static String formatSpeed(@NonNull NavigationTextResources resources, float speedMps) {
        return NavigationMeasurementFormatter.formatSpeed(resources, speedMps);
    }

    @NonNull
    private static String formatSpeedField(
            @NonNull NavigationTextResources resources,
            @NonNull NavTripStatus status,
            float speedMps
    ) {
        return status.started
                ? formatSpeed(resources, speedMps)
                : resources.getString(R.string.nav_status_unavailable);
    }

    @NonNull
    private static String formatDurationField(
            @NonNull NavigationTextResources resources,
            @NonNull NavTripStatus status,
            long durationMs
    ) {
        return status.started
                ? formatDuration(resources, durationMs)
                : resources.getString(R.string.nav_status_unavailable);
    }

    @NonNull
    private static String formatDuration(@NonNull NavigationTextResources resources, long durationMs) {
        long totalSeconds = Math.max(0L, Math.round(durationMs / 1000.0));
        if (totalSeconds >= 3600L) {
            return resources.getString(
                    R.string.format_duration_h_min,
                    totalSeconds / 3600L,
                    (totalSeconds % 3600L) / 60L
            );
        }
        if (totalSeconds >= 60L) {
            return resources.getString(
                    R.string.format_duration_min_s,
                    totalSeconds / 60L,
                    totalSeconds % 60L
            );
        }
        return resources.getString(R.string.format_time_s, totalSeconds);
    }

    @NonNull
    private static String formatBatteryUsed(
            @NonNull NavigationTextResources resources,
            @NonNull NavTripStatus status
    ) {
        return status.started && Float.isFinite(status.batteryUsedMilliAmpHours)
                ? resources.getString(R.string.format_nav_battery_used_mah, status.batteryUsedMilliAmpHours)
                : resources.getString(R.string.nav_status_unavailable);
    }

    @NonNull
    private static String formatBatteryDrop(
            @NonNull NavigationTextResources resources,
            @NonNull NavTripStatus status
    ) {
        return status.started && status.batteryDropPercent >= 0
                ? resources.getString(R.string.format_nav_battery_drop_percent, status.batteryDropPercent)
                : resources.getString(R.string.nav_status_unavailable);
    }

    private static float averageSpeedMps(double distanceMeters, long durationMs) {
        if (!Double.isFinite(distanceMeters) || distanceMeters <= 0.0 || durationMs <= 0L) {
            return 0f;
        }
        return (float) (distanceMeters / (durationMs / 1000.0));
    }
}
