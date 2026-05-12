package vibro.navigator.nav.format;


import vibro.navigator.nav.location.NavigationGpsTextFormatter;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
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
        if (isReachedArrival(hint) && distanceMeters <= 0.0 && timeSeconds <= 0.0) {
            return String.format(
                    Locale.getDefault(),
                    "%s %s",
                    formatDirectionSymbol(direction),
                    directionText
            );
        }
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
        return NavigationMeasurementFormatter.formatDistance(context, meters);
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
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount
    ) {
        return NavigationGpsTextFormatter.formatGpsStatus(
                context,
                speedMps,
                elevationMeters,
                accuracyMeters,
                bearingDegrees,
                bearingAccuracyDegrees,
                fixedSatelliteCount,
                acquiredFixCount
        );
    }

    private static boolean isReachedArrival(@NonNull VoiceHint hint) {
        return hint.command == NavArrivalHintFactory.ARRIVAL_COMMAND
                || hint.command == NavArrivalHintFactory.INTERMEDIATE_ARRIVAL_COMMAND;
    }

    @NonNull
    public static String formatSpeed(@NonNull Context context, float speedMps) {
        return NavigationGpsTextFormatter.formatSpeed(context, speedMps);
    }

    @NonNull
    public static String formatElevation(@NonNull Context context, @Nullable Double elevationMeters) {
        return NavigationGpsTextFormatter.formatElevation(context, elevationMeters);
    }

    @NonNull
    public static String formatAccuracy(@NonNull Context context, float accuracyMeters) {
        return NavigationGpsTextFormatter.formatAccuracy(context, accuracyMeters);
    }

    @NonNull
    public static String formatFixedSatelliteCount(
            @NonNull Context context,
            @Nullable Integer fixedSatelliteCount
    ) {
        return NavigationGpsTextFormatter.formatFixedSatelliteCount(context, fixedSatelliteCount);
    }

    @NonNull
    public static String formatAcquiredFixCount(
            @NonNull Context context,
            @Nullable Integer acquiredFixCount
    ) {
        return NavigationGpsTextFormatter.formatAcquiredFixCount(context, acquiredFixCount);
    }

    @NonNull
    public static String formatGpsBearing(@NonNull Context context, @Nullable Float bearingDegrees) {
        return NavigationGpsTextFormatter.formatGpsBearing(context, bearingDegrees);
    }

    @NonNull
    public static String formatGpsBearingAccuracy(
            @NonNull Context context,
            @Nullable Float bearingAccuracyDegrees
    ) {
        return NavigationGpsTextFormatter.formatGpsBearingAccuracy(context, bearingAccuracyDegrees);
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
