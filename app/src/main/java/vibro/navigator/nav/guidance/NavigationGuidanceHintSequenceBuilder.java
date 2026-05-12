package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

final class NavigationGuidanceHintSequenceBuilder {
    private static final double TRACK_INDEX_TOLERANCE_METERS = 1.0;

    private NavigationGuidanceHintSequenceBuilder() {
    }

    @NonNull
    static BuiltHints build(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<LatLon> intermediateStops
    ) {
        List<GuidanceHintCandidate> candidates = new ArrayList<>();
        addRouteHints(candidates, route, polylineIndex);
        addIntermediateArrivalHints(candidates, route, polylineIndex, intermediateStops);
        Collections.sort(candidates, new Comparator<GuidanceHintCandidate>() {
            @Override
            public int compare(GuidanceHintCandidate first, GuidanceHintCandidate second) {
                int distanceComparison = Double.compare(first.alongTrackMeters, second.alongTrackMeters);
                if (distanceComparison != 0) {
                    return distanceComparison;
                }
                return Integer.compare(first.sortPriority, second.sortPriority);
            }
        });
        return toBuiltHints(candidates);
    }

    private static void addRouteHints(
            @NonNull List<GuidanceHintCandidate> candidates,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex
    ) {
        for (int i = 0; i < route.voiceHints.size(); i++) {
            VoiceHint hint = route.voiceHints.get(i);
            candidates.add(new GuidanceHintCandidate(
                    hint,
                    i,
                    polylineIndex.distanceAtPointIndex(hint.indexInTrack),
                    arrivalSortPriority(hint.command)
            ));
        }
    }

    private static void addIntermediateArrivalHints(
            @NonNull List<GuidanceHintCandidate> candidates,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<LatLon> intermediateStops
    ) {
        for (LatLon stop : intermediateStops) {
            PolylineIndex.Match match = polylineIndex.match(stop, -1);
            if (match != null) {
                addIntermediateArrivalHint(candidates, route, polylineIndex, match);
            }
        }
    }

    private static void addIntermediateArrivalHint(
            @NonNull List<GuidanceHintCandidate> candidates,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull PolylineIndex.Match match
    ) {
        int trackIndex = trackIndexAtOrAfter(polylineIndex, route.track.size(), match.alongTrackMeters);
        candidates.add(new GuidanceHintCandidate(
                new VoiceHint(trackIndex, 101, 0, 0.0, 0),
                -1,
                match.alongTrackMeters,
                arrivalSortPriority(101)
        ));
    }

    @NonNull
    private static BuiltHints toBuiltHints(@NonNull List<GuidanceHintCandidate> candidates) {
        List<VoiceHint> hints = new ArrayList<>(candidates.size());
        List<Double> alongTrackMeters = new ArrayList<>(candidates.size());
        List<Integer> routeHintIndexes = new ArrayList<>(candidates.size());
        for (GuidanceHintCandidate candidate : candidates) {
            hints.add(candidate.hint);
            alongTrackMeters.add(candidate.alongTrackMeters);
            routeHintIndexes.add(candidate.routeHintIndex);
        }
        return new BuiltHints(hints, alongTrackMeters, routeHintIndexes);
    }

    private static int trackIndexAtOrAfter(
            @NonNull PolylineIndex polylineIndex,
            int trackSize,
            double alongTrackMeters
    ) {
        int lastTrackIndex = Math.max(0, trackSize - 1);
        for (int i = 0; i <= lastTrackIndex; i++) {
            if (polylineIndex.distanceAtPointIndex(i) + TRACK_INDEX_TOLERANCE_METERS >= alongTrackMeters) {
                return i;
            }
        }
        return lastTrackIndex;
    }

    private static int arrivalSortPriority(int command) {
        if (command == 101) {
            return 0;
        }
        if (command == 100) {
            return 2;
        }
        return 1;
    }

    static final class BuiltHints {
        @NonNull
        final List<VoiceHint> hints;
        @NonNull
        final List<Double> alongTrackMeters;
        @NonNull
        final List<Integer> routeHintIndexes;

        BuiltHints(
                @NonNull List<VoiceHint> hints,
                @NonNull List<Double> alongTrackMeters,
                @NonNull List<Integer> routeHintIndexes
        ) {
            this.hints = hints;
            this.alongTrackMeters = alongTrackMeters;
            this.routeHintIndexes = routeHintIndexes;
        }
    }

    private static final class GuidanceHintCandidate {
        @NonNull
        final VoiceHint hint;
        final int routeHintIndex;
        final double alongTrackMeters;
        final int sortPriority;

        GuidanceHintCandidate(
                @NonNull VoiceHint hint,
                int routeHintIndex,
                double alongTrackMeters,
                int sortPriority
        ) {
            this.hint = hint;
            this.routeHintIndex = routeHintIndex;
            this.alongTrackMeters = alongTrackMeters;
            this.sortPriority = sortPriority;
        }
    }
}
