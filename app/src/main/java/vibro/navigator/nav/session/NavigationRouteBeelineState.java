package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.session.NavigationRouteBeelineLegs.Leg;

final class NavigationRouteBeelineState {
    @NonNull
    private NavigationRouteBeelineLegs legs = NavigationRouteBeelineLegs.empty();
    @Nullable
    private PolylineIndex polylineIndex;
    private int nextLegIndex;
    private int minimumRouteSegmentIndex = -1;
    @Nullable
    private Leg activeLeg;

    void reset() {
        legs = NavigationRouteBeelineLegs.empty();
        polylineIndex = null;
        nextLegIndex = 0;
        minimumRouteSegmentIndex = -1;
        activeLeg = null;
    }

    void onRouteApplied(@NonNull GeoJsonRoute route, @NonNull PolylineIndex polylineIndex) {
        reset();
        this.polylineIndex = polylineIndex;
        legs = NavigationRouteBeelineLegs.from(route);
    }

    boolean activateIfReached(
            @NonNull PolylineIndex.Match routeMatch,
            @NonNull NavigationLocation location,
            double reachedRadiusMeters
    ) {
        if (activeLeg != null || nextLegIndex >= legs.size() || polylineIndex == null) {
            return activeLeg != null;
        }
        skipPassedLegs(routeMatch, reachedRadiusMeters);
        if (nextLegIndex >= legs.size()) {
            return false;
        }
        Leg candidate = legs.get(nextLegIndex);
        if (!isAtLegStart(candidate, routeMatch, location, reachedRadiusMeters)) {
            return false;
        }
        activeLeg = candidate;
        return true;
    }

    @Nullable
    PolylineIndex.Match completeIfReached(
            @NonNull NavigationLocation location,
            double reachedRadiusMeters
    ) {
        if (activeLeg == null || polylineIndex == null || distanceToTargetMeters(location) > reachedRadiusMeters) {
            return null;
        }
        Leg completed = activeLeg;
        nextLegIndex++;
        minimumRouteSegmentIndex = Math.max(
                minimumRouteSegmentIndex,
                completed.targetTrackIndex
        );
        activeLeg = null;
        activateFollowingLeg(completed.targetTrackIndex);
        return matchAtTarget(completed);
    }

    boolean isActive() {
        return activeLeg != null;
    }

    @Nullable
    LatLon target() {
        if (activeLeg == null) {
            return null;
        }
        return new LatLon(activeLeg.target.lat, activeLeg.target.lon);
    }

    @Nullable
    PolylineIndex.Match progressMatch() {
        if (activeLeg == null || polylineIndex == null) {
            return null;
        }
        return matchAtStart(activeLeg);
    }

    @NonNull
    PolylineIndex.Match constrainRouteMatch(
            @NonNull NavigationLocation location,
            @NonNull PolylineIndex.Match fallbackMatch
    ) {
        if (polylineIndex == null || minimumRouteSegmentIndex < 0) {
            return fallbackMatch;
        }
        PolylineIndex.Match constrained = polylineIndex.matchFromSegmentIndex(
                new LatLon(location.getLatitude(), location.getLongitude()),
                minimumRouteSegmentIndex
        );
        return constrained != null ? constrained : fallbackMatch;
    }

    @Nullable
    Double bearingDegreesFrom(@Nullable NavigationLocation location) {
        if (location == null || activeLeg == null) {
            return null;
        }
        return GeoMath.bearingDegrees(
                location.getLatitude(),
                location.getLongitude(),
                activeLeg.target.lat,
                activeLeg.target.lon
        );
    }

    private void skipPassedLegs(@NonNull PolylineIndex.Match routeMatch, double reachedRadiusMeters) {
        while (nextLegIndex < legs.size()) {
            Leg candidate = legs.get(nextLegIndex);
            double targetDistance = polylineIndex.distanceAtPointIndex(candidate.targetTrackIndex);
            if (routeMatch.alongTrackMeters <= targetDistance + reachedRadiusMeters) {
                return;
            }
            nextLegIndex++;
        }
    }

    private boolean isAtLegStart(
            @NonNull Leg candidate,
            @NonNull PolylineIndex.Match routeMatch,
            @NonNull NavigationLocation location,
            double reachedRadiusMeters
    ) {
        if (distanceMeters(location, candidate.start) <= reachedRadiusMeters) {
            return true;
        }
        double startDistance = polylineIndex.distanceAtPointIndex(candidate.startTrackIndex);
        double targetDistance = polylineIndex.distanceAtPointIndex(candidate.targetTrackIndex);
        return routeMatch.distanceToTrackMeters <= reachedRadiusMeters
                && routeMatch.alongTrackMeters + reachedRadiusMeters >= startDistance
                && routeMatch.alongTrackMeters <= targetDistance + reachedRadiusMeters;
    }

    private void activateFollowingLeg(int completedTargetTrackIndex) {
        if (nextLegIndex >= legs.size()) {
            return;
        }
        Leg next = legs.get(nextLegIndex);
        if (next.startTrackIndex == completedTargetTrackIndex) {
            activeLeg = next;
        }
    }

    @NonNull
    private PolylineIndex.Match matchAtStart(@NonNull Leg leg) {
        return new PolylineIndex.Match(
                0.0,
                polylineIndex.distanceAtPointIndex(leg.startTrackIndex),
                leg.bearingDegrees,
                leg.startTrackIndex
        );
    }

    @NonNull
    private PolylineIndex.Match matchAtTarget(@NonNull Leg leg) {
        return new PolylineIndex.Match(
                0.0,
                polylineIndex.distanceAtPointIndex(leg.targetTrackIndex),
                leg.bearingDegrees,
                leg.targetTrackIndex
        );
    }

    private double distanceToTargetMeters(@NonNull NavigationLocation location) {
        return activeLeg == null ? Double.POSITIVE_INFINITY : distanceMeters(location, activeLeg.target);
    }

    private static double distanceMeters(
            @NonNull NavigationLocation location,
            @NonNull LatLon point
    ) {
        return GeoMath.distanceMeters(
                location.getLatitude(),
                location.getLongitude(),
                point.lat,
                point.lon
        );
    }

}
