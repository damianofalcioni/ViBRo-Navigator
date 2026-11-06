package vibro.navigator.nav.simulation;

import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

/** Continuous route cursor, retained across pause and re-anchored only when geometry changes. */
final class NavigationSimulationMotion {
    @Nullable private GeoJsonRoute route;
    @Nullable private PolylineIndex index;
    private double along;
    private boolean direct;

    void reset() {
        route = null;
        index = null;
        along = 0;
        direct = false;
    }

    LatLon advance(LatLon position, NavigationSimulationPath path, double meters) {
        if (path.reached) { return position; }
        if (path.directTarget != null) {
            direct = true;
            return toward(position, path.directTarget, meters);
        }
        if (path.route == null || path.route.track.isEmpty()) { return position; }
        prepareRoute(position, path.route);
        return advanceRoute(position, path.barrier, meters);
    }

    private void prepareRoute(LatLon position, GeoJsonRoute nextRoute) {
        if (route != nextRoute || direct) {
            route = nextRoute;
            index = new PolylineIndex(route.track);
            PolylineIndex.Match match = index.match(position, -1);
            along = match == null ? 0 : match.alongTrackMeters;
            direct = false;
        }
    }

    private LatLon advanceRoute(LatLon position, @Nullable NavigationSimulationBarrier barrier, double meters) {
        if (index == null) { return position; }
        double limit = barrier == null ? index.totalLengthMeters() : barrier.alongTrackMeters;
        if (barrier != null && along >= limit) { return toward(position, barrier.position, meters); }
        return advanceAlongTrack(position, meters, limit, barrier);
    }

    private LatLon advanceAlongTrack(LatLon position, double meters, double limit,
            @Nullable NavigationSimulationBarrier barrier) {
        LatLon anchor = index.pointAtDistance(along);
        if (anchor == null) { return position; }
        double approach = distance(position, anchor);
        if (approach > 1) { return toward(position, anchor, meters); }
        double before = along;
        along = Math.min(Math.max(along, limit), along + Math.max(0, meters - approach));
        LatLon next = index.pointAtDistance(along);
        if (next == null) { return position; }
        if (barrier != null && along >= limit) {
            return toward(next, barrier.position, Math.max(0, meters - approach - (along - before)));
        }
        return next;
    }

    static LatLon toward(LatLon from, LatLon to, double meters) {
        double distance = distance(from, to);
        if (distance <= meters || distance == 0) { return to; }
        double fraction = meters / distance;
        return new LatLon(from.lat + (to.lat - from.lat) * fraction,
                from.lon + (to.lon - from.lon) * fraction);
    }

    static double distance(LatLon from, LatLon to) {
        return GeoMath.distanceMeters(from.lat, from.lon, to.lat, to.lon);
    }
}
