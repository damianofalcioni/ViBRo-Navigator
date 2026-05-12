package vibro.navigator.nav.session;


import vibro.navigator.nav.guidance.NavigationBlockedRouteState;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationRouteDeviationHandler;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.geo.LatLon;
import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

import java.util.Collections;
import java.util.List;

// Route coordinator: route safety policies stay split across named helpers even though they meet in this class.
@SuppressWarnings("PMD.CouplingBetweenObjects")
public final class NavigationSessionRouteState {

    private static final long NO_SUGGESTED_INTERVAL = -1L;

    private final NavigationRouteGeometryState geometryState = new NavigationRouteGeometryState();
    private final NavigationBlockedRouteState blockedRouteState = new NavigationBlockedRouteState();
    private final NavigationTurnState turnState = new NavigationTurnState();
    private final NavigationRouteProgressTracker progressTracker = new NavigationRouteProgressTracker();
    private final NavigationRouteDeviationHandler deviationHandler =
            new NavigationRouteDeviationHandler(progressTracker);
    private final NavigationSessionRouteDisplayState displayState = new NavigationSessionRouteDisplayState();
    private final NavigationArrivalDetector arrivalDetector = new NavigationArrivalDetector(geometryState);
    private final NavigationIntermediateArrivalTracker intermediateArrivalTracker =
            new NavigationIntermediateArrivalTracker();
    private final NavigationRouteEvaluator routeEvaluator = new NavigationRouteEvaluator(
            geometryState,
            turnState,
            progressTracker,
            deviationHandler,
            displayState,
            arrivalDetector,
            intermediateArrivalTracker
    );
    private final NavigationBlockedPointSelector blockedPointSelector = new NavigationBlockedPointSelector(
            geometryState,
            blockedRouteState
    );
    private final NavigationRouteResultApplier routeResultApplier = new NavigationRouteResultApplier(
            geometryState,
            displayState,
            deviationHandler,
            progressTracker,
            turnState,
            arrivalDetector,
            intermediateArrivalTracker
    );

    public void reset() {
        geometryState.reset();
        displayState.reset();
        deviationHandler.clearDeviationEvidence();
        progressTracker.reset();
        blockedRouteState.reset();
        turnState.reset();
        intermediateArrivalTracker.reset();
    }

    public boolean hasActiveRoute() {
        return geometryState.hasActiveRoute();
    }

    @NonNull
    public List<LatLon> remainingIntermediateStops(@NonNull List<LatLon> fallbackStops) {
        return intermediateArrivalTracker.remainingStops(fallbackStops);
    }

    @NonNull
    public List<NogoPoint> copyBlockedPoints() {
        return blockedRouteState.copyBlockedPoints();
    }

    @NonNull
    public Evaluation evaluateLocation(
            @NonNull Location filtered,
            float speedMps,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            long nowMs,
            long fastChecksUntilMs
    ) {
        return evaluateLocation(
                filtered,
                speedMps,
                false,
                accuracyMeters,
                actualBearingDegrees,
                nowMs,
                fastChecksUntilMs
        );
    }

    @NonNull
    public Evaluation evaluateLocation(
            @NonNull Location filtered,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            long nowMs,
            long fastChecksUntilMs
    ) {
        return routeEvaluator.evaluateLocation(
                filtered,
                speedMps,
                likelyStationary,
                accuracyMeters,
                actualBearingDegrees,
                nowMs,
                fastChecksUntilMs
        );
    }

    @Nullable
    public Double currentSegmentBearingDegrees(@Nullable Location lastFiltered) {
        return geometryState.currentSegmentBearingDegrees(lastFiltered);
    }

    @NonNull
    public List<NogoPoint> addBlockedPointsAhead(@Nullable Location lastFiltered, long nowMs) {
        return blockedPointSelector.addBlockedPointsAhead(lastFiltered, nowMs);
    }

    @NonNull
    public List<NavigationTurnEvent> applyRouteResult(
            @NonNull Context context,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            @Nullable Location lastFiltered,
            float speedMps,
            long beganAt
    ) {
        return applyRouteResult(
                context,
                snapshot,
                newRoute,
                lastFiltered,
                speedMps,
                false,
                beganAt
        );
    }

    @NonNull
    public List<NavigationTurnEvent> applyRouteResult(
            @NonNull Context context,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            @Nullable Location lastFiltered,
            float speedMps,
            boolean likelyStationary,
            long beganAt
    ) {
        return routeResultApplier.applyRouteResult(new NavigationRouteResultInput(
                context,
                snapshot,
                newRoute,
                lastFiltered,
                speedMps,
                likelyStationary,
                beganAt
        ));
    }

    @NonNull
    public NavState advanceDisplayState(@NonNull NavigationDisplaySnapshot snapshot) {
        NavState state = displayState.buildState(
                snapshot,
                geometryState.route(),
                geometryState.polylineIndex(),
                geometryState.lastSegmentIndex(),
                turnState,
                progressTracker
        );
        displayState.rememberRenderedState(state, snapshot);
        return state;
    }

    @NonNull
    NavState buildState(
            @NonNull Context context,
            @Nullable Location lastFiltered,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            boolean routeCalculationInProgress,
            @Nullable String routeCalculationNotice,
            @Nullable Throwable lastRouteFailure
    ) {
        return advanceDisplayState(NavigationDisplaySnapshot.builder(context)
                .location(lastFiltered, speedMps, likelyStationary, accuracyMeters)
                .gps(fixedSatelliteCount, 0)
                .heading(headingDegrees, headingAccuracyDegrees)
                .timing(nextEvaluationDeadlineElapsedMs, nowMs)
                .routeCalculation(routeCalculationInProgress, routeCalculationNotice, lastRouteFailure)
                .build());
    }

    public static final class Evaluation {
        private final boolean shouldRecalculateRoute;
        private final boolean stableOnRouteSample;
        private final long suggestedUpdateIntervalMs;
        @Nullable
        final NavigationRerouteNotice rerouteNotice;
        @NonNull
        final List<NavigationTurnEvent> turnEvents;

        private Evaluation(
                boolean shouldRecalculateRoute,
                boolean stableOnRouteSample,
                long suggestedUpdateIntervalMs,
                @Nullable NavigationRerouteNotice rerouteNotice,
                @NonNull List<NavigationTurnEvent> turnEvents
        ) {
            this.shouldRecalculateRoute = shouldRecalculateRoute;
            this.stableOnRouteSample = stableOnRouteSample;
            this.suggestedUpdateIntervalMs = suggestedUpdateIntervalMs;
            this.rerouteNotice = rerouteNotice;
            this.turnEvents = turnEvents;
        }

        @NonNull
        public static Evaluation requestRecalculation(@Nullable NavigationRerouteNotice rerouteNotice) {
            return new Evaluation(true, false, NO_SUGGESTED_INTERVAL, rerouteNotice, Collections.emptyList());
        }

        @NonNull
        public static Evaluation keepRoute(
                @NonNull List<NavigationTurnEvent> turnEvents,
                long suggestedUpdateIntervalMs,
                boolean stableOnRouteSample
        ) {
            return new Evaluation(false, stableOnRouteSample, suggestedUpdateIntervalMs, null, turnEvents);
        }

        public boolean shouldRecalculateRoute() {
            return shouldRecalculateRoute;
        }

        public boolean isStableOnRouteSample() {
            return stableOnRouteSample;
        }

        public long getSuggestedUpdateIntervalMs() {
            return suggestedUpdateIntervalMs;
        }
    }

}
