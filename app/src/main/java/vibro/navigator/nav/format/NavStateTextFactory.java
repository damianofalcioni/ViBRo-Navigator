package vibro.navigator.nav.format;



import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.guidance.RouteTimeEstimator;

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
            int intermediateDestinationReachedTrackIndex,
            @NonNull List<NavTarget> targets,
            @NonNull NavigationTextResources textResources
    ) {
        if (route.track.isEmpty()) {
            return new ArrayList<>();
        }
        if (destinationReached) {
            return buildDestinationReachedDirectionLines(route, textResources);
        }

        List<NavUpcomingHint> upcomingHints = NavUpcomingHintCollector.collect(
                route,
                index,
                alongTrackMeters,
                hintIdx,
                currentSegmentIndex,
                speedMps,
                accuracyMeters,
                targets,
                intermediateDestinationReachedTrackIndex,
                2
        );
        return formatDirectionLines(route, index, upcomingHints, textResources);
    }

    @NonNull
    private static List<String> buildDestinationReachedDirectionLines(
            @NonNull GeoJsonRoute route,
            @NonNull NavigationTextResources textResources
    ) {
        return new ArrayList<>(Collections.singletonList(
                NavigationTextFormatter.formatTurnNotification(
                        textResources,
                        new VoiceHint(route.track.size() - 1, NavArrivalHintFactory.ARRIVAL_COMMAND, 0, 0.0, 0),
                        0.0,
                        0.0
                )
        ));
    }

    @NonNull
    private static List<String> formatDirectionLines(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<NavUpcomingHint> upcomingHints,
            @NonNull NavigationTextResources textResources
    ) {
        List<String> lines = new ArrayList<>(upcomingHints.size());
        if (upcomingHints.isEmpty()) {
            return lines;
        }
        addNextDirectionLine(lines, upcomingHints.get(0), textResources);
        if (upcomingHints.size() > 1) {
            addFollowingDirectionLine(route, index, lines, upcomingHints.get(0), upcomingHints.get(1), textResources);
        }
        return lines;
    }

    private static void addNextDirectionLine(
            @NonNull List<String> lines,
            @NonNull NavUpcomingHint nextHint,
            @NonNull NavigationTextResources textResources
    ) {
        lines.add(NavigationTextFormatter.formatTurnNotification(
                textResources,
                nextHint.hint,
                nextHint.distanceMeters,
                nextHint.timeSeconds
        ));
    }

    private static void addFollowingDirectionLine(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<String> lines,
            @NonNull NavUpcomingHint nextHint,
            @NonNull NavUpcomingHint afterNextHint,
            @NonNull NavigationTextResources textResources
    ) {
        double relativeDistanceMeters = Math.max(
                0.0,
                afterNextHint.distanceMeters - nextHint.distanceMeters
        );
        double relativeTimeSeconds = relativeHintTimeSeconds(route, index, nextHint, afterNextHint);
        lines.add(NavigationTextFormatter.formatTurnNotification(
                textResources,
                afterNextHint.hint,
                relativeDistanceMeters,
                relativeTimeSeconds
        ));
    }

    private static double relativeHintTimeSeconds(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull NavUpcomingHint nextHint,
            @NonNull NavUpcomingHint afterNextHint
    ) {
        return Double.isFinite(nextHint.timeSeconds) && Double.isFinite(afterNextHint.timeSeconds)
                ? Math.max(0.0, afterNextHint.timeSeconds - nextHint.timeSeconds)
                : resolveRelativeHintTimeSeconds(route, index, nextHint, afterNextHint);
    }

    private static double resolveRelativeHintTimeSeconds(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull NavUpcomingHint nextHint,
            @NonNull NavUpcomingHint afterNextHint
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
            @NonNull NavigationTextResources textResources
    ) {
        if (destinationReached) {
            return textResources.getString(R.string.nav_destination_reached);
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
        return buildProgressLine(textResources, destination.label, distTo, secTo, nowMs);
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
            @NonNull NavigationTextResources textResources
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
            return buildProgressLine(textResources, t.label, distTo, secTo, nowMs);
        }
        return "";
    }

    @NonNull
    public static String buildProgressLine(
            @NonNull NavigationTextResources textResources,
            @NonNull String label,
            double distanceMeters,
            @Nullable Double seconds,
            long nowMs
    ) {
        String timeText = NavigationTextFormatter.formatTimeSeconds(
                textResources,
                seconds != null ? seconds : Double.NaN
        );
        String etaText = seconds != null && Double.isFinite(seconds)
                ? NavigationTextFormatter.formatEta(nowMs + (long) (seconds * 1000))
                : textResources.getString(R.string.nav_status_unavailable);
        return textResources.getString(
                R.string.format_progress_line,
                label,
                NavigationTextFormatter.formatDistance(textResources, distanceMeters),
                timeText,
                textResources.getString(R.string.nav_eta),
                etaText
        );
    }

}
