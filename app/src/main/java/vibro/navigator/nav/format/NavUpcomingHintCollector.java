package vibro.navigator.nav.format;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.nav.guidance.RouteTimeEstimator;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

final class NavUpcomingHintCollector {
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
        List<NavUpcomingHint> upcomingHints = new ArrayList<>(Math.max(0, maxHintCount));
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
                accuracyMeters,
                maxHintCount
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
        return upcomingHints;
    }

    private static void addRouteHints(
            @NonNull List<NavUpcomingHint> upcomingHints,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int startHintIdx,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters,
            int maxHintCount
    ) {
        double minReliableDistanceMeters = minimumReliableTurnDistanceMeters(accuracyMeters);
        for (int i = startHintIdx; i < route.voiceHints.size() && upcomingHints.size() < maxHintCount; i++) {
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
        if (distanceMeters <= minReliableDistanceMeters && hint.command != NavArrivalHintFactory.ARRIVAL_COMMAND) {
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
        if (upcomingHints.size() >= maxHintCount) {
            return;
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
}
