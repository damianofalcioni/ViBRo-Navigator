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
    final NavigationRouteHistory routeHistory = new NavigationRouteHistory();
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
    final NavigationRouteDirectGuidance directGuidance =
            new NavigationRouteDirectGuidance(
                    geometryState,
                    turnState,
                    progressTracker,
                    deviationHandler,
                    arrivalDetector,
                    intermediateArrivalTracker,
                    routeHistory
            );
    @NonNull
    final NavigationRouteEvaluator routeEvaluator = new NavigationRouteEvaluator(
            geometryState,
            turnState,
            progressTracker,
            deviationHandler,
            displayState,
            arrivalDetector,
            intermediateArrivalTracker,
            directGuidance.evaluator(),
            routeHistory
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
            turnState,
            arrivalDetector,
            intermediateArrivalTracker,
            directGuidance.state(),
            routeHistory
    );

    void reset() {
        geometryState.reset();
        displayState.reset();
        deviationHandler.clearDeviationEvidence();
        progressTracker.reset();
        routeHistory.reset();
        blockedRouteState.reset();
        turnState.reset();
        intermediateArrivalTracker.reset();
        directGuidance.reset();
    }
}
