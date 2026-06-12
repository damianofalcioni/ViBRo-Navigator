package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.NavigationRouteGeometryState;

final class StraightLineNavigationProgress {
    private static final float MIN_ETA_SPEED_MPS = 0.2f;

    private StraightLineNavigationProgress() {
    }

    @Nullable
    static LatLon nextTarget(
            @NonNull NavigationRequest request,
            boolean destinationReached,
            int nextStopIndex
    ) {
        if (destinationReached) {
            return null;
        }
        if (nextStopIndex < request.stops.size()) {
            return request.stops.get(nextStopIndex);
        }
        return request.destination;
    }

    @NonNull
    static List<LatLon> remainingTargetsAfterNext(
            @NonNull NavigationRequest request,
            int nextStopIndex
    ) {
        List<LatLon> remaining = new ArrayList<>();
        int firstRemainingStopAfterNext = nextStopIndex < request.stops.size()
                ? nextStopIndex + 1
                : nextStopIndex;
        for (int i = firstRemainingStopAfterNext; i < request.stops.size(); i++) {
            remaining.add(request.stops.get(i));
        }
        if (request.destination != null && nextStopIndex < request.stops.size()) {
            remaining.add(request.destination);
        }
        return remaining;
    }

    @NonNull
    static List<LatLon> remainingTargets(
            @NonNull NavigationRequest request,
            boolean destinationReached,
            int nextStopIndex
    ) {
        if (destinationReached || request.destination == null) {
            return Collections.emptyList();
        }
        List<LatLon> remaining = new ArrayList<>();
        for (int i = nextStopIndex; i < request.stops.size(); i++) {
            remaining.add(request.stops.get(i));
        }
        remaining.add(request.destination);
        return remaining;
    }

    @NonNull
    static List<LatLon> routeTrack(
            @NonNull NavigationLocation location,
            @NonNull List<LatLon> targets
    ) {
        List<LatLon> track = new ArrayList<>();
        track.add(new LatLon(location.getLatitude(), location.getLongitude()));
        track.addAll(targets);
        return track;
    }

    static double trackDistanceMeters(@NonNull List<LatLon> track) {
        double distanceMeters = 0.0;
        for (int i = 1; i < track.size(); i++) {
            distanceMeters += distanceMeters(track.get(i - 1), track.get(i));
        }
        return distanceMeters;
    }

    static double remainingDistanceToDestination(
            @NonNull NavigationRequest request,
            @NonNull NavigationLocation location,
            int nextStopIndex
    ) {
        if (request.destination == null) {
            return 0.0;
        }
        LatLon cursor = new LatLon(location.getLatitude(), location.getLongitude());
        double distanceMeters = 0.0;
        for (int i = nextStopIndex; i < request.stops.size(); i++) {
            LatLon stop = request.stops.get(i);
            distanceMeters += distanceMeters(cursor, stop);
            cursor = stop;
        }
        return distanceMeters + distanceMeters(cursor, request.destination);
    }

    static boolean isWithinReachedRadius(
            @NonNull NavigationLocation location,
            float accuracyMeters,
            @NonNull LatLon destination
    ) {
        return distanceMeters(location, destination)
                <= NavigationRouteGeometryState.resolveDestinationReachedRadiusMeters(accuracyMeters);
    }

    static double distanceMeters(
            @NonNull NavigationLocation location,
            @NonNull LatLon destination
    ) {
        return GeoMath.distanceMeters(
                location.getLatitude(),
                location.getLongitude(),
                destination.lat,
                destination.lon
        );
    }

    @Nullable
    static Double estimateSeconds(double distanceMeters, float speedMps, boolean likelyStationary) {
        if (likelyStationary || !Float.isFinite(speedMps) || speedMps < MIN_ETA_SPEED_MPS) {
            return null;
        }
        return Math.max(0.0, distanceMeters) / speedMps;
    }

    private static double distanceMeters(@NonNull LatLon from, @NonNull LatLon to) {
        return GeoMath.distanceMeters(from.lat, from.lon, to.lat, to.lon);
    }
}
