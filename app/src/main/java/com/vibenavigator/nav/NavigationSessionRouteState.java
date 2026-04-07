package com.vibenavigator.nav;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.R;
import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.PolylineIndex;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class NavigationSessionRouteState {

    private static final String TAG = "NavSessionRoute";
    private static final long NO_SUGGESTED_INTERVAL = -1L;

    private final NavigationBlockedRouteState blockedRouteState = new NavigationBlockedRouteState();
    private final RouteDeviationPolicy routeDeviationPolicy = new RouteDeviationPolicy();
    private final NavigationTurnState turnState = new NavigationTurnState();

    @Nullable
    private GeoJsonRoute route;
    @Nullable
    private PolylineIndex polylineIndex;
    private int lastSegmentIndex = -1;
    @NonNull
    private List<NavTarget> targets = new ArrayList<>();

    void reset() {
        route = null;
        polylineIndex = null;
        lastSegmentIndex = -1;
        targets = new ArrayList<>();
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
        if (route == null || polylineIndex == null || route.track.isEmpty()) {
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

        RouteDeviationPolicy.Decision deviationDecision = routeDeviationPolicy.evaluate(
                match.distanceToTrackMeters,
                accuracyMeters,
                actualBearingDegrees,
                match.segmentBearingDegrees
        );
        if (deviationDecision.reason == RouteDeviationPolicy.Reason.OFF_TRACK) {
            AppLogger.w(TAG, "Off-track detected distance=" + match.distanceToTrackMeters
                    + " threshold=" + deviationDecision.offTrackThresholdMeters);
            return Evaluation.requestRecalculation(NavigationRerouteNotice.fromDecision(deviationDecision));
        }
        if (deviationDecision.reason == RouteDeviationPolicy.Reason.BEARING_MISMATCH) {
            AppLogger.w(TAG, "Bearing mismatch detected diff=" + deviationDecision.bearingDiffDegrees
                    + " expected=" + match.segmentBearingDegrees
                    + " actual=" + actualBearingDegrees);
            return Evaluation.requestRecalculation(NavigationRerouteNotice.fromDecision(deviationDecision));
        }

        NavigationTurnState.Progress progress = turnState.evaluate(
                route,
                polylineIndex,
                match.alongTrackMeters,
                speedMps,
                nowMs,
                fastChecksUntilMs
        );
        return Evaluation.keepRoute(progress.turnEvents, progress.suggestedUpdateIntervalMs);
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
    List<NavigationSession.TurnEvent> applyRouteResult(
            @NonNull Context context,
            @NonNull NavigationRequest request,
            @NonNull NavigationSession.RouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            @Nullable Location lastFiltered,
            float speedMps,
            long beganAt
    ) {
        route = newRoute;
        polylineIndex = new PolylineIndex(newRoute.track);
        lastSegmentIndex = -1;
        targets = buildTargets(context, request.stops, polylineIndex);

        List<NavigationSession.TurnEvent> turnEvents =
                turnState.onRouteApplied(newRoute, polylineIndex, lastFiltered, speedMps);
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
            float accuracyMeters,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            boolean routeCalculationInProgress,
            @Nullable Throwable lastRouteFailure
    ) {
        if (lastFiltered == null) {
            if (lastRouteFailure != null) {
                return NavState.routeUnavailable(
                        context,
                        NavigationRouteFailureFormatter.format(context, lastRouteFailure, false),
                        nextEvaluationDeadlineElapsedMs
                );
            }
            return NavState.waitingForLocation(context, nextEvaluationDeadlineElapsedMs);
        }

        if (routeCalculationInProgress) {
            return NavState.calculatingRoute(context, nextEvaluationDeadlineElapsedMs);
        }

        if (route == null || polylineIndex == null) {
            if (lastRouteFailure != null) {
                return NavState.routeUnavailable(
                        context,
                        NavigationRouteFailureFormatter.format(context, lastRouteFailure, false),
                        nextEvaluationDeadlineElapsedMs
                );
            }
            return NavState.calculatingRoute(context, nextEvaluationDeadlineElapsedMs);
        }

        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                lastSegmentIndex
        );
        if (match == null) {
            return NavState.waiting(context);
        }

        NavState state = NavState.from(
                route,
                polylineIndex,
                match.alongTrackMeters,
                turnState.getNextHintIdx(),
                speedMps,
                accuracyMeters,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                targets,
                context
        );
        if (lastRouteFailure != null) {
            return NavState.withNotice(
                    state,
                    NavigationRouteFailureFormatter.format(context, lastRouteFailure, true)
            );
        }
        return state;
    }

    @NonNull
    private List<NavTarget> buildTargets(
            @NonNull Context context,
            @NonNull List<LatLon> intermediates,
            @NonNull PolylineIndex index
    ) {
        List<NavTarget> out = new ArrayList<>();
        for (int i = 0; i < intermediates.size(); i++) {
            PolylineIndex.Match match = index.match(intermediates.get(i), -1);
            if (match != null) {
                out.add(new NavTarget(context.getString(R.string.format_stop_label, i + 1), match.alongTrackMeters));
            }
        }
        out.add(new NavTarget(context.getString(R.string.label_destination), index.totalLengthMeters()));
        return out;
    }

    static final class Evaluation {
        private final boolean shouldRecalculateRoute;
        private final boolean stableOnRouteSample;
        private final long suggestedUpdateIntervalMs;
        @Nullable
        final NavigationRerouteNotice rerouteNotice;
        @NonNull
        final List<NavigationSession.TurnEvent> turnEvents;

        private Evaluation(
                boolean shouldRecalculateRoute,
                boolean stableOnRouteSample,
                long suggestedUpdateIntervalMs,
                @Nullable NavigationRerouteNotice rerouteNotice,
                @NonNull List<NavigationSession.TurnEvent> turnEvents
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
                @NonNull List<NavigationSession.TurnEvent> turnEvents,
                long suggestedUpdateIntervalMs
        ) {
            return new Evaluation(false, true, suggestedUpdateIntervalMs, null, turnEvents);
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
