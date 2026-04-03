package com.vibenavigator.nav;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.R;
import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.geo.GeoMath;
import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.PolylineIndex;
import com.vibenavigator.nav.route.VoiceHint;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class NavigationSessionRouteState {

    private static final String TAG = "NavSessionRoute";
    private static final double BLOCKED_ROUTE_FIRST_POINT_OFFSET_METERS = 20.0;
    private static final double BLOCKED_ROUTE_POINT_STEP_METERS = 18.0;
    private static final double BLOCKED_RADIUS_BASE_METERS = 12.0;
    private static final double BLOCKED_RADIUS_STEP_METERS = 6.0;
    private static final double BLOCKED_RADIUS_MAX_METERS = 30.0;
    private static final int BLOCKED_POINT_COUNT_MAX = 3;
    private static final double BLOCKED_SAME_AREA_METERS = 35.0;
    private static final double BLOCKED_QUICK_REPEAT_NEARBY_METERS = 75.0;
    private static final long BLOCKED_QUICK_REPEAT_WINDOW_MS = 15_000L;
    private static final long NO_SUGGESTED_INTERVAL = -1L;

    private final RouteDeviationPolicy routeDeviationPolicy = new RouteDeviationPolicy();
    private final NavigationUpdateScheduler updateScheduler = new NavigationUpdateScheduler();
    private final TurnEventPlanner turnEventPlanner = new TurnEventPlanner();
    private final List<NogoPoint> blocked = new ArrayList<>();

    @Nullable
    private GeoJsonRoute route;
    @Nullable
    private PolylineIndex polylineIndex;
    private int lastSegmentIndex = -1;
    private int nextHintIdx;
    private boolean notified10;
    private boolean notified5;
    private boolean initialTurnNotificationSent;
    @NonNull
    private List<NavTarget> targets = new ArrayList<>();
    @Nullable
    private LatLon lastBlockedAreaCenter;
    private long lastBlockedAreaAtMs;
    private int lastBlockedAreaLevel;

    void reset() {
        route = null;
        polylineIndex = null;
        lastSegmentIndex = -1;
        nextHintIdx = 0;
        notified10 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        targets = new ArrayList<>();
        blocked.clear();
        lastBlockedAreaCenter = null;
        lastBlockedAreaAtMs = 0L;
        lastBlockedAreaLevel = 0;
    }

    boolean hasActiveRoute() {
        return route != null;
    }

    @NonNull
    List<NogoPoint> copyBlockedPoints() {
        return new ArrayList<>(blocked);
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
            return Evaluation.requestRecalculation();
        }

        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(filtered.getLatitude(), filtered.getLongitude()),
                lastSegmentIndex
        );
        if (match == null) {
            AppLogger.w(TAG, "Route match failed, requesting recalculation");
            return Evaluation.requestRecalculation();
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
            return Evaluation.requestRecalculation();
        }
        if (deviationDecision.reason == RouteDeviationPolicy.Reason.BEARING_MISMATCH) {
            AppLogger.w(TAG, "Bearing mismatch detected diff=" + deviationDecision.bearingDiffDegrees
                    + " expected=" + match.segmentBearingDegrees
                    + " actual=" + actualBearingDegrees);
            return Evaluation.requestRecalculation();
        }

        TurnEventPlanner.Progress turnProgress = turnEventPlanner.advance(
                route.voiceHints,
                polylineIndex,
                nextHintIdx,
                notified10,
                notified5,
                match.alongTrackMeters,
                speedMps
        );
        nextHintIdx = turnProgress.nextHintIdx;
        notified10 = turnProgress.notified10;
        notified5 = turnProgress.notified5;
        long suggestedIntervalMs = updateScheduler.suggestUpdateInterval(
                nowMs,
                fastChecksUntilMs,
                route.voiceHints,
                polylineIndex,
                nextHintIdx,
                match.alongTrackMeters,
                speedMps
        );
        return Evaluation.keepRoute(toTurnEvents(turnProgress.signals), suggestedIntervalMs);
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

        LatLon anchor = polylineIndex.pointAtDistance(match.alongTrackMeters + BLOCKED_ROUTE_FIRST_POINT_OFFSET_METERS);
        if (anchor == null) {
            return added;
        }

        int level = nextBlockedAreaLevel(anchor, nowMs);
        double radiusMeters = blockedRadiusForLevel(level);
        int pointCount = blockedPointCountForLevel(level);
        replaceNearbyBlockedPoints(anchor);

        for (int i = 0; i < pointCount; i++) {
            double distance = match.alongTrackMeters
                    + BLOCKED_ROUTE_FIRST_POINT_OFFSET_METERS
                    + (i * BLOCKED_ROUTE_POINT_STEP_METERS);
            LatLon point = polylineIndex.pointAtDistance(distance);
            if (point == null) {
                continue;
            }
            NogoPoint nogo = new NogoPoint(point.lat, point.lon, radiusMeters);
            blocked.add(nogo);
            added.add(nogo);
        }

        lastBlockedAreaCenter = anchor;
        lastBlockedAreaAtMs = nowMs;
        lastBlockedAreaLevel = level;
        return added;
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
        nextHintIdx = findNextHintIndex(newRoute, polylineIndex, lastFiltered);
        notified10 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        targets = buildTargets(context, request.stops, polylineIndex);

        List<NavigationSession.TurnEvent> turnEvents = buildInitialTurnEventIfNeeded(lastFiltered, speedMps);
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
            @Nullable String lastRouteFailureMessage
    ) {
        if (lastFiltered == null) {
            if (lastRouteFailureMessage != null) {
                return NavState.routeUnavailable(context, lastRouteFailureMessage, nextEvaluationDeadlineElapsedMs);
            }
            return NavState.waitingForLocation(context, nextEvaluationDeadlineElapsedMs);
        }

        if (routeCalculationInProgress) {
            return NavState.calculatingRoute(context, nextEvaluationDeadlineElapsedMs);
        }

        if (route == null || polylineIndex == null) {
            if (lastRouteFailureMessage != null) {
                return NavState.routeUnavailable(context, lastRouteFailureMessage, nextEvaluationDeadlineElapsedMs);
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

        return NavState.from(
                route,
                polylineIndex,
                match.alongTrackMeters,
                nextHintIdx,
                speedMps,
                accuracyMeters,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                targets,
                context
        );
    }

    @NonNull
    private List<NavigationSession.TurnEvent> buildInitialTurnEventIfNeeded(
            @Nullable Location lastFiltered,
            float speedMps
    ) {
        if (initialTurnNotificationSent || route == null || polylineIndex == null) {
            return Collections.emptyList();
        }
        List<VoiceHint> hints = route.voiceHints;
        if (hints.isEmpty() || nextHintIdx < 0 || nextHintIdx >= hints.size()) {
            return Collections.emptyList();
        }

        double alongTrackMeters = 0.0;
        if (lastFiltered != null) {
            PolylineIndex.Match match = polylineIndex.match(
                    new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                    -1
            );
            if (match != null) {
                alongTrackMeters = match.alongTrackMeters;
            }
        }

        TurnEventPlanner.TurnSignal initialSignal = turnEventPlanner.buildInitialSignal(
                hints,
                polylineIndex,
                nextHintIdx,
                initialTurnNotificationSent,
                alongTrackMeters,
                speedMps
        );
        if (initialSignal == null) {
            return Collections.emptyList();
        }
        initialTurnNotificationSent = true;
        return Collections.singletonList(toTurnEvent(initialSignal));
    }

    private int findNextHintIndex(
            @NonNull GeoJsonRoute candidateRoute,
            @NonNull PolylineIndex candidateIndex,
            @Nullable Location location
    ) {
        if (location == null || candidateRoute.voiceHints.isEmpty()) {
            return 0;
        }

        PolylineIndex.Match match = candidateIndex.match(
                new LatLon(location.getLatitude(), location.getLongitude()),
                -1
        );
        if (match == null) {
            return 0;
        }

        for (int i = 0; i < candidateRoute.voiceHints.size(); i++) {
            VoiceHint hint = candidateRoute.voiceHints.get(i);
            double hintDistance = candidateIndex.distanceAtPointIndex(hint.indexInTrack);
            if (hintDistance + 5.0 > match.alongTrackMeters) {
                return i;
            }
        }
        return candidateRoute.voiceHints.size();
    }

    @NonNull
    private List<NavigationSession.TurnEvent> toTurnEvents(@NonNull List<TurnEventPlanner.TurnSignal> signals) {
        if (signals.isEmpty()) {
            return Collections.emptyList();
        }
        List<NavigationSession.TurnEvent> events = new ArrayList<>(signals.size());
        for (TurnEventPlanner.TurnSignal signal : signals) {
            events.add(toTurnEvent(signal));
        }
        return events;
    }

    @NonNull
    private NavigationSession.TurnEvent toTurnEvent(@NonNull TurnEventPlanner.TurnSignal signal) {
        switch (signal.type) {
            case PASSED:
                return NavigationSession.TurnEvent.passed(signal.hint);
            case INITIAL:
                return NavigationSession.TurnEvent.initial(signal.hint, signal.distanceMeters, signal.timeSeconds);
            case IMMINENT:
            default:
                return NavigationSession.TurnEvent.imminent(signal.hint, signal.distanceMeters, signal.timeSeconds);
        }
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

    private int nextBlockedAreaLevel(@NonNull LatLon anchor, long nowMs) {
        if (lastBlockedAreaCenter == null || lastBlockedAreaLevel <= 0) {
            return 1;
        }
        double distanceMeters = GeoMath.distanceMeters(
                lastBlockedAreaCenter.lat,
                lastBlockedAreaCenter.lon,
                anchor.lat,
                anchor.lon
        );
        boolean sameArea = distanceMeters <= BLOCKED_SAME_AREA_METERS;
        boolean quickNearbyRepeat = nowMs - lastBlockedAreaAtMs <= BLOCKED_QUICK_REPEAT_WINDOW_MS
                && distanceMeters <= BLOCKED_QUICK_REPEAT_NEARBY_METERS;
        if (sameArea || quickNearbyRepeat) {
            return Math.min(BLOCKED_POINT_COUNT_MAX, lastBlockedAreaLevel + 1);
        }
        return 1;
    }

    private void replaceNearbyBlockedPoints(@NonNull LatLon anchor) {
        blocked.removeIf(existing ->
                GeoMath.distanceMeters(existing.lat, existing.lon, anchor.lat, anchor.lon)
                        <= BLOCKED_QUICK_REPEAT_NEARBY_METERS
        );
    }

    private int blockedPointCountForLevel(int level) {
        return Math.max(1, Math.min(BLOCKED_POINT_COUNT_MAX, level));
    }

    private double blockedRadiusForLevel(int level) {
        return Math.min(
                BLOCKED_RADIUS_MAX_METERS,
                BLOCKED_RADIUS_BASE_METERS + ((Math.max(1, level) - 1) * BLOCKED_RADIUS_STEP_METERS)
        );
    }

    static final class Evaluation {
        private final boolean shouldRecalculateRoute;
        private final long suggestedUpdateIntervalMs;
        @NonNull
        final List<NavigationSession.TurnEvent> turnEvents;

        private Evaluation(
                boolean shouldRecalculateRoute,
                long suggestedUpdateIntervalMs,
                @NonNull List<NavigationSession.TurnEvent> turnEvents
        ) {
            this.shouldRecalculateRoute = shouldRecalculateRoute;
            this.suggestedUpdateIntervalMs = suggestedUpdateIntervalMs;
            this.turnEvents = turnEvents;
        }

        @NonNull
        static Evaluation requestRecalculation() {
            return new Evaluation(true, NO_SUGGESTED_INTERVAL, Collections.emptyList());
        }

        @NonNull
        static Evaluation keepRoute(
                @NonNull List<NavigationSession.TurnEvent> turnEvents,
                long suggestedUpdateIntervalMs
        ) {
            return new Evaluation(false, suggestedUpdateIntervalMs, turnEvents);
        }

        boolean shouldRecalculateRoute() {
            return shouldRecalculateRoute;
        }

        long getSuggestedUpdateIntervalMs() {
            return suggestedUpdateIntervalMs;
        }
    }
}
