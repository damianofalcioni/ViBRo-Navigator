package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.NavigationRouteDeviationHandler;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

final class NavigationRouteDirectGuidance {
    @NonNull
    private final NavigationRouteGeometryState geometryState;
    @NonNull
    private final NavigationRouteDirectGuidanceState state =
            new NavigationRouteDirectGuidanceState();
    @NonNull
    private final NavigationRouteDirectGuidanceEvaluator evaluator;

    NavigationRouteDirectGuidance(
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @NonNull NavigationRouteDeviationHandler deviationHandler,
            @NonNull NavigationArrivalDetector arrivalDetector,
            @NonNull NavigationIntermediateArrivalTracker intermediateArrivalTracker,
            @NonNull NavigationRouteHistory routeHistory
    ) {
        this.geometryState = geometryState;
        evaluator = new NavigationRouteDirectGuidanceEvaluator(
                geometryState,
                turnState,
                progressTracker,
                deviationHandler,
                arrivalDetector,
                intermediateArrivalTracker,
                state,
                routeHistory
        );
    }

    void reset() {
        state.reset();
    }

    @NonNull
    NavigationRouteDirectGuidanceState state() {
        return state;
    }

    @NonNull
    NavigationRouteDirectGuidanceEvaluator evaluator() {
        return evaluator;
    }

    @Nullable
    LatLon activeTarget() {
        return state.activeDirectTarget();
    }

    @Nullable
    PolylineIndex.Match resolveRouteMatch(
            @Nullable NavigationLocation location,
            float accuracyMeters
    ) {
        PolylineIndex.Match activeMatch = state.activeRouteBeelineProgressMatch();
        if (activeMatch != null || location == null) {
            return activeMatch;
        }
        PolylineIndex.Match fallbackMatch = geometryState.match(location, accuracyMeters);
        return fallbackMatch == null
                ? null
                : state.constrainRouteMatch(location, fallbackMatch);
    }

    @Nullable
    Double bearingDegreesFrom(@Nullable NavigationLocation location) {
        return state.bearingDegreesFrom(location);
    }

    @Nullable
    Double expectedRouteBearingDegrees(
            @Nullable NavigationLocation location,
            float accuracyMeters
    ) {
        PolylineIndex.Match match = resolveRouteMatch(location, accuracyMeters);
        return match == null ? null : geometryState.expectedBearingDegrees(match);
    }
}
