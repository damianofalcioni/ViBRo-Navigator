package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import vibro.navigator.nav.guidance.NavigationBlockedRouteState;
import vibro.navigator.nav.guidance.NavigationRouteDeviationHandler;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.route.NavigationRouteGeometryState;

final class NavigationSessionRouteComponents {
    @NonNull
    final NavigationRouteGeometryState geometryState = new NavigationRouteGeometryState();
    @NonNull
    final NavigationBlockedRouteState blockedRouteState = new NavigationBlockedRouteState();
    @NonNull
    final NavigationTurnState turnState = new NavigationTurnState();
    @NonNull
    final NavigationRouteProgressTracker progressTracker = new NavigationRouteProgressTracker();
    @NonNull
    final NavigationRouteDeviationHandler deviationHandler =
            new NavigationRouteDeviationHandler(progressTracker);
    @NonNull
    final NavigationSessionRouteDisplayState displayState = new NavigationSessionRouteDisplayState();
    @NonNull
    final NavigationArrivalDetector arrivalDetector = new NavigationArrivalDetector(geometryState);
    @NonNull
    final NavigationIntermediateArrivalTracker intermediateArrivalTracker =
            new NavigationIntermediateArrivalTracker();
    @NonNull
    final RouteStartApproachState routeStartApproachState = new RouteStartApproachState();
    @NonNull
    final NavigationRouteEvaluator routeEvaluator = new NavigationRouteEvaluator(
            geometryState,
            turnState,
            progressTracker,
            deviationHandler,
            displayState,
            arrivalDetector,
            intermediateArrivalTracker,
            routeStartApproachState
    );
    @NonNull
    final NavigationBlockedPointSelector blockedPointSelector = new NavigationBlockedPointSelector(
            geometryState,
            blockedRouteState
    );
    @NonNull
    final NavigationRouteResultApplier routeResultApplier = new NavigationRouteResultApplier(
            geometryState,
            displayState,
            deviationHandler,
            progressTracker,
            turnState,
            arrivalDetector,
            intermediateArrivalTracker,
            routeStartApproachState
    );

    void reset() {
        geometryState.reset();
        displayState.reset();
        deviationHandler.clearDeviationEvidence();
        progressTracker.reset();
        blockedRouteState.reset();
        turnState.reset();
        intermediateArrivalTracker.reset();
        routeStartApproachState.reset();
    }
}
