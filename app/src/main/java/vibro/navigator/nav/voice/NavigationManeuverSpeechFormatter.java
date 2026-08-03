package vibro.navigator.nav.voice;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Locale;

import vibro.navigator.R;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationOrientationDegreeFormatter;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.directions.DirectionInfo;
import vibro.navigator.nav.directions.VoiceHintMapper;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;

public final class NavigationManeuverSpeechFormatter {
    private static final int ARRIVAL_COMMAND = 100;
    private static final int INTERMEDIATE_ARRIVAL_COMMAND = 101;

    private NavigationManeuverSpeechFormatter() {
    }

    @NonNull
    public static String formatTurnSpeech(
            @NonNull Context context,
            @NonNull VoiceHint hint,
            double timeSeconds
    ) {
        return formatTurnSpeech(new AndroidNavigationTextResources(context), hint, timeSeconds);
    }

    @NonNull
    public static String formatStationaryOrientationSpeech(
            @NonNull Context context,
            @NonNull StationaryOrientationAdvisor.Decision decision
    ) {
        return formatStationaryOrientationSpeech(new AndroidNavigationTextResources(context), decision);
    }

    @NonNull
    public static String formatOffRouteSpeech(
            @NonNull Context context,
            @NonNull NavigationRerouteNotice rerouteNotice
    ) {
        return formatOffRouteSpeech(new AndroidNavigationTextResources(context), rerouteNotice);
    }

    @NonNull
    public static String formatWrongDirectionSpeech(@NonNull Context context) {
        return formatWrongDirectionSpeech(new AndroidNavigationTextResources(context));
    }

    @NonNull
    static String formatTurnSpeech(
            @NonNull NavigationTextResources resources,
            @NonNull VoiceHint hint,
            double timeSeconds
    ) {
        DirectionInfo direction = VoiceHintMapper.toDirection(hint);
        String directionText = firstCharLowercase(formatDirectionText(resources, direction));
        if (isReachedArrival(hint) && timeSeconds <= 0.0) {
            return directionText;
        }
        String timeText = formatTimeSecondsForSpeech(resources, timeSeconds);
        if (timeText.isEmpty()) {
            return "";
        }
        return resources.getString(R.string.format_turn_speech, timeText, directionText);
    }

    @NonNull
    static String formatStationaryOrientationSpeech(
            @NonNull NavigationTextResources resources,
            @NonNull StationaryOrientationAdvisor.Decision decision
    ) {
        int degrees = NavigationOrientationDegreeFormatter.roundedTurnDegrees(decision.absoluteTurnDegrees());
        return resources.getQuantityString(
                R.plurals.format_stationary_orientation_speech,
                degrees,
                degrees,
                resources.getString(decision.turnRight()
                        ? R.string.direction_side_right
                        : R.string.direction_side_left)
        );
    }

    @NonNull
    static String formatOffRouteSpeech(
            @NonNull NavigationTextResources resources,
            @NonNull NavigationRerouteNotice rerouteNotice
    ) {
        if (isWrongDirectionNoticeOnly(rerouteNotice)) {
            return formatWrongDirectionSpeech(resources);
        }
        return resources.getString(rerouteNotice.routeRecalculationExpected
                ? R.string.speech_off_route_recalculating
                : R.string.speech_off_route);
    }

    @NonNull
    static String formatWrongDirectionSpeech(@NonNull NavigationTextResources resources) {
        return resources.getString(R.string.speech_wrong_direction_turn_back);
    }

    @NonNull
    private static String formatTimeSecondsForSpeech(@NonNull NavigationTextResources resources, double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0.0) {
            return "";
        }
        int roundedSeconds = Math.max(0, (int) Math.round(seconds));
        if (roundedSeconds >= 60) {
            int minutes = (int) Math.round(roundedSeconds / 60.0);
            return resources.getQuantityString(
                    R.plurals.format_time_speech_minutes,
                    minutes,
                    minutes
            );
        }
        return resources.getQuantityString(
                R.plurals.format_time_speech_seconds,
                roundedSeconds,
                roundedSeconds
        );
    }

    @NonNull
    private static String formatDirectionText(
            @NonNull NavigationTextResources resources,
            @NonNull DirectionInfo direction
    ) {
        return direction.exitNumber > 0
                ? resources.getString(direction.labelRes, direction.exitNumber)
                : resources.getString(direction.labelRes);
    }

    @NonNull
    private static String firstCharLowercase(@NonNull String text) {
        if (text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toLowerCase(Locale.getDefault()) + text.substring(1);
    }

    private static boolean isReachedArrival(@NonNull VoiceHint hint) {
        return hint.command == ARRIVAL_COMMAND
                || hint.command == INTERMEDIATE_ARRIVAL_COMMAND;
    }

    private static boolean isWrongDirectionNoticeOnly(@NonNull NavigationRerouteNotice rerouteNotice) {
        return rerouteNotice.reason == RouteDeviationPolicy.Reason.BEARING_MISMATCH
                && !rerouteNotice.routeRecalculationExpected;
    }
}
