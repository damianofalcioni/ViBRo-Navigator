package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.geo.GeoMath;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.guidance.NavigationArrivalConfirmation;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.simulation.NavigationSimulationBarrier;

final class NavigationIntermediateArrivalTracker {
    private static final double TRACK_INDEX_TOLERANCE_METERS = 1.0;

    @NonNull
    private final List<IntermediateDestination> destinations = new ArrayList<>();
    private int nextDestinationIndex;
    private boolean routeApplied;
    private final NavigationArrivalConfirmation arrivalConfirmation = new NavigationArrivalConfirmation();

    void reset() {
        destinations.clear();
        nextDestinationIndex = 0;
        routeApplied = false;
        arrivalConfirmation.reset();
    }

    void clearArrivalEvidence() {
        arrivalConfirmation.reset();
    }

    void onRouteApplied(
            @NonNull List<LatLon> stops,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex
    ) {
        destinations.clear();
        arrivalConfirmation.reset();
        for (LatLon stop : stops) {
            PolylineIndex.Match match = polylineIndex.match(stop, -1);
            if (match != null) {
                destinations.add(new IntermediateDestination(
                        stop,
                        trackIndexAtOrAfter(polylineIndex, route.track.size(), match.alongTrackMeters),
                        match.alongTrackMeters
                ));
            }
        }
        nextDestinationIndex = 0;
        routeApplied = true;
    }

    @Nullable
    Integer reachedTrackIndex(@NonNull NavigationLocation currentLocation, float accuracyMeters, long nowMs) {
        if (nextDestinationIndex >= destinations.size()) {
            return null;
        }
        IntermediateDestination destination = destinations.get(nextDestinationIndex);
        if (!arrivalConfirmation.observe(currentLocation, accuracyMeters, destination.location, nowMs)) {
            return null;
        }
        nextDestinationIndex++;
        arrivalConfirmation.reset();
        return destination.trackIndex;
    }

    @Nullable
    NavigationSimulationBarrier nextSimulationBarrier() {
        return hasRemainingDestinations() ? destinations.get(nextDestinationIndex).simulationBarrier : null;
    }

    boolean hasRemainingDestinations() {
        return nextDestinationIndex < destinations.size();
    }

    boolean verifyBeelineTarget(NavigationLocation location, float accuracy, LatLon target, long nowMs) {
        return !isCurrentTarget(target) || arrivalConfirmation.observe(location, accuracy,
                destinations.get(nextDestinationIndex).location, nowMs);
    }

    boolean isCurrentTarget(LatLon target) {
        if (nextDestinationIndex >= destinations.size()) {
            return false;
        }
        LatLon stop = destinations.get(nextDestinationIndex).location;
        return GeoMath.distanceMeters(stop.lat, stop.lon, target.lat, target.lon) <= 1;
    }

    @Nullable
    Integer skipCurrentAtTrackIndex(int trackIndex) {
        if (nextDestinationIndex >= destinations.size()) {
            return null;
        }
        IntermediateDestination destination = destinations.get(nextDestinationIndex);
        if (destination.trackIndex != trackIndex) {
            return null;
        }
        nextDestinationIndex++;
        arrivalConfirmation.reset();
        return destination.trackIndex;
    }

    @NonNull
    List<LatLon> remainingStops(@NonNull List<LatLon> fallbackStops) {
        if (!routeApplied) {
            return new ArrayList<>(fallbackStops);
        }
        List<LatLon> remaining = new ArrayList<>();
        for (int i = nextDestinationIndex; i < destinations.size(); i++) {
            remaining.add(destinations.get(i).location);
        }
        return remaining;
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

    private static final class IntermediateDestination {
        @NonNull
        final LatLon location;
        final int trackIndex;
        final NavigationSimulationBarrier simulationBarrier;

        IntermediateDestination(@NonNull LatLon location, int trackIndex, double alongTrackMeters) {
            this.location = location;
            this.trackIndex = trackIndex;
            simulationBarrier = new NavigationSimulationBarrier(location, alongTrackMeters);
        }
    }
}
