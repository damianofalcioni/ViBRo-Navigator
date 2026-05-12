package vibro.navigator.nav.format;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vibro.navigator.nav.guidance.RouteTimeEstimator;
import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

final class NavUpcomingHintCollector {
    private static final double TRACK_INDEX_TOLERANCE_METERS = 1.0;

    private NavUpcomingHintCollector() {
    }

    @NonNull
    static List<NavUpcomingHint> collect(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int hintIdx,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters,
            int maxHintCount
    ) {
        return collect(
                route,
                index,
                alongTrackMeters,
                hintIdx,
                currentSegmentIndex,
                speedMps,
                accuracyMeters,
                Collections.emptyList(),
                -1,
                maxHintCount
        );
    }

    @NonNull
    static List<NavUpcomingHint> collect(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int hintIdx,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters,
            @NonNull List<NavTarget> targets,
            int intermediateDestinationReachedTrackIndex,
            int maxHintCount
    ) {
        List<NavUpcomingHint> upcomingHints = new ArrayList<>();
        if (maxHintCount <= 0) {
            return upcomingHints;
        }
        addRouteHints(
                upcomingHints,
                route,
                index,
                alongTrackMeters,
                Math.max(0, hintIdx),
                currentSegmentIndex,
                speedMps,
                accuracyMeters
        );
        addSyntheticIntermediateArrivalHints(
                upcomingHints,
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                targets,
                intermediateDestinationReachedTrackIndex
        );
        addArrivalHintIfNeeded(
                upcomingHints,
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                Math.max(0, hintIdx),
                maxHintCount
        );
        Collections.sort(upcomingHints, new Comparator<NavUpcomingHint>() {
            @Override
            public int compare(NavUpcomingHint first, NavUpcomingHint second) {
                int distanceComparison = Double.compare(first.distanceMeters, second.distanceMeters);
                if (distanceComparison != 0) {
                    return distanceComparison;
                }
                return Integer.compare(
                        arrivalSortPriority(first.hint.command),
                        arrivalSortPriority(second.hint.command)
                );
            }
        });
        return new ArrayList<>(upcomingHints.subList(0, Math.min(maxHintCount, upcomingHints.size())));
    }

    private static void addRouteHints(
            @NonNull List<NavUpcomingHint> upcomingHints,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int startHintIdx,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters
    ) {
        double minReliableDistanceMeters = minimumReliableTurnDistanceMeters(accuracyMeters);
        for (int i = startHintIdx; i < route.voiceHints.size(); i++) {
            addRouteHintIfReliable(
                    upcomingHints,
                    route,
                    index,
                    route.voiceHints.get(i),
                    alongTrackMeters,
                    currentSegmentIndex,
                    speedMps,
                    minReliableDistanceMeters
            );
        }
    }

    private static void addRouteHintIfReliable(
            @NonNull List<NavUpcomingHint> upcomingHints,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull VoiceHint hint,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            double minReliableDistanceMeters
    ) {
        double hintDist = index.distanceAtPointIndex(hint.indexInTrack);
        double distanceMeters = Math.max(0.0, hintDist - alongTrackMeters);
        if (distanceMeters <= minReliableDistanceMeters && !isArrivalCommand(hint.command)) {
            return;
        }
        Double timeSeconds = RouteTimeEstimator.estimateSecondsToTrackPoint(
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                hint.indexInTrack,
                speedMps
        );
        upcomingHints.add(new NavUpcomingHint(
                hint,
                distanceMeters,
                timeSeconds != null ? timeSeconds : Double.NaN
        ));
    }

    private static void addSyntheticIntermediateArrivalHints(
            @NonNull List<NavUpcomingHint> upcomingHints,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            @NonNull List<NavTarget> targets,
            int intermediateDestinationReachedTrackIndex
    ) {
        int finalTargetIndex = targets.size() - 1;
        for (int i = 0; i < finalTargetIndex; i++) {
            NavTarget target = targets.get(i);
            int trackIndex = resolveTargetTrackIndex(route, index, target);
            if (target.alongTrackMeters <= alongTrackMeters
                    || isReachedIntermediateTarget(index, target, trackIndex, intermediateDestinationReachedTrackIndex)) {
                continue;
            }
            Double timeSeconds = RouteTimeEstimator.estimateSecondsToAlongTrack(
                    route,
                    index,
                    alongTrackMeters,
                    currentSegmentIndex,
                    target.alongTrackMeters,
                    speedMps
            );
            upcomingHints.add(new NavUpcomingHint(
                    new VoiceHint(trackIndex, NavArrivalHintFactory.INTERMEDIATE_ARRIVAL_COMMAND, 0, 0.0, 0),
                    target.alongTrackMeters - alongTrackMeters,
                    timeSeconds != null ? timeSeconds : Double.NaN
            ));
        }
    }

    private static void addArrivalHintIfNeeded(
            @NonNull List<NavUpcomingHint> upcomingHints,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            int startHintIdx,
            int maxHintCount
    ) {
        if (upcomingHints.size() >= maxHintCount && hasLaterHint(upcomingHints, route, index)) {
            return;
        }
        NavArrivalHintFactory.ArrivalHint arrivalHint = NavArrivalHintFactory.buildSyntheticArrivalHint(
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                startHintIdx
        );
        if (arrivalHint != null) {
            upcomingHints.add(new NavUpcomingHint(
                    arrivalHint.hint,
                    arrivalHint.distanceMeters,
                    arrivalHint.timeSeconds
            ));
        }
    }

    private static double minimumReliableTurnDistanceMeters(float accuracyMeters) {
        double safeAccuracyMeters = Float.isFinite(accuracyMeters) && accuracyMeters > 0f
                ? accuracyMeters
                : 0.0;
        return Math.max(5.0, safeAccuracyMeters);
    }

    private static int resolveTargetTrackIndex(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull NavTarget target
    ) {
        if (target.trackIndex >= 0) {
            return Math.min(target.trackIndex, Math.max(0, route.track.size() - 1));
        }
        int lastTrackIndex = Math.max(0, route.track.size() - 1);
        for (int i = 0; i <= lastTrackIndex; i++) {
            if (index.distanceAtPointIndex(i) + TRACK_INDEX_TOLERANCE_METERS >= target.alongTrackMeters) {
                return i;
            }
        }
        return lastTrackIndex;
    }

    private static boolean isReachedIntermediateTarget(
            @NonNull PolylineIndex index,
            @NonNull NavTarget target,
            int targetTrackIndex,
            int reachedTrackIndex
    ) {
        if (reachedTrackIndex < 0) {
            return false;
        }
        return targetTrackIndex == reachedTrackIndex
                || Math.abs(index.distanceAtPointIndex(reachedTrackIndex) - target.alongTrackMeters) <= 1.0;
    }

    private static boolean isArrivalCommand(int command) {
        return command == NavArrivalHintFactory.ARRIVAL_COMMAND
                || command == NavArrivalHintFactory.INTERMEDIATE_ARRIVAL_COMMAND;
    }

    private static int arrivalSortPriority(int command) {
        if (command == NavArrivalHintFactory.INTERMEDIATE_ARRIVAL_COMMAND) {
            return 0;
        }
        if (command == NavArrivalHintFactory.ARRIVAL_COMMAND) {
            return 2;
        }
        return 1;
    }

    private static boolean hasLaterHint(
            @NonNull List<NavUpcomingHint> upcomingHints,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index
    ) {
        double destinationDistanceMeters = index.distanceAtPointIndex(Math.max(0, route.track.size() - 1));
        for (NavUpcomingHint hint : upcomingHints) {
            if (index.distanceAtPointIndex(hint.hint.indexInTrack) < destinationDistanceMeters) {
                return true;
            }
        }
        return false;
    }
}
