package vibro.navigator.nav.session;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.simulation.NavigationSimulationPath;

/** Read-only simulation view of the same targets used by normal navigation. */
public final class NavigationSessionSimulation {
    private NavigationSessionSimulation() { }

    public static NavigationSimulationPath path(NavigationSession session) {
        NavigationRequest request = session.currentNavigationRequest();
        if (request.routingMode != NavigationRoutingMode.STRAIGHT_LINE) {
            NavigationSessionRouteState state = session.components.routeState;
            return new NavigationSimulationPath(state.currentRoute(), state.components.directGuidance.activeTarget(),
                    state.components.intermediateArrivalTracker.nextSimulationBarrier(), state.isDestinationReached(),
                    state.components.directGuidance.minimumRouteSegmentIndex());
        }
        StraightLineNavigationState state = session.components.straightLineState;
        LatLon target = StraightLineNavigationProgress.nextTarget(request,
                state.isDestinationReachedForDisplay(), state.nextStopIndexForDisplay());
        return new NavigationSimulationPath(null, target, null, state.isDestinationReachedForDisplay());
    }
}
