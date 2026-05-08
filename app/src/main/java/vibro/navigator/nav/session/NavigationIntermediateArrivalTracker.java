package vibro.navigator.nav.session;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

final class NavigationIntermediateArrivalTracker {

    @NonNull
    private final List<IntermediateDestination> destinations = new ArrayList<>();
    private int nextDestinationIndex;
    private boolean routeApplied;

    void reset() {
        destinations.clear();
        nextDestinationIndex = 0;
        routeApplied = false;
    }

    void onRouteApplied(
            @NonNull List<LatLon> stops,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex
    ) {
        destinations.clear();
        for (LatLon stop : stops) {
            PolylineIndex.Match match = polylineIndex.match(stop, -1);
            if (match != null) {
                destinations.add(new IntermediateDestination(
                        stop,
                        trackIndexAtOrAfter(polylineIndex, route.track.size(), match.alongTrackMeters)
                ));
            }
        }
        nextDestinationIndex = 0;
        routeApplied = true;
    }

    @Nullable
    Integer reachedTrackIndex(@NonNull Location location, float accuracyMeters) {
        if (nextDestinationIndex >= destinations.size()) {
            return null;
        }
        IntermediateDestination destination = destinations.get(nextDestinationIndex);
        if (!isWithinReachedRadius(location, accuracyMeters, destination.location)) {
            return null;
        }
        nextDestinationIndex++;
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

    private static boolean isWithinReachedRadius(
            @NonNull Location location,
            float accuracyMeters,
            @NonNull LatLon destination
    ) {
        double distanceMeters = GeoMath.distanceMeters(
                location.getLatitude(),
                location.getLongitude(),
                destination.lat,
                destination.lon
        );
        return distanceMeters <= NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(accuracyMeters);
    }

    private static int trackIndexAtOrAfter(
            @NonNull PolylineIndex polylineIndex,
            int trackSize,
            double alongTrackMeters
    ) {
        int lastTrackIndex = Math.max(0, trackSize - 1);
        for (int i = 0; i <= lastTrackIndex; i++) {
            if (polylineIndex.distanceAtPointIndex(i) >= alongTrackMeters) {
                return i;
            }
        }
        return lastTrackIndex;
    }

    private static final class IntermediateDestination {
        @NonNull
        final LatLon location;
        final int trackIndex;

        IntermediateDestination(@NonNull LatLon location, int trackIndex) {
            this.location = location;
            this.trackIndex = trackIndex;
        }
    }
}
