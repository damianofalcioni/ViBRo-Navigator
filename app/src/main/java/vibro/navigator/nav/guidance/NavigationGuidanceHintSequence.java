package vibro.navigator.nav.guidance;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

final class NavigationGuidanceHintSequence {
    @NonNull
    private final List<VoiceHint> hints = new ArrayList<>();
    @NonNull
    private final List<Double> hintAlongTrackMeters = new ArrayList<>();
    @NonNull
    private final List<Integer> routeHintIndexes = new ArrayList<>();
    private int nextIndex;

    void reset() {
        hints.clear();
        hintAlongTrackMeters.clear();
        routeHintIndexes.clear();
        nextIndex = 0;
    }

    void onRouteApplied(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<LatLon> intermediateStops,
            @Nullable NavigationLocation currentLocation
    ) {
        onRouteApplied(route, polylineIndex, intermediateStops, toLatLon(currentLocation));
    }

    void onRouteApplied(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<LatLon> intermediateStops,
            @Nullable LatLon currentLocation
    ) {
        replaceWith(NavigationGuidanceHintSequenceBuilder.build(route, polylineIndex, intermediateStops));
        nextIndex = findNextHintIndex(polylineIndex, currentLocation);
    }

    @NonNull
    List<VoiceHint> hints() {
        return hints;
    }

    @NonNull
    List<Double> hintAlongTrackMeters() {
        return hintAlongTrackMeters;
    }

    int nextIndex() {
        return nextIndex;
    }

    void advanceTo(int nextIndex) {
        this.nextIndex = Math.max(0, Math.min(nextIndex, hints.size()));
    }

    void advanceToEnd() {
        nextIndex = hints.size();
    }

    void advancePastIntermediateDestination(int trackIndex) {
        while (nextIndex < hints.size() && isPassedByIntermediateArrival(hints.get(nextIndex), trackIndex)) {
            nextIndex++;
        }
    }

    int nextRouteHintIndex(int fallbackRouteHintCount) {
        for (int i = nextIndex; i < routeHintIndexes.size(); i++) {
            int routeHintIndex = routeHintIndexes.get(i);
            if (routeHintIndex >= 0) {
                return routeHintIndex;
            }
        }
        return fallbackRouteHintCount;
    }

    @Nullable
    VoiceHint nextHint() {
        if (nextIndex < 0 || nextIndex >= hints.size()) {
            return null;
        }
        return hints.get(nextIndex);
    }

    @Nullable
    Integer nextTurnManeuverDegrees() {
        VoiceHint nextHint = nextTurnManeuverHint();
        return nextHint == null ? null : nextHint.angleDegrees;
    }

    @Nullable
    Integer nextTurnManeuverTrackIndex() {
        VoiceHint nextHint = nextTurnManeuverHint();
        return nextHint == null ? null : nextHint.indexInTrack;
    }

    @Nullable
    Double nextAlongTrackMeters() {
        if (nextIndex < 0 || nextIndex >= hintAlongTrackMeters.size()) {
            return null;
        }
        return hintAlongTrackMeters.get(nextIndex);
    }

    @Nullable
    private VoiceHint nextTurnManeuverHint() {
        VoiceHint nextHint = nextHint();
        return nextHint == null || NavigationTurnManeuverCueState.isSyntheticArrivalHint(nextHint)
                ? null
                : nextHint;
    }

    private void replaceWith(@NonNull NavigationGuidanceHintSequenceBuilder.BuiltHints builtHints) {
        hints.clear();
        hintAlongTrackMeters.clear();
        routeHintIndexes.clear();
        hints.addAll(builtHints.hints);
        hintAlongTrackMeters.addAll(builtHints.alongTrackMeters);
        routeHintIndexes.addAll(builtHints.routeHintIndexes);
    }

    private int findNextHintIndex(@NonNull PolylineIndex polylineIndex, @Nullable LatLon currentLocation) {
        if (currentLocation == null || hints.isEmpty()) {
            return 0;
        }

        PolylineIndex.Match match = polylineIndex.match(currentLocation, -1);
        if (match == null) {
            return 0;
        }

        for (int i = 0; i < hints.size(); i++) {
            if (hintAlongTrackMeters.get(i) + 5.0 > match.alongTrackMeters) {
                return i;
            }
        }
        return hints.size();
    }

    @Nullable
    private static LatLon toLatLon(@Nullable NavigationLocation location) {
        return location == null ? null : new LatLon(location.getLatitude(), location.getLongitude());
    }

    private static boolean isPassedByIntermediateArrival(@NonNull VoiceHint hint, int trackIndex) {
        return hint.indexInTrack < trackIndex || (hint.command == 101 && hint.indexInTrack <= trackIndex);
    }
}
