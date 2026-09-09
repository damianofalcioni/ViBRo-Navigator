package vibro.navigator.nav.session;

import java.util.List;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.BeelineRecoveryRouteValidator;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

/** Captures the live leg and plan so late candidates cannot replace unrelated guidance. */
final class NavigationBeelineRecoveryRequests {
    private final NavigationSession session;
    private int requestToken = -1;
    private long context = -1;
    private NavigationRequest plan;

    NavigationBeelineRecoveryRequests(NavigationSession session) {
        this.session = session;
    }

    boolean canPrepare(long nowMs) {
        NavigationBeelineRecoveryState state = session.components.routeState.beelineRecovery();
        return session.started && !session.paused && !session.currentRequest.isRoundTrip()
                && !session.currentRequest.isStraightLine() && state.isCurrent(state.context(), nowMs);
    }

    void remember(NavigationRouteRequestSnapshot snapshot, long nowMs) {
        requestToken = snapshot.requestToken;
        NavigationBeelineRecoveryState state = session.components.routeState.beelineRecovery();
        context = state.context();
        plan = session.currentRequest;
        state.recordAttempt(session.components.locationState.getLastFilteredLocation(), nowMs);
    }

    boolean isFor(NavigationRouteRequestSnapshot snapshot) {
        return snapshot.speculative && snapshot.requestToken == requestToken;
    }

    boolean isActive() {
        return requestToken >= 0;
    }

    void clear() {
        requestToken = -1;
        context = -1;
        plan = null;
    }

    boolean accepts(NavigationRouteRequestSnapshot snapshot, GeoJsonRoute route, long nowMs) {
        if (!canPrepare(nowMs) || plan != session.currentRequest
                || !session.components.routeState.beelineRecovery().isCurrent(context, nowMs)) {
            return false;
        }
        List<LatLon> remaining = session.components.routeState.remainingIntermediateStops(plan.stops);
        return sameStops(snapshot.intermediates, remaining)
                && BeelineRecoveryRouteValidator.isUsable(route, snapshot,
                        session.components.locationState.getLastFilteredLocation());
    }

    private static boolean sameStops(List<LatLon> first, List<LatLon> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i).lat != second.get(i).lat || first.get(i).lon != second.get(i).lon) {
                return false;
            }
        }
        return true;
    }
}
