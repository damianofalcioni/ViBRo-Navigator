package vibro.navigator.nav.format;



import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.guidance.RouteTimeEstimator;
import vibro.navigator.nav.guidance.TurnDistanceReliability;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavStopEstimate;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NavStateTextFactory {
    private static final int VISIBLE_DIRECTION_LINE_COUNT = 2;

    private NavStateTextFactory() {
    }

    @NonNull
    public static NavGuidanceStatus buildGuidanceStatus(
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
            return new NavGuidanceStatus("", "");
        }
        if (destinationReached) {
            return guidanceStatus(buildDestinationReachedDirectionLines(route, textResources));
        }
        List<NavUpcomingHint> upcomingHints = NavUpcomingHintCollector.collect(
                route,
                index,
                alongTrackMeters,
                hintIdx,
                currentSegmentIndex,
                speedMps,
                targets,
                intermediateDestinationReachedTrackIndex,
                VISIBLE_DIRECTION_LINE_COUNT
        );
        if (upcomingHints.isEmpty()) {
            return new NavGuidanceStatus("", "");
        }
        NavUpcomingHint next = upcomingHints.get(0);
        NavUpcomingHint following = upcomingHints.size() > 1
                ? followingHint(route, index, next, upcomingHints.get(1)) : null;
        return new NavGuidanceStatus(formatDirectionLine(next, textResources),
                following == null ? "" : formatDirectionLine(following, textResources),
                isUncertain(upcomingHints, 0, accuracyMeters),
                isUncertain(upcomingHints, 1, accuracyMeters), maneuvers(upcomingHints, textResources),
                following == null ? null : maneuver(following, textResources));
    }

    @NonNull
    private static List<NavManeuver> maneuvers(@NonNull List<NavUpcomingHint> hints,
            @NonNull NavigationTextResources resources) {
        List<NavManeuver> result = new ArrayList<>();
        for (NavUpcomingHint upcoming : hints) {
            result.add(maneuver(upcoming, resources));
        }
        return result;
    }

    private static NavManeuver maneuver(NavUpcomingHint hint, NavigationTextResources resources) {
        return new NavManeuver(hint.hint, NavigationTextFormatter.maneuverInstruction(resources, hint.hint),
                hint.distanceMeters, hint.timeSeconds);
    }

    private static boolean isUncertain(@NonNull List<NavUpcomingHint> hints, int index, float accuracyMeters) {
        return index < hints.size()
                && TurnDistanceReliability.isInsideAccuracyRadius(hints.get(index).distanceMeters, accuracyMeters);
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
        return displayedLines(buildGuidanceStatus(
                route,
                index,
                alongTrackMeters,
                hintIdx,
                currentSegmentIndex,
                speedMps,
                accuracyMeters,
                destinationReached,
                intermediateDestinationReachedTrackIndex,
                targets,
                textResources
        ));
    }

    @NonNull
    private static NavGuidanceStatus guidanceStatus(@NonNull List<String> lines) {
        String next = lines.isEmpty() ? "" : lines.get(0);
        String afterNext = lines.size() > 1 ? lines.get(1) : "";
        return new NavGuidanceStatus(next, afterNext);
    }

    @NonNull
    private static List<String> displayedLines(@NonNull NavGuidanceStatus status) {
        List<String> lines = new ArrayList<>(VISIBLE_DIRECTION_LINE_COUNT);
        if (!status.nextLine.isEmpty()) {
            lines.add(status.nextLine);
        }
        if (!status.afterNextLine.isEmpty()) {
            lines.add(status.afterNextLine);
        }
        return lines;
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
    private static String formatDirectionLine(NavUpcomingHint hint, NavigationTextResources resources) {
        return NavigationTextFormatter.formatTurnNotification(resources, hint.hint, hint.distanceMeters,
                hint.timeSeconds);
    }

    private static NavUpcomingHint followingHint(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull NavUpcomingHint nextHint,
            @NonNull NavUpcomingHint afterNextHint
    ) {
        double relativeDistanceMeters = Math.max(
                0.0,
                afterNextHint.distanceMeters - nextHint.distanceMeters
        );
        double relativeTimeSeconds = relativeHintTimeSeconds(route, index, nextHint, afterNextHint);
        return new NavUpcomingHint(afterNextHint.hint, relativeDistanceMeters, relativeTimeSeconds);
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
    public static String formatStopProgress(@NonNull NavigationTextResources resources,
            @Nullable NavStopEstimate stop, long nowMs) {
        return stop == null ? "" : buildProgressLine(resources, stop.label(), stop.distanceMeters(),
                stop.timeSeconds(), nowMs);
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
                etaText
        );
    }

}
