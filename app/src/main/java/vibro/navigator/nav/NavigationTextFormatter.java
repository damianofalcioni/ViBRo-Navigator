package vibro.navigator.nav;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.directions.DirectionInfo;
import vibro.navigator.nav.directions.VoiceHintMapper;
import vibro.navigator.nav.route.VoiceHint;

import java.util.Calendar;
import java.util.Locale;

public final class NavigationTextFormatter {

    private NavigationTextFormatter() {
    }

    @NonNull
    public static String formatTurnNotification(
            @NonNull Context context,
            @NonNull VoiceHint hint,
            double distanceMeters,
            double timeSeconds
    ) {
        DirectionInfo direction = VoiceHintMapper.toDirection(hint);
        String directionText = direction.exitNumber > 0
                ? context.getString(direction.labelRes, direction.exitNumber)
                : context.getString(direction.labelRes);
        return context.getString(
                R.string.format_turn_notification,
                formatDirectionSymbol(direction),
                formatDistance(context, distanceMeters),
                formatTimeSeconds(context, timeSeconds),
                directionText
        );
    }

    @NonNull
    public static String formatDirectionSymbol(@NonNull DirectionInfo direction) {
        if (direction.exitNumber > 0) {
            return direction.emoji + direction.exitNumber;
        }
        return direction.emoji;
    }

    @NonNull
    public static String formatOffRouteNotification(
            @NonNull Context context,
            @NonNull NavigationRerouteNotice rerouteNotice
    ) {
        switch (rerouteNotice.reason) {
            case OFF_TRACK:
                return context.getString(
                        R.string.format_off_route_off_track_notification,
                        formatDistance(context, rerouteNotice.distanceToTrackMeters),
                        formatDistance(context, rerouteNotice.offTrackThresholdMeters)
                );
            case BEARING_MISMATCH:
                return context.getString(
                        R.string.format_off_route_bearing_notification,
                        formatBearingDegrees(context, rerouteNotice.bearingDiffDegrees),
                        formatBearingDegrees(context, rerouteNotice.expectedBearingDegrees),
                        formatBearingDegrees(context, rerouteNotice.actualBearingDegrees)
                );
            case NONE:
            default:
                return context.getString(R.string.notification_off_route_title);
        }
    }

    @NonNull
    public static String formatStationaryOrientationNotification(
            @NonNull Context context,
            @NonNull StationaryOrientationAdvisor.Decision decision
    ) {
        return context.getString(
                R.string.format_startup_orientation_notification,
                formatBearingDegrees(context, decision.absoluteTurnDegrees()),
                context.getString(decision.turnRight()
                        ? R.string.direction_side_right
                        : R.string.direction_side_left)
        );
    }

    @NonNull
    public static String formatDistance(@NonNull Context context, double meters) {
        if (meters >= 1000.0) {
            return context.getString(R.string.format_distance_km, meters / 1000.0);
        }
        return context.getString(R.string.format_distance_m, meters);
    }

    @NonNull
    public static String formatTimeSeconds(@NonNull Context context, int seconds) {
        if (seconds >= 60) {
            return context.getString(R.string.format_time_min, (int) Math.round(seconds / 60.0));
        }
        return context.getString(R.string.format_time_s, Math.max(0, seconds));
    }

    @NonNull
    public static String formatTimeSeconds(@NonNull Context context, double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0.0) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return formatTimeSeconds(context, (int) Math.round(seconds));
    }

    @NonNull
    public static String formatBearingDegrees(@NonNull Context context, @Nullable Double degrees) {
        return context.getString(
                R.string.format_bearing_degrees,
                degrees == null ? 0.0 : degrees
        );
    }

    @NonNull
    public static String formatGpsStatus(
            @NonNull Context context,
            float speedMps,
            @Nullable Double elevationMeters,
            float accuracyMeters,
            @Nullable Float bearingDegrees,
            @Nullable Float bearingAccuracyDegrees,
            @Nullable Integer fixedSatelliteCount
    ) {
        return context.getString(
                R.string.format_nav_gps_status,
                formatSpeed(context, speedMps),
                formatElevation(context, elevationMeters),
                formatGpsBearing(context, bearingDegrees),
                formatAccuracy(context, accuracyMeters),
                formatGpsBearingAccuracy(context, bearingAccuracyDegrees),
                formatFixedSatelliteCount(context, fixedSatelliteCount)
        );
    }

    @NonNull
    public static String formatSpeed(@NonNull Context context, float speedMps) {
        if (!Float.isFinite(speedMps) || speedMps < 0f) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_speed_value, speedMps * 3.6f);
    }

    @NonNull
    public static String formatElevation(@NonNull Context context, @Nullable Double elevationMeters) {
        if (elevationMeters == null || !Double.isFinite(elevationMeters)) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_elevation_value, elevationMeters);
    }

    @NonNull
    public static String formatAccuracy(@NonNull Context context, float accuracyMeters) {
        if (!Float.isFinite(accuracyMeters) || accuracyMeters <= 0f) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_accuracy_value, accuracyMeters);
    }

    @NonNull
    public static String formatFixedSatelliteCount(
            @NonNull Context context,
            @Nullable Integer fixedSatelliteCount
    ) {
        if (fixedSatelliteCount == null || fixedSatelliteCount < 0) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_satellite_count_value, fixedSatelliteCount);
    }

    @NonNull
    public static String formatGpsBearing(@NonNull Context context, @Nullable Float bearingDegrees) {
        if (bearingDegrees == null || !Float.isFinite(bearingDegrees)) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_bearing_value, bearingDegrees);
    }

    @NonNull
    public static String formatGpsBearingAccuracy(
            @NonNull Context context,
            @Nullable Float bearingAccuracyDegrees
    ) {
        if (bearingAccuracyDegrees == null
                || !Float.isFinite(bearingAccuracyDegrees)
                || bearingAccuracyDegrees < 0f) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_bearing_accuracy_value, bearingAccuracyDegrees);
    }

    @NonNull
    public static String formatEta(long timeMs) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMs);
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
        );
    }
}
