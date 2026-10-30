package vibro.navigator.nav.session;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.PolylineIndex;

/** Waiting inside reach must neither complete an unconfirmed target nor invent spur travel. */
final class NavigationBeelineCompletionPolicy {
    private NavigationBeelineCompletionPolicy() { }

    static PolylineIndex.Match travelledMatch(PolylineIndex.Match match, PolylineIndex.Match boundary) {
        return match.alongTrackMeters <= boundary.alongTrackMeters ? match : boundary;
    }

    static boolean shouldHoldUntravelled(NavigationRouteDirectGuidanceState state,
            NavigationRouteHistory history, NavigationLocation location, double radiusMeters) {
        LatLon target = state.activeDirectTarget();
        return !history.hasPendingRerouteFixPath() && target != null
                && RouteStartApproach.distanceMeters(new LatLon(location.getLatitude(), location.getLongitude()), target)
                        <= radiusMeters;
    }

    static boolean verifyTarget(NavigationRouteDirectGuidanceState state,
            NavigationIntermediateArrivalTracker intermediate, NavigationArrivalDetector arrival,
            NavigationLocation location, float accuracy, long nowMs) {
        LatLon target = state.activeDirectTarget();
        return target != null && intermediate.verifyBeelineTarget(location, accuracy, target, nowMs)
                && arrival.verifyBeelineTarget(location, accuracy, target, state.activeRouteBeelineTargetMatch(), nowMs);
    }
}
