package vibro.navigator.nav.session;


import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.geo.LatLon;
import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

import java.util.List;

// Route coordinator: route safety policies stay split across named helpers even though they meet in this class.
public final class NavigationSessionRouteState {

    private final NavigationSessionRouteComponents components = new NavigationSessionRouteComponents();

    public void reset() {
        components.reset();
    }

    public boolean hasActiveRoute() {
        return components.geometryState.hasActiveRoute();
    }

    @Nullable
    public GeoJsonRoute currentRoute() {
        return components.geometryState.route();
    }

    @NonNull
    public List<LatLon> remainingIntermediateStops(@NonNull List<LatLon> fallbackStops) {
        return components.intermediateArrivalTracker.remainingStops(fallbackStops);
    }

    @NonNull
    public List<NogoPoint> copyBlockedPoints() {
        return components.blockedRouteState.copyBlockedPoints();
    }

    @NonNull
    public NavigationRouteEvaluation evaluateLocation(
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
    public NavigationRouteEvaluation evaluateLocation(
            @NonNull Location filtered,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            long nowMs,
            long fastChecksUntilMs
    ) {
        return evaluateLocation(
                filtered,
                speedMps,
                likelyStationary,
                accuracyMeters,
                actualBearingDegrees,
                nowMs,
                fastChecksUntilMs,
                false
        );
    }

    @NonNull
    public NavigationRouteEvaluation evaluateLocation(
            @NonNull Location filtered,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            long nowMs,
            long fastChecksUntilMs,
            boolean reacquiringAfterLongGap
    ) {
        return components.routeEvaluator.evaluateLocation(
                filtered,
                speedMps,
                likelyStationary,
                accuracyMeters,
                actualBearingDegrees,
                nowMs,
                fastChecksUntilMs,
                reacquiringAfterLongGap
        );
    }

    @Nullable
    public Double currentSegmentBearingDegrees(@Nullable Location lastFiltered) {
        return components.geometryState.currentSegmentBearingDegrees(lastFiltered);
    }

    @NonNull
    public List<NogoPoint> addBlockedPointsAhead(@Nullable Location lastFiltered, long nowMs) {
        return components.blockedPointSelector.addBlockedPointsAhead(lastFiltered, nowMs);
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
        return components.routeResultApplier.applyRouteResult(new NavigationRouteResultInput(
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
        NavState state = components.displayState.buildState(
                snapshot,
                components.geometryState.route(),
                components.geometryState.polylineIndex(),
                components.geometryState.lastSegmentIndex(),
                components.turnState,
                components.progressTracker
        );
        components.displayState.rememberRenderedState(state, snapshot);
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

}
