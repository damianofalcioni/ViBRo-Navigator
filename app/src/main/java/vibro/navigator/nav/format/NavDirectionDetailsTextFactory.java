package vibro.navigator.nav.format;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.nav.guidance.RouteTimeEstimator;
import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

public final class NavDirectionDetailsTextFactory {
    private static final int ALL_UPCOMING_HINTS = Integer.MAX_VALUE;

    private NavDirectionDetailsTextFactory() {
    }

    @NonNull
    public static List<String> buildRelativeLines(
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
            return destinationReachedLine(route, textResources);
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
                ALL_UPCOMING_HINTS
        );
        return formatRelativeLines(route, index, upcomingHints, textResources);
    }

    @NonNull
    private static List<String> destinationReachedLine(
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
    private static List<String> formatRelativeLines(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<NavUpcomingHint> upcomingHints,
            @NonNull NavigationTextResources textResources
    ) {
        List<String> lines = new ArrayList<>(upcomingHints.size());
        if (upcomingHints.isEmpty()) {
            return lines;
        }
        addNextLine(lines, upcomingHints.get(0), textResources);
        for (int i = 1; i < upcomingHints.size(); i++) {
            addFollowingLine(route, index, lines, upcomingHints.get(i - 1), upcomingHints.get(i), textResources);
        }
        return lines;
    }

    private static void addNextLine(
            @NonNull List<String> lines,
            @NonNull NavUpcomingHint hint,
            @NonNull NavigationTextResources textResources
    ) {
        lines.add(NavigationTextFormatter.formatTurnNotification(
                textResources,
                hint.hint,
                hint.distanceMeters,
                hint.timeSeconds
        ));
    }

    private static void addFollowingLine(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<String> lines,
            @NonNull NavUpcomingHint previousHint,
            @NonNull NavUpcomingHint hint,
            @NonNull NavigationTextResources textResources
    ) {
        double distanceMeters = Math.max(0.0, hint.distanceMeters - previousHint.distanceMeters);
        double timeSeconds = relativeHintTimeSeconds(route, index, previousHint, hint);
        lines.add(NavigationTextFormatter.formatTurnNotification(
                textResources,
                hint.hint,
                distanceMeters,
                timeSeconds
        ));
    }

    private static double relativeHintTimeSeconds(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull NavUpcomingHint previousHint,
            @NonNull NavUpcomingHint hint
    ) {
        if (Double.isFinite(previousHint.timeSeconds) && Double.isFinite(hint.timeSeconds)) {
            return Math.max(0.0, hint.timeSeconds - previousHint.timeSeconds);
        }
        Double estimatedSeconds = RouteTimeEstimator.estimateSecondsBetweenTrackPoints(
                route,
                index,
                previousHint.hint.indexInTrack,
                hint.hint.indexInTrack
        );
        return estimatedSeconds != null ? estimatedSeconds : Double.NaN;
    }
}
