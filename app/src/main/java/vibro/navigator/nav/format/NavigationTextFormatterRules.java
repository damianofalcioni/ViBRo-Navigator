package vibro.navigator.nav.format;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import vibro.navigator.R;
import vibro.navigator.nav.directions.DirectionInfo;
import vibro.navigator.nav.directions.VoiceHintMapper;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;

final class NavigationTextFormatterRules {
    private NavigationTextFormatterRules() {
    }

    @NonNull
    static String formatTurnNotification(
            @NonNull NavigationTextResources resources,
            @NonNull VoiceHint hint,
            double distanceMeters,
            double timeSeconds
    ) {
        DirectionInfo direction = VoiceHintMapper.toDirection(hint);
        String directionText = direction.exitNumber > 0
                ? resources.getString(direction.labelRes, direction.exitNumber)
                : resources.getString(direction.labelRes);
        if (isReachedArrival(hint) && distanceMeters <= 0.0 && timeSeconds <= 0.0) {
            return String.format(
                    Locale.getDefault(),
                    "%s %s",
                    NavigationTextFormatter.formatDirectionSymbol(direction),
                    directionText
            );
        }
        return resources.getString(
                R.string.format_turn_notification,
                NavigationTextFormatter.formatDirectionSymbol(direction),
                NavigationMeasurementFormatter.formatDistance(resources, distanceMeters),
                formatTimeSeconds(resources, timeSeconds),
                directionText
        );
    }

    @NonNull
    static String formatOffRouteNotification(
            @NonNull NavigationTextResources resources,
            @NonNull NavigationRerouteNotice rerouteNotice
    ) {
        switch (rerouteNotice.reason) {
            case OFF_TRACK:
                return resources.getString(
                        rerouteNotice.routeRecalculationExpected
                                ? R.string.format_off_route_off_track_notification
                                : R.string.format_off_route_off_track_notice_only,
                        NavigationMeasurementFormatter.formatDistance(resources, rerouteNotice.distanceToTrackMeters),
                        NavigationMeasurementFormatter.formatDistance(resources, rerouteNotice.offTrackThresholdMeters)
                );
            case BEARING_MISMATCH:
                return resources.getString(
                        rerouteNotice.routeRecalculationExpected
                                ? R.string.format_off_route_bearing_notification
                                : R.string.format_off_route_bearing_notice_only,
                        formatBearingDegrees(resources, rerouteNotice.bearingDiffDegrees),
                        formatBearingDegrees(resources, rerouteNotice.expectedBearingDegrees),
                        formatBearingDegrees(resources, rerouteNotice.actualBearingDegrees)
                );
            case NONE:
            default:
                return resources.getString(R.string.notification_off_route_title);
        }
    }

    @NonNull
    static String formatWrongDirectionNotification(
            @NonNull NavigationTextResources resources,
            @NonNull NavigationWrongDirectionNotice wrongDirectionNotice
    ) {
        return resources.getString(
                R.string.format_wrong_direction_notification,
                formatBearingDegrees(resources, wrongDirectionNotice.expectedBearingDegrees),
                formatBearingDegrees(resources, wrongDirectionNotice.actualBearingDegrees),
                formatBearingDegrees(resources, wrongDirectionNotice.bearingDiffDegrees)
        );
    }

    @NonNull
    static String formatStationaryOrientationNotification(
            @NonNull NavigationTextResources resources,
            @NonNull StationaryOrientationAdvisor.Decision decision
    ) {
        return resources.getString(
                R.string.format_startup_orientation_notification,
                formatBearingDegrees(resources, decision.absoluteTurnDegrees()),
                resources.getString(decision.turnRight()
                        ? R.string.direction_side_right
                        : R.string.direction_side_left)
        );
    }

    @NonNull
    static String formatTimeSeconds(@NonNull NavigationTextResources resources, int seconds) {
        if (seconds >= 60) {
            return resources.getString(R.string.format_time_min, (int) Math.round(seconds / 60.0));
        }
        return resources.getString(R.string.format_time_s, Math.max(0, seconds));
    }

    @NonNull
    static String formatTimeSeconds(@NonNull NavigationTextResources resources, double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0.0) {
            return resources.getString(R.string.nav_status_unavailable);
        }
        return formatTimeSeconds(resources, (int) Math.round(seconds));
    }

    @NonNull
    static String formatBearingDegrees(@NonNull NavigationTextResources resources, @Nullable Double degrees) {
        return resources.getString(
                R.string.format_bearing_degrees,
                degrees == null ? 0.0 : degrees
        );
    }

    private static boolean isReachedArrival(@NonNull VoiceHint hint) {
        return hint.command == NavArrivalHintFactory.ARRIVAL_COMMAND
                || hint.command == NavArrivalHintFactory.INTERMEDIATE_ARRIVAL_COMMAND;
    }
}
