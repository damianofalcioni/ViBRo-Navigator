package com.vibenavigator.nav;

import android.content.Context;

import androidx.annotation.NonNull;

import com.vibenavigator.R;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.PolylineIndex;
import com.vibenavigator.nav.route.VoiceHint;

import java.util.ArrayList;
import java.util.List;

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
    public static NavState withNotice(@NonNull NavState base, @NonNull String notice) {
        if (notice.trim().isEmpty()) {
            return base;
        }
        String remaining = base.remainingBlock.isEmpty()
                ? notice
                : notice + "\n" + base.remainingBlock;
        return new NavState(
                base.nextLine,
                base.afterNextLine,
                base.accuracyLine,
                base.nextEvaluationDeadlineElapsedMs,
                remaining
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
        List<String> directionLines = buildDirectionLines(
                route,
                index,
                alongTrackMeters,
                nextHintIdx,
                speedMps,
                accuracyMeters,
                context
        );
        String next = directionLines.isEmpty() ? "" : directionLines.get(0);
        String afterNext = directionLines.size() > 1 ? directionLines.get(1) : "";
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
    private static List<String> buildDirectionLines(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int hintIdx,
            float speedMps,
            float accuracyMeters,
            @NonNull Context context
    ) {
        if (route.voiceHints.isEmpty() || hintIdx < 0 || hintIdx >= route.voiceHints.size()) {
            return new ArrayList<>();
        }
        List<String> lines = new ArrayList<>(2);
        double minReliableDistanceMeters = minimumReliableTurnDistanceMeters(accuracyMeters);
        for (int i = hintIdx; i < route.voiceHints.size() && lines.size() < 2; i++) {
            VoiceHint hint = route.voiceHints.get(i);
            double hintDist = index.distanceAtPointIndex(hint.indexInTrack);
            double dist = Math.max(0.0, hintDist - alongTrackMeters);
            if (dist <= minReliableDistanceMeters) {
                continue;
            }
            double time = dist / Math.max(1.0, speedMps);
            lines.add(NavigationTextFormatter.formatTurnNotification(context, hint, dist, time));
        }
        return lines;
    }

    private static double minimumReliableTurnDistanceMeters(float accuracyMeters) {
        double safeAccuracyMeters = Float.isFinite(accuracyMeters) && accuracyMeters > 0f
                ? accuracyMeters
                : 0.0;
        return Math.max(5.0, safeAccuracyMeters);
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
                    NavigationTextFormatter.formatDistance(context, distTo),
                    NavigationTextFormatter.formatTimeSeconds(context, (int) Math.round(secTo)),
                    context.getString(R.string.nav_eta),
                    NavigationTextFormatter.formatEta(nowMs + (long) (secTo * 1000))
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
}
