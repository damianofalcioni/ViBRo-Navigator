package vibro.navigator.nav.format;



import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.guidance.RouteTimeEstimator;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NavStateTextFactory {
    private NavStateTextFactory() {
    }

    @NonNull
    public static List<String> buildDirectionLines(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int hintIdx,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters,
            boolean destinationReached,
            @NonNull Context context
    ) {
        if (route.track.isEmpty()) {
            return new ArrayList<>();
        }
        if (destinationReached) {
            return buildDestinationReachedDirectionLines(route, context);
        }

        List<UpcomingHint> upcomingHints = collectUpcomingDirectionHints(
                route,
                index,
                alongTrackMeters,
                hintIdx,
                currentSegmentIndex,
                speedMps,
                accuracyMeters
        );
        return formatDirectionLines(route, index, upcomingHints, context);
    }

    @NonNull
    private static List<String> buildDestinationReachedDirectionLines(
            @NonNull GeoJsonRoute route,
            @NonNull Context context
    ) {
        return new ArrayList<>(Collections.singletonList(
                NavigationTextFormatter.formatTurnNotification(
                        context,
                        new VoiceHint(route.track.size() - 1, NavArrivalHintFactory.ARRIVAL_COMMAND, 0, 0.0, 0),
                        0.0,
                        0.0
                )
        ));
    }

    @NonNull
    private static List<UpcomingHint> collectUpcomingDirectionHints(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int hintIdx,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters
    ) {
        List<UpcomingHint> upcomingHints = new ArrayList<>(2);
        double minReliableDistanceMeters = minimumReliableTurnDistanceMeters(accuracyMeters);
        int startHintIdx = Math.max(0, hintIdx);
        for (int i = startHintIdx; i < route.voiceHints.size() && upcomingHints.size() < 2; i++) {
            VoiceHint hint = route.voiceHints.get(i);
            double hintDist = index.distanceAtPointIndex(hint.indexInTrack);
            double dist = Math.max(0.0, hintDist - alongTrackMeters);
            if (dist <= minReliableDistanceMeters && hint.command != NavArrivalHintFactory.ARRIVAL_COMMAND) {
                continue;
            }
            Double timeSeconds = RouteTimeEstimator.estimateSecondsToTrackPoint(
                    route,
                    index,
                    alongTrackMeters,
                    currentSegmentIndex,
                    hint.indexInTrack,
                    speedMps
            );
            upcomingHints.add(new UpcomingHint(
                    hint,
                    dist,
                    timeSeconds != null ? timeSeconds : Double.NaN
            ));
        }
        NavArrivalHintFactory.ArrivalHint arrivalHint = NavArrivalHintFactory.buildSyntheticArrivalHint(
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                startHintIdx,
                upcomingHints.size()
        );
        if (arrivalHint != null) {
            upcomingHints.add(new UpcomingHint(
                    arrivalHint.hint,
                    arrivalHint.distanceMeters,
                    arrivalHint.timeSeconds
            ));
        }
        return upcomingHints;
    }

    @NonNull
    private static List<String> formatDirectionLines(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<UpcomingHint> upcomingHints,
            @NonNull Context context
    ) {
        List<String> lines = new ArrayList<>(upcomingHints.size());
        if (upcomingHints.isEmpty()) {
            return lines;
        }
        addNextDirectionLine(lines, upcomingHints.get(0), context);
        if (upcomingHints.size() > 1) {
            addFollowingDirectionLine(route, index, lines, upcomingHints.get(0), upcomingHints.get(1), context);
        }
        return lines;
    }

    private static void addNextDirectionLine(
            @NonNull List<String> lines,
            @NonNull UpcomingHint nextHint,
            @NonNull Context context
    ) {
        lines.add(NavigationTextFormatter.formatTurnNotification(
                context,
                nextHint.hint,
                nextHint.distanceMeters,
                nextHint.timeSeconds
        ));
    }

    private static void addFollowingDirectionLine(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<String> lines,
            @NonNull UpcomingHint nextHint,
            @NonNull UpcomingHint afterNextHint,
            @NonNull Context context
    ) {
        double relativeDistanceMeters = Math.max(
                0.0,
                afterNextHint.distanceMeters - nextHint.distanceMeters
        );
        double relativeTimeSeconds = relativeHintTimeSeconds(route, index, nextHint, afterNextHint);
        lines.add(NavigationTextFormatter.formatTurnNotification(
                context,
                afterNextHint.hint,
                relativeDistanceMeters,
                relativeTimeSeconds
        ));
    }

    private static double relativeHintTimeSeconds(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull UpcomingHint nextHint,
            @NonNull UpcomingHint afterNextHint
    ) {
        return Double.isFinite(nextHint.timeSeconds) && Double.isFinite(afterNextHint.timeSeconds)
                ? Math.max(0.0, afterNextHint.timeSeconds - nextHint.timeSeconds)
                : resolveRelativeHintTimeSeconds(route, index, nextHint, afterNextHint);
    }

    private static double minimumReliableTurnDistanceMeters(float accuracyMeters) {
        double safeAccuracyMeters = Float.isFinite(accuracyMeters) && accuracyMeters > 0f
                ? accuracyMeters
                : 0.0;
        return Math.max(5.0, safeAccuracyMeters);
    }

    private static double resolveRelativeHintTimeSeconds(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull UpcomingHint nextHint,
            @NonNull UpcomingHint afterNextHint
    ) {
        Double estimatedSeconds = RouteTimeEstimator.estimateSecondsBetweenTrackPoints(
                route,
                index,
                nextHint.hint.indexInTrack,
                afterNextHint.hint.indexInTrack
        );
        return estimatedSeconds != null ? estimatedSeconds : Double.NaN;
    }

    @NonNull
    public static String buildDestinationLine(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            long nowMs,
            boolean destinationReached,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        if (destinationReached) {
            return context.getString(R.string.nav_destination_reached);
        }
        if (targets.isEmpty()) {
            return "";
        }
        NavTarget destination = targets.get(targets.size() - 1);
        double distTo = Math.max(0.0, destination.alongTrackMeters - alongTrackMeters);
        Double secTo = RouteTimeEstimator.estimateSecondsToAlongTrack(
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                destination.alongTrackMeters,
                speedMps
        );
        return buildProgressLine(context, destination.label, distTo, secTo, nowMs);
    }

    @NonNull
    public static String buildStopProgress(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            long nowMs,
            boolean destinationReached,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        if (destinationReached) {
            return "";
        }
        int lastStopIndex = Math.max(0, targets.size() - 1);
        for (int i = 0; i < lastStopIndex; i++) {
            NavTarget t = targets.get(i);
            double distTo = Math.max(0.0, t.alongTrackMeters - alongTrackMeters);
            if (distTo <= 0.0) {
                continue;
            }
            Double secTo = RouteTimeEstimator.estimateSecondsToAlongTrack(
                    route,
                    index,
                    alongTrackMeters,
                    currentSegmentIndex,
                    t.alongTrackMeters,
                    speedMps
            );
            return buildProgressLine(context, t.label, distTo, secTo, nowMs);
        }
        return "";
    }

    @NonNull
    private static String buildProgressLine(
            @NonNull Context context,
            @NonNull String label,
            double distanceMeters,
            @Nullable Double seconds,
            long nowMs
    ) {
        String timeText = NavigationTextFormatter.formatTimeSeconds(
                context,
                seconds != null ? seconds : Double.NaN
        );
        String etaText = seconds != null && Double.isFinite(seconds)
                ? NavigationTextFormatter.formatEta(nowMs + (long) (seconds * 1000))
                : context.getString(R.string.nav_status_unavailable);
        return context.getString(
                R.string.format_progress_line,
                label,
                NavigationTextFormatter.formatDistance(context, distanceMeters),
                timeText,
                context.getString(R.string.nav_eta),
                etaText
        );
    }

    private static final class UpcomingHint {
        @NonNull
        public final VoiceHint hint;
        public final double distanceMeters;
        public final double timeSeconds;

        private UpcomingHint(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            this.hint = hint;
            this.distanceMeters = distanceMeters;
            this.timeSeconds = timeSeconds;
        }
    }
}
