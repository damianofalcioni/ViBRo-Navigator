package com.vibenavigator.nav;

import android.content.Context;

import androidx.annotation.NonNull;

import com.vibenavigator.R;
import com.vibenavigator.nav.directions.DirectionInfo;
import com.vibenavigator.nav.directions.VoiceHintMapper;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.PolylineIndex;
import com.vibenavigator.nav.route.VoiceHint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class NavState {
    public static final long NO_DEADLINE = -1L;

    @NonNull
    public final String nextLine;
    @NonNull
    public final String afterNextLine;
    @NonNull
    public final String accuracyLine;
    public final long nextEvaluationDeadlineElapsedMs;
    @NonNull
    public final String remainingBlock;

    private NavState(@NonNull String nextLine,
                     @NonNull String afterNextLine,
                     @NonNull String accuracyLine,
                     long nextEvaluationDeadlineElapsedMs,
                     @NonNull String remainingBlock) {
        this.nextLine = nextLine;
        this.afterNextLine = afterNextLine;
        this.accuracyLine = accuracyLine;
        this.nextEvaluationDeadlineElapsedMs = nextEvaluationDeadlineElapsedMs;
        this.remainingBlock = remainingBlock;
    }

    @NonNull
    public static NavState waiting(@NonNull Context context) {
        String noRoute = context.getString(R.string.nav_no_route);
        return new NavState(noRoute, "", "", NO_DEADLINE, noRoute);
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull Context context) {
        return waitingForLocation(context, NO_DEADLINE);
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull Context context, long nextEvaluationDeadlineElapsedMs) {
        return new NavState(
                context.getString(R.string.nav_waiting_for_location_title),
                "",
                "",
                nextEvaluationDeadlineElapsedMs,
                context.getString(R.string.nav_waiting_for_location_body)
        );
    }

    @NonNull
    public static NavState calculatingRoute(@NonNull Context context) {
        return calculatingRoute(context, NO_DEADLINE);
    }

    @NonNull
    public static NavState calculatingRoute(@NonNull Context context, long nextEvaluationDeadlineElapsedMs) {
        return new NavState(
                context.getString(R.string.nav_calculating_route_title),
                "",
                "",
                nextEvaluationDeadlineElapsedMs,
                context.getString(R.string.nav_calculating_route_body)
        );
    }

    @NonNull
    public static NavState routeUnavailable(@NonNull Context context, @NonNull String detail) {
        return routeUnavailable(context, detail, NO_DEADLINE);
    }

    @NonNull
    public static NavState routeUnavailable(@NonNull Context context,
                                            @NonNull String detail,
                                            long nextEvaluationDeadlineElapsedMs) {
        return new NavState(
                context.getString(R.string.nav_route_unavailable_title),
                "",
                "",
                nextEvaluationDeadlineElapsedMs,
                context.getString(R.string.format_nav_route_unavailable_body, detail)
        );
    }

    @NonNull
    public static NavState from(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIdx,
            float speedMps,
            float accuracyMeters,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        String next = buildDirectionLine(route, index, alongTrackMeters, nextHintIdx, speedMps, context);
        String afterNext = buildDirectionLine(route, index, alongTrackMeters, nextHintIdx + 1, speedMps, context);
        String accuracy = buildAccuracyLine(accuracyMeters, context);
        String remaining = buildRemaining(route, index, alongTrackMeters, speedMps, nowMs, targets, context);
        return new NavState(next, afterNext, accuracy, nextEvaluationDeadlineElapsedMs, remaining);
    }

    @NonNull
    private static String buildAccuracyLine(float accuracyMeters, @NonNull Context context) {
        if (!Float.isFinite(accuracyMeters) || accuracyMeters <= 0f) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_accuracy_value, accuracyMeters);
    }

    @NonNull
    private static String buildDirectionLine(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int hintIdx,
            float speedMps,
            @NonNull Context context
    ) {
        if (route.voiceHints.isEmpty() || hintIdx < 0 || hintIdx >= route.voiceHints.size()) {
            return "";
        }
        VoiceHint hint = route.voiceHints.get(hintIdx);
        double hintDist = index.distanceAtPointIndex(hint.indexInTrack);
        double dist = Math.max(0.0, hintDist - alongTrackMeters);
        double time = dist / Math.max(1.0, speedMps);

        DirectionInfo di = VoiceHintMapper.toDirection(hint);
        String dirText = di.exitNumber > 0 ? context.getString(di.labelRes, di.exitNumber) : context.getString(di.labelRes);
        String distText = formatDistance(context, dist);
        String timeText = formatTimeSeconds(context, (int) Math.round(time));
        return context.getString(R.string.format_turn_notification, di.emoji, distText, timeText, dirText);
    }

    @NonNull
    private static String buildRemaining(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            float speedMps,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        List<String> lines = new ArrayList<>();
        double total = index.totalLengthMeters();

        for (NavTarget t : targets) {
            double distTo = Math.max(0.0, t.alongTrackMeters - alongTrackMeters);
            double secTo = estimateSeconds(route, total, distTo, speedMps);
            String line = context.getString(
                    R.string.format_progress_line,
                    t.label,
                    formatDistance(context, distTo),
                    formatTimeSeconds(context, (int) Math.round(secTo)),
                    context.getString(R.string.nav_eta),
                    formatEta(context, nowMs + (long) (secTo * 1000))
            );
            lines.add(line);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    private static double estimateSeconds(@NonNull GeoJsonRoute route, double totalMeters, double remainingMeters, float speedMps) {
        if (speedMps >= 1.0f) {
            return remainingMeters / speedMps;
        }
        if (route.totalTimeSeconds > 0.0 && totalMeters > 0.0) {
            return route.totalTimeSeconds * (remainingMeters / totalMeters);
        }
        return 0.0;
    }

    @NonNull
    private static String formatDistance(@NonNull Context context, double meters) {
        if (meters >= 1000.0) {
            return context.getString(R.string.format_distance_km, meters / 1000.0);
        }
        return context.getString(R.string.format_distance_m, meters);
    }

    @NonNull
    private static String formatTimeSeconds(@NonNull Context context, int seconds) {
        if (seconds >= 60) {
            return context.getString(R.string.format_time_min, (int) Math.round(seconds / 60.0));
        }
        return context.getString(R.string.format_time_s, Math.max(0, seconds));
    }

    @NonNull
    private static String formatEta(@NonNull Context context, long timeMs) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMs);
        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }
}
