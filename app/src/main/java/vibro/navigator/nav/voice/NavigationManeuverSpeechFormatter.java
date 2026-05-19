package vibro.navigator.nav.voice;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Locale;

import vibro.navigator.R;
import vibro.navigator.nav.directions.DirectionInfo;
import vibro.navigator.nav.directions.VoiceHintMapper;
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
        DirectionInfo direction = VoiceHintMapper.toDirection(hint);
        String directionText = firstCharLowercase(formatDirectionText(context, direction));
        if (isReachedArrival(hint) && timeSeconds <= 0.0) {
            return directionText;
        }
        String timeText = formatTimeSecondsForSpeech(context, timeSeconds);
        if (timeText.isEmpty()) {
            return "";
        }
        return context.getString(R.string.format_turn_speech, timeText, directionText);
    }

    @NonNull
    private static String formatTimeSecondsForSpeech(@NonNull Context context, double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0.0) {
            return "";
        }
        int roundedSeconds = Math.max(0, (int) Math.round(seconds));
        if (roundedSeconds >= 60) {
            return context.getString(R.string.format_time_speech_min, (int) Math.round(roundedSeconds / 60.0));
        }
        return context.getString(R.string.format_time_speech_s, roundedSeconds);
    }

    @NonNull
    private static String formatDirectionText(@NonNull Context context, @NonNull DirectionInfo direction) {
        return direction.exitNumber > 0
                ? context.getString(direction.labelRes, direction.exitNumber)
                : context.getString(direction.labelRes);
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
}
