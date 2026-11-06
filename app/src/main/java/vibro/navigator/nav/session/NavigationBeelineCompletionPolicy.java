package vibro.navigator.nav.session;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.GeoJsonRoute;

/** Waiting inside reach must neither complete an unconfirmed target nor invent spur travel. */
final class NavigationBeelineCompletionPolicy {
    private static final long DIRECT_GUIDANCE_INTERVAL_MS = 3_000L;
    private final NavigationRouteDirectGuidanceState state;
    private final NavigationIntermediateArrivalTracker intermediate;
    private final NavigationArrivalDetector arrival;
    private final NavigationTurnState turnState;

    NavigationBeelineCompletionPolicy(NavigationRouteDirectGuidanceState state,
            NavigationIntermediateArrivalTracker intermediate, NavigationArrivalDetector arrival,
            NavigationTurnState turnState) {
        this.state = state;
        this.intermediate = intermediate;
        this.arrival = arrival;
        this.turnState = turnState;
    }

    Completion completeReachable(NavigationLocation location, double radius, float accuracy, long nowMs) {
        Completion completed = new Completion();
        while (state.isRouteBeelineActive()) {
            if (!verifyTarget(location, accuracy, nowMs)) {
                return completed;
            }
            LatLon target = state.activeDirectTarget();
            PolylineIndex.Match next = state.completeRouteBeelineIfReached(location, radius);
            if (next == null) {
                return completed;
            }
            completed.match = next;
            consumeCompletedIntermediate(completed, target, location, accuracy, nowMs);
        }
        return completed;
    }

    private void consumeCompletedIntermediate(Completion completed, LatLon target,
            NavigationLocation location, float accuracy, long nowMs) {
        if (target == null || !intermediate.isCurrentTarget(target)) {
            return;
        }
        Integer reached = intermediate.reachedTrackIndex(location, accuracy, nowMs);
        if (reached != null) {
            // Advance the required-stop barrier before considering another leg.
            // Its next target must gather its own independently timed confirmation.
            completed.turnEvents.addAll(turnState.onIntermediateDestinationReached(reached));
        }
    }

    @Nullable
    NavigationRouteEvaluation arrivalEvaluation(Completion completed, NavigationLocation location,
            float accuracy, long nowMs, GeoJsonRoute route) {
        if (!completed.turnEvents.isEmpty()) {
            return NavigationRouteEvaluation.keepRoute(completed.turnEvents, DIRECT_GUIDANCE_INTERVAL_MS,
                    !state.isRouteBeelineActive());
        }
        if (arrival.isDestinationReached(location, accuracy, completed.match, nowMs)) {
            return NavigationRouteEvaluation.keepRoute(turnState.onDestinationReached(route), -1L, true);
        }
        Integer reached = intermediate.reachedTrackIndex(location, accuracy, nowMs);
        return reached == null ? null : NavigationRouteEvaluation.keepRoute(
                turnState.onIntermediateDestinationReached(reached), DIRECT_GUIDANCE_INTERVAL_MS,
                !state.isRouteBeelineActive());
    }

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

    private boolean verifyTarget(NavigationLocation location, float accuracy, long nowMs) {
        LatLon target = state.activeDirectTarget();
        return target != null && intermediate.verifyBeelineTarget(location, accuracy, target, nowMs)
                && arrival.verifyBeelineTarget(location, accuracy, target, state.activeRouteBeelineTargetMatch(), nowMs);
    }

    static final class Completion {
        PolylineIndex.Match match;
        final List<NavigationTurnEvent> turnEvents = new ArrayList<>();
    }
}
