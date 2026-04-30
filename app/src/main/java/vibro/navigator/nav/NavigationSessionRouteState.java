package vibro.navigator.nav;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.util.AppLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class NavigationSessionRouteState {

    private static final String TAG = "NavSessionRoute";
    private static final long NO_SUGGESTED_INTERVAL = -1L;
    private static final double MIN_DESTINATION_REACHED_RADIUS_METERS = 5.0;

    private final NavigationBlockedRouteState blockedRouteState = new NavigationBlockedRouteState();
    private final NavigationTurnState turnState = new NavigationTurnState();
    private final NavigationRouteProgressTracker progressTracker = new NavigationRouteProgressTracker();
    private final NavigationRouteDeviationHandler deviationHandler =
            new NavigationRouteDeviationHandler(progressTracker);
    private final NavigationSessionRouteDisplayState displayState = new NavigationSessionRouteDisplayState();

    @Nullable
    private GeoJsonRoute route;
    @Nullable
    private PolylineIndex polylineIndex;
    private int lastSegmentIndex = -1;

    void reset() {
        route = null;
        polylineIndex = null;
        lastSegmentIndex = -1;
        displayState.reset();
        deviationHandler.clearDeviationEvidence();
        progressTracker.reset();
        blockedRouteState.reset();
        turnState.reset();
    }

    boolean hasActiveRoute() {
        return route != null;
    }

    @NonNull
    List<NogoPoint> copyBlockedPoints() {
        return blockedRouteState.copyBlockedPoints();
    }

    @NonNull
    Evaluation evaluateLocation(
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
    Evaluation evaluateLocation(
            @NonNull Location filtered,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            long nowMs,
            long fastChecksUntilMs
    ) {
        if (isRouteUnavailable()) {
            AppLogger.i(TAG, "No active route loaded, requesting route calculation");
            return Evaluation.requestRecalculation(null);
        }

        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(filtered.getLatitude(), filtered.getLongitude()),
                lastSegmentIndex
        );
        if (match == null) {
            AppLogger.w(TAG, "Route match failed, requesting recalculation");
            return Evaluation.requestRecalculation(null);
        }
        lastSegmentIndex = match.segmentIndex;
        double expectedBearingDegrees = expectedBearingDegrees(match);
        double smoothedAccuracyMeters = progressTracker.rememberAndResolveSmoothedAccuracyMeters(accuracyMeters, nowMs);
        displayState.rememberSmoothedAccuracyMeters((float) smoothedAccuracyMeters);
        float etaSpeedMps = progressTracker.resolveEtaSpeedMps(
                filtered,
                match.alongTrackMeters,
                accuracyMeters,
                likelyStationary
        );
        NavigationRouteProgressTracker.DirectionAssessment directionOfProgress = progressTracker.assessDirection(
                match.alongTrackMeters,
                nowMs
        );
        if (isWithinDestinationReachedRadius(filtered, accuracyMeters)) {
            deviationHandler.clearDeviationEvidence();
            progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            return Evaluation.keepRoute(turnState.onDestinationReached(route), NO_SUGGESTED_INTERVAL, true);
        }

        NavigationRouteDeviationHandler.Decision deviationDecision = deviationHandler.evaluate(
                match,
                smoothedAccuracyMeters,
                directionOfProgress,
                speedMps,
                expectedBearingDegrees,
                actualBearingDegrees,
                nowMs
        );
        if (deviationDecision.shouldRecalculateRoute()) {
            return Evaluation.requestRecalculation(deviationDecision.getRerouteNotice());
        }
        if (deviationDecision.shouldKeepCurrentRoute()) {
            return keepCurrentRoute(
                    match,
                    etaSpeedMps,
                    accuracyMeters,
                    nowMs,
                    fastChecksUntilMs,
                    deviationDecision.isStableOnRouteSample()
            );
        }

        progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
        return keepCurrentRoute(match, etaSpeedMps, accuracyMeters, nowMs, fastChecksUntilMs, true);
    }

    @NonNull
    private Evaluation keepCurrentRoute(
            @NonNull PolylineIndex.Match match,
            float etaSpeedMps,
            float accuracyMeters,
            long nowMs,
            long fastChecksUntilMs,
            boolean stableOnRouteSample
    ) {
        NavigationTurnState.Progress progress = turnState.evaluate(
                route,
                polylineIndex,
                match.alongTrackMeters,
                match.segmentIndex,
                etaSpeedMps,
                accuracyMeters,
                nowMs,
                fastChecksUntilMs
        );
        return Evaluation.keepRoute(progress.turnEvents, progress.suggestedUpdateIntervalMs, stableOnRouteSample);
    }

    private boolean isRouteUnavailable() {
        return route == null || polylineIndex == null || route.track.isEmpty();
    }

    @Nullable
    Double currentSegmentBearingDegrees(@Nullable Location lastFiltered) {
        if (lastFiltered == null || route == null || polylineIndex == null || route.track.isEmpty()) {
            return null;
        }
        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                lastSegmentIndex
        );
        return match == null ? null : expectedBearingDegrees(match);
    }

    @NonNull
    List<NogoPoint> addBlockedPointsAhead(@Nullable Location lastFiltered, long nowMs) {
        List<NogoPoint> added = new ArrayList<>();
        if (lastFiltered == null || route == null || polylineIndex == null || route.track.isEmpty()) {
            return added;
        }

        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                lastSegmentIndex
        );
        if (match == null) {
            return added;
        }

        return blockedRouteState.addBlockedPointsAhead(polylineIndex, match.alongTrackMeters, nowMs);
    }

    @NonNull
    List<NavigationTurnEvent> applyRouteResult(
            @NonNull Context context,
            @NonNull NavigationRequest request,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            @Nullable Location lastFiltered,
            float speedMps,
            long beganAt
    ) {
        return applyRouteResult(
                context,
                request,
                snapshot,
                newRoute,
                lastFiltered,
                speedMps,
                false,
                beganAt
        );
    }

    @NonNull
    List<NavigationTurnEvent> applyRouteResult(
            @NonNull Context context,
            @NonNull NavigationRequest request,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            @Nullable Location lastFiltered,
            float speedMps,
            boolean likelyStationary,
            long beganAt
    ) {
        route = newRoute;
        polylineIndex = new PolylineIndex(newRoute.track);
        lastSegmentIndex = -1;
        displayState.onRouteApplied(context, request, newRoute, polylineIndex);
        deviationHandler.clearDeviationEvidence();
        progressTracker.reset();
        float etaSpeedMps = 0f;

        List<NavigationTurnEvent> turnEvents = lastFiltered != null
                && isWithinDestinationReachedRadius(lastFiltered, accuracyOf(lastFiltered))
                ? turnState.onDestinationReached(newRoute)
                : turnState.onRouteApplied(newRoute, polylineIndex, lastFiltered, etaSpeedMps, accuracyOf(lastFiltered));
        AppLogger.i(TAG, "Route recalculation #" + snapshot.requestNumber
                + " succeeded durationMs=" + (System.currentTimeMillis() - beganAt)
                + " trackPoints=" + newRoute.track.size()
                + " voiceHints=" + newRoute.voiceHints.size()
                + " lengthMeters=" + newRoute.trackLengthMeters);
        return turnEvents;
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
        return displayState.buildState(
                context,
                route,
                polylineIndex,
                lastSegmentIndex,
                turnState,
                progressTracker,
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

    private float accuracyOf(@Nullable Location location) {
        return location != null && location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }

    private boolean isWithinDestinationReachedRadius(@NonNull Location location, float accuracyMeters) {
        if (route == null || route.track.isEmpty()) {
            return false;
        }
        LatLon destination = route.track.get(route.track.size() - 1);
        double destinationDistanceMeters = GeoMath.distanceMeters(
                location.getLatitude(),
                location.getLongitude(),
                destination.lat,
                destination.lon
        );
        double destinationReachedRadiusMeters = Math.max(
                MIN_DESTINATION_REACHED_RADIUS_METERS,
                Float.isFinite(accuracyMeters) && accuracyMeters > 0f ? accuracyMeters : 0.0
        );
        return destinationDistanceMeters <= destinationReachedRadiusMeters;
    }

    private double expectedBearingDegrees(@NonNull PolylineIndex.Match match) {
        return NavigationExpectedBearingResolver.resolve(polylineIndex, match);
    }

    static final class Evaluation {
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
        static Evaluation requestRecalculation(@Nullable NavigationRerouteNotice rerouteNotice) {
            return new Evaluation(true, false, NO_SUGGESTED_INTERVAL, rerouteNotice, Collections.emptyList());
        }

        @NonNull
        static Evaluation keepRoute(
                @NonNull List<NavigationTurnEvent> turnEvents,
                long suggestedUpdateIntervalMs,
                boolean stableOnRouteSample
        ) {
            return new Evaluation(false, stableOnRouteSample, suggestedUpdateIntervalMs, null, turnEvents);
        }

        boolean shouldRecalculateRoute() {
            return shouldRecalculateRoute;
        }

        boolean isStableOnRouteSample() {
            return stableOnRouteSample;
        }

        long getSuggestedUpdateIntervalMs() {
            return suggestedUpdateIntervalMs;
        }
    }

}
