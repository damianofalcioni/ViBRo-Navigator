package vibro.navigator.nav.session;


import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.geo.LatLon;
import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

import java.util.List;

// Route coordinator: route safety policies stay split across named helpers even though they meet in this class.
public final class NavigationSessionRouteState {

    private final NavigationSessionRouteComponents components = new NavigationSessionRouteComponents();
    private boolean singleInstructionMode;
    private long displayedHistoryRevision = -1;

    public void reset() {
        components.reset();
        displayedHistoryRevision = -1;
    }

    void clearMotionEvidence() {
        components.deviationHandler.clearDeviationEvidence();
        components.progressTracker.reset();
        components.directGuidance.state().clearMotionEvidence();
    }

    void setSingleInstructionMode(boolean singleInstructionMode) {
        this.singleInstructionMode = singleInstructionMode;
    }

    public boolean hasActiveRoute() {
        return components.geometryState.hasActiveRoute();
    }

    @NonNull
    public List<String> buildDirectionDetails(@NonNull NavigationDisplaySnapshot snapshot) {
        return NavigationRouteDirectionDetails.build(
                snapshot,
                components.geometryState,
                components.turnState,
                components.progressTracker,
                components.directGuidance.activeTarget(),
                components.directGuidance.resolveRouteMatch(
                        snapshot.lastFiltered,
                        snapshot.accuracyMeters
                ),
                components.displayState.targetsForDetails()
        );
    }

    @NonNull
    NavigationRouteHistory historyForExport() {
        return components.routeHistory;
    }

    NavigationBeelineRecoveryState beelineRecovery() {
        return components.directGuidance.state().recovery;
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
            @NonNull NavigationLocation filtered,
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
            @NonNull NavigationLocation filtered,
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
            @NonNull NavigationLocation filtered,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            long nowMs,
            long fastChecksUntilMs,
            boolean reacquiringAfterLongGap
    ) {
        NavigationRouteEvaluation evaluation = components.routeEvaluator.evaluateLocation(
                filtered,
                speedMps,
                likelyStationary,
                accuracyMeters,
                actualBearingDegrees,
                nowMs,
                fastChecksUntilMs,
                reacquiringAfterLongGap,
                singleInstructionMode
        );
        beelineRecovery().setTarget(components.directGuidance.activeTarget());
        return evaluation;
    }

    @Nullable
    public Double currentSegmentBearingDegrees(@Nullable NavigationLocation lastFiltered) {
        if (lastFiltered == null || components.geometryState.isRouteUnavailable()) {
            return null;
        }
        float accuracyMeters = lastFiltered.hasAccuracy()
                ? lastFiltered.getAccuracy()
                : Float.MAX_VALUE;
        return components.directGuidance.expectedRouteBearingDegrees(
                lastFiltered,
                accuracyMeters
        );
    }

    void recordRecalculationFixPath(
            @NonNull NavigationLocation filtered,
            @NonNull NavigationRouteEvaluation evaluation,
            boolean routeCalculationInProgress
    ) {
        components.routeHistory.recordRerouteFixPath(
                filtered,
                evaluation,
                routeCalculationInProgress
        );
    }

    @Nullable
    public Double currentRouteBearingDegrees(@Nullable NavigationLocation lastFiltered) {
        Double directGuidanceBearingDegrees =
                components.directGuidance.bearingDegreesFrom(lastFiltered);
        return directGuidanceBearingDegrees != null
                ? directGuidanceBearingDegrees
                : currentSegmentBearingDegrees(lastFiltered);
    }

    @NonNull
    public List<NogoPoint> addBlockedPointsAhead(@Nullable NavigationLocation lastFiltered, long nowMs) {
        return components.blockedPointSelector.addBlockedPointsAhead(lastFiltered, nowMs);
    }

    @NonNull
    public List<NavigationTurnEvent> applyRouteResult(
            @NonNull Context context,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            @Nullable NavigationLocation lastFiltered,
            float speedMps,
            long beganAt
    ) {
        return applyRouteResult(
                new AndroidNavigationTextResources(context),
                snapshot,
                newRoute,
                lastFiltered,
                speedMps,
                beganAt
        );
    }

    @NonNull
    public List<NavigationTurnEvent> applyRouteResult(
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            @Nullable NavigationLocation lastFiltered,
            float speedMps,
            long beganAt
    ) {
        return applyRouteResult(
                textResources,
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
            @Nullable NavigationLocation lastFiltered,
            float speedMps,
            boolean likelyStationary,
            long beganAt
    ) {
        return applyRouteResult(
                new AndroidNavigationTextResources(context),
                snapshot,
                newRoute,
                lastFiltered,
                speedMps,
                likelyStationary,
                beganAt
        );
    }

    @NonNull
    public List<NavigationTurnEvent> applyRouteResult(
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            @Nullable NavigationLocation lastFiltered,
            float speedMps,
            boolean likelyStationary,
            long beganAt
    ) {
        components.deviationHandler.clearDeviationEvidence();
        components.progressTracker.reset();
        return components.routeResultApplier.applyRouteResult(new NavigationRouteResultInput(
                textResources,
                snapshot,
                newRoute,
                lastFiltered,
                speedMps,
                likelyStationary,
                beganAt,
                singleInstructionMode
        ));
    }

    @NonNull
    public NavState advanceDisplayState(@NonNull NavigationDisplaySnapshot snapshot) {
        return advanceDisplayState(snapshot, false);
    }

    @NonNull
    public NavState advanceDisplayState(
            @NonNull NavigationDisplaySnapshot snapshot,
            boolean showNextManeuverCue
    ) {
        synchronizeDisplayHistory();
        NavState state = components.displayState.buildState(
                snapshot,
                components.geometryState,
                components.turnState,
                components.progressTracker,
                components.directGuidance.activeTarget(),
                components.directGuidance.resolveRouteMatch(
                        snapshot.lastFiltered,
                        snapshot.accuracyMeters
                ),
                showNextManeuverCue
        );
        components.displayState.rememberRenderedState(state, snapshot);
        return state;
    }

    private void synchronizeDisplayHistory() {
        if (displayedHistoryRevision != components.routeHistory.revision()) {
            components.displayState.compassMemory.synchronizeHistory(components.routeHistory.entryMeters(),
                    components.routeHistory.archivedSegmentsSnapshot(),
                    components.routeHistory.recalculationBridgeSegmentsSnapshot());
            displayedHistoryRevision = components.routeHistory.revision();
        }
    }

    @NonNull
    NavState buildState(
            @NonNull Context context,
            @Nullable NavigationLocation lastFiltered,
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
        return buildState(
                new AndroidNavigationTextResources(context),
                lastFiltered,
                speedMps,
                likelyStationary,
                accuracyMeters,
                fixedSatelliteCount,
                headingDegrees,
                headingAccuracyDegrees,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                routeCalculationInProgress,
                routeCalculationNotice,
                lastRouteFailure
        );
    }

    @NonNull
    NavState buildState(
            @NonNull NavigationTextResources textResources,
            @Nullable NavigationLocation lastFiltered,
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
        return advanceDisplayState(NavigationDisplaySnapshot.builder(textResources)
                .location(lastFiltered, speedMps, likelyStationary, accuracyMeters)
                .gps(fixedSatelliteCount, 0)
                .heading(headingDegrees, headingAccuracyDegrees)
                .blockedPoints(copyBlockedPoints())
                .timing(nextEvaluationDeadlineElapsedMs, nowMs)
                .routeCalculation(routeCalculationInProgress, routeCalculationNotice, lastRouteFailure)
                .build());
    }

}
