package com.vibenavigator.nav;

import android.content.Context;

import androidx.annotation.NonNull;

import com.vibenavigator.R;
import com.vibenavigator.nav.directions.DirectionInfo;
import com.vibenavigator.nav.directions.VoiceHintMapper;
import com.vibenavigator.nav.route.VoiceHint;

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
                direction.emoji,
                formatDistance(context, distanceMeters),
                formatTimeSeconds(context, (int) Math.round(timeSeconds)),
                directionText
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
