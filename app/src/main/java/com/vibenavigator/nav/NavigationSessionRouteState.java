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
import com.vibenavigator.util.AppLogger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class NavigationSessionRouteState {

    private static final String TAG = "NavSessionRoute";
    private static final long NO_SUGGESTED_INTERVAL = -1L;
    private static final long NO_COMPASS_RADIUS_UPDATE_TIME_MS = -1L;
    private static final int DEVIATION_CONFIRMATION_SAMPLES = 2;
    private static final long MAX_ACCURACY_SAMPLE_AGE_MS = 5_000L;
    private static final float WALKING_SPEED_MPS = 2.0f;
    private static final float FAST_TRAVEL_SPEED_MPS = 8.0f;
    private static final double LOW_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS = 12.0;
    private static final double MEDIUM_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS = 8.0;
    private static final double HIGH_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS = 5.0;
    private static final double EXPECTED_BEARING_LOOKAHEAD_METERS = 20.0;
    private static final double MIN_EXPECTED_BEARING_BASELINE_METERS = 3.0;
    private static final long DIRECTION_PROGRESS_WINDOW_MS = 3_000L;
    private static final long MAX_DIRECTION_PROGRESS_SAMPLE_AGE_MS = 10_000L;
    private static final double MIN_DIRECTION_PROGRESS_METERS = 4.0;

    private final NavigationBlockedRouteState blockedRouteState = new NavigationBlockedRouteState();
    private final RouteDeviationPolicy routeDeviationPolicy = new RouteDeviationPolicy();
    private final NavigationTurnState turnState = new NavigationTurnState();
    private final ArrayDeque<AccuracySample> recentAccuracySamples = new ArrayDeque<>();
    private final ArrayDeque<AlongTrackSample> recentAlongTrackSamples = new ArrayDeque<>();

    @Nullable
    private GeoJsonRoute route;
    @Nullable
    private PolylineIndex polylineIndex;
    private int lastSegmentIndex = -1;
    @NonNull
    private List<NavTarget> targets = new ArrayList<>();
    @Nullable
    private Float lastCompassVisibleRadiusMeters;
    private long lastCompassRadiusUpdateTimeMs = NO_COMPASS_RADIUS_UPDATE_TIME_MS;
    @NonNull
    private RouteDeviationPolicy.Reason pendingDeviationReason = RouteDeviationPolicy.Reason.NONE;
    private int pendingDeviationSampleCount;

    void reset() {
        route = null;
        polylineIndex = null;
        lastSegmentIndex = -1;
        targets = new ArrayList<>();
        lastCompassVisibleRadiusMeters = null;
        lastCompassRadiusUpdateTimeMs = NO_COMPASS_RADIUS_UPDATE_TIME_MS;
        clearPendingDeviation();
        recentAccuracySamples.clear();
        recentAlongTrackSamples.clear();
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
        double expectedBearingDegrees = expectedBearingDegrees(match);
        double smoothedAccuracyMeters = rememberAndResolveSmoothedAccuracyMeters(accuracyMeters, nowMs);
        DirectionOfProgressAssessment directionOfProgress = assessDirectionOfProgress(
                match.alongTrackMeters,
                nowMs
        );

        RouteDeviationPolicy.Decision deviationDecision = routeDeviationPolicy.evaluate(
                match.distanceToTrackMeters,
                smoothedAccuracyMeters,
                actualBearingDegrees,
                expectedBearingDegrees
        );
        if (deviationDecision.reason == RouteDeviationPolicy.Reason.BEARING_MISMATCH) {
            if (directionOfProgress.status == DirectionOfProgressStatus.FORWARD) {
                AppLogger.i(TAG, "Ignoring bearing mismatch because along-track progress is forward delta="
                        + directionOfProgress.alongTrackDeltaMeters);
                clearPendingDeviation();
                rememberAlongTrackSample(match.alongTrackMeters, nowMs);
                NavigationTurnState.Progress progress = turnState.evaluate(
                        route,
                        polylineIndex,
                        match.alongTrackMeters,
                        speedMps,
                        accuracyMeters,
                        nowMs,
                        fastChecksUntilMs
                );
                return Evaluation.keepRoute(progress.turnEvents, progress.suggestedUpdateIntervalMs, true);
            }
            if (directionOfProgress.status == DirectionOfProgressStatus.UNKNOWN) {
                AppLogger.i(TAG, "Holding bearing mismatch until direction-of-progress is known");
                clearPendingDeviation();
                rememberAlongTrackSample(match.alongTrackMeters, nowMs);
                NavigationTurnState.Progress progress = turnState.evaluate(
                        route,
                        polylineIndex,
                        match.alongTrackMeters,
                        speedMps,
                        accuracyMeters,
                        nowMs,
                        fastChecksUntilMs
                );
                return Evaluation.keepRoute(progress.turnEvents, progress.suggestedUpdateIntervalMs, false);
            }
        }
        if (deviationDecision.reason == RouteDeviationPolicy.Reason.NONE) {
            clearPendingDeviation();
        } else if (!isConfirmedDeviation(deviationDecision, speedMps)) {
            AppLogger.i(TAG, "Tentative deviation detected reason=" + deviationDecision.reason
                    + " distance=" + match.distanceToTrackMeters
                    + " threshold=" + deviationDecision.offTrackThresholdMeters
                    + " bearingDiff=" + deviationDecision.bearingDiffDegrees
                    + " direction=" + directionOfProgress.status
                    + " alongTrackDelta=" + directionOfProgress.alongTrackDeltaMeters
                    + " samples=" + pendingDeviationSampleCount);
            rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            NavigationTurnState.Progress progress = turnState.evaluate(
                    route,
                    polylineIndex,
                    match.alongTrackMeters,
                    speedMps,
                    accuracyMeters,
                    nowMs,
                    fastChecksUntilMs
            );
            return Evaluation.keepRoute(progress.turnEvents, progress.suggestedUpdateIntervalMs, false);
        }
        if (deviationDecision.reason == RouteDeviationPolicy.Reason.OFF_TRACK) {
            AppLogger.w(TAG, "Off-track detected distance=" + match.distanceToTrackMeters
                    + " threshold=" + deviationDecision.offTrackThresholdMeters);
            clearPendingDeviation();
            rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            return Evaluation.requestRecalculation(NavigationRerouteNotice.fromDecision(deviationDecision));
        }
        if (deviationDecision.reason == RouteDeviationPolicy.Reason.BEARING_MISMATCH) {
            AppLogger.w(TAG, "Bearing mismatch detected diff=" + deviationDecision.bearingDiffDegrees
                    + " expected=" + expectedBearingDegrees
                    + " actual=" + actualBearingDegrees
                    + " direction=" + directionOfProgress.status
                    + " alongTrackDelta=" + directionOfProgress.alongTrackDeltaMeters);
            clearPendingDeviation();
            rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            return Evaluation.requestRecalculation(NavigationRerouteNotice.fromDecision(deviationDecision));
        }

        rememberAlongTrackSample(match.alongTrackMeters, nowMs);
        NavigationTurnState.Progress progress = turnState.evaluate(
                route,
                polylineIndex,
                match.alongTrackMeters,
                speedMps,
                accuracyMeters,
                nowMs,
                fastChecksUntilMs
        );
        return Evaluation.keepRoute(progress.turnEvents, progress.suggestedUpdateIntervalMs, true);
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
        lastCompassVisibleRadiusMeters = null;
        lastCompassRadiusUpdateTimeMs = NO_COMPASS_RADIUS_UPDATE_TIME_MS;
        clearPendingDeviation();
        recentAccuracySamples.clear();
        recentAlongTrackSamples.clear();

        List<NavigationSession.TurnEvent> turnEvents =
                turnState.onRouteApplied(newRoute, polylineIndex, lastFiltered, speedMps, accuracyOf(lastFiltered));
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
            @Nullable Throwable lastRouteFailure
    ) {
        String gpsStatusLine = NavState.buildGpsStatusLine(
                lastFiltered == null ? Float.NaN : speedMps,
                lastFiltered,
                lastFiltered == null ? Float.NaN : accuracyMeters,
                fixedSatelliteCount,
                context
        );
        if (lastFiltered == null) {
            if (lastRouteFailure != null) {
                return NavState.withGpsStatus(NavState.routeUnavailable(
                        context,
                        NavigationRouteFailureFormatter.format(context, lastRouteFailure, false),
                        nextEvaluationDeadlineElapsedMs
                ), gpsStatusLine);
            }
            return NavState.withGpsStatus(
                    NavState.waitingForLocation(context, nextEvaluationDeadlineElapsedMs),
                    gpsStatusLine
            );
        }

        if (routeCalculationInProgress) {
            return NavState.withGpsStatus(
                    NavState.calculatingRoute(context, nextEvaluationDeadlineElapsedMs),
                    gpsStatusLine
            );
        }

        if (route == null || polylineIndex == null) {
            if (lastRouteFailure != null) {
                return NavState.withGpsStatus(NavState.routeUnavailable(
                        context,
                        NavigationRouteFailureFormatter.format(context, lastRouteFailure, false),
                        nextEvaluationDeadlineElapsedMs
                ), gpsStatusLine);
            }
            return NavState.withGpsStatus(
                    NavState.calculatingRoute(context, nextEvaluationDeadlineElapsedMs),
                    gpsStatusLine
            );
        }

        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                lastSegmentIndex
        );
        if (match == null) {
            return NavState.withGpsStatus(NavState.waiting(context), gpsStatusLine);
        }

        NavState state = NavState.from(
                route,
                polylineIndex,
                match.alongTrackMeters,
                turnState.getNextHintIdx(),
                speedMps,
                likelyStationary,
                accuracyMeters,
                lastFiltered,
                fixedSatelliteCount,
                headingDegrees,
                headingAccuracyDegrees,
                lastCompassVisibleRadiusMeters,
                resolveCompassRadiusUpdateDeltaMs(nowMs),
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                targets,
                context
        );
        rememberCompassVisibleRadius(state, nowMs);
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
        out.add(new NavTarget(context.getString(R.string.nav_destination_label), index.totalLengthMeters()));
        return out;
    }

    private float accuracyOf(@Nullable Location location) {
        return location != null && location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }

    private long resolveCompassRadiusUpdateDeltaMs(long nowMs) {
        if (lastCompassRadiusUpdateTimeMs == NO_COMPASS_RADIUS_UPDATE_TIME_MS || nowMs <= lastCompassRadiusUpdateTimeMs) {
            return 0L;
        }
        return nowMs - lastCompassRadiusUpdateTimeMs;
    }

    private void rememberCompassVisibleRadius(@NonNull NavState state, long nowMs) {
        if (state.compassState == null) {
            return;
        }
        lastCompassVisibleRadiusMeters = state.compassState.visibleRadiusMeters;
        lastCompassRadiusUpdateTimeMs = nowMs;
    }

    private double expectedBearingDegrees(@NonNull PolylineIndex.Match match) {
        if (polylineIndex == null) {
            return match.segmentBearingDegrees;
        }
        LatLon current = polylineIndex.pointAtDistance(match.alongTrackMeters);
        if (current == null) {
            return match.segmentBearingDegrees;
        }
        double lookaheadAlongTrackMeters = Math.min(
                polylineIndex.totalLengthMeters(),
                match.alongTrackMeters + EXPECTED_BEARING_LOOKAHEAD_METERS
        );
        LatLon ahead = polylineIndex.pointAtDistance(lookaheadAlongTrackMeters);
        if (ahead == null) {
            return match.segmentBearingDegrees;
        }
        double baselineMeters = GeoMath.distanceMeters(current.lat, current.lon, ahead.lat, ahead.lon);
        if (baselineMeters < MIN_EXPECTED_BEARING_BASELINE_METERS) {
            return match.segmentBearingDegrees;
        }
        return GeoMath.bearingDegrees(current.lat, current.lon, ahead.lat, ahead.lon);
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

    private boolean isConfirmedDeviation(@NonNull RouteDeviationPolicy.Decision decision, float speedMps) {
        if (decision.reason == RouteDeviationPolicy.Reason.OFF_TRACK
                && decision.distanceToTrackMeters >= decision.offTrackThresholdMeters
                + immediateOffTrackMarginMeters(speedMps)) {
            return true;
        }
        if (pendingDeviationReason != decision.reason) {
            pendingDeviationReason = decision.reason;
            pendingDeviationSampleCount = 1;
            return false;
        }
        pendingDeviationSampleCount++;
        return pendingDeviationSampleCount >= DEVIATION_CONFIRMATION_SAMPLES;
    }

    private void clearPendingDeviation() {
        pendingDeviationReason = RouteDeviationPolicy.Reason.NONE;
        pendingDeviationSampleCount = 0;
    }

    private double rememberAndResolveSmoothedAccuracyMeters(float accuracyMeters, long nowMs) {
        if (Float.isFinite(accuracyMeters) && accuracyMeters > 0f) {
            recentAccuracySamples.addLast(new AccuracySample(accuracyMeters, nowMs));
        }
        pruneAccuracySamples(nowMs);
        if (recentAccuracySamples.isEmpty()) {
            return accuracyMeters;
        }

        double[] samples = new double[recentAccuracySamples.size()];
        int idx = 0;
        for (AccuracySample sample : recentAccuracySamples) {
            samples[idx++] = sample.accuracyMeters;
        }
        java.util.Arrays.sort(samples);
        int middle = samples.length / 2;
        if ((samples.length & 1) == 1) {
            return samples[middle];
        }
        return (samples[middle - 1] + samples[middle]) / 2.0;
    }

    private void pruneAccuracySamples(long nowMs) {
        long cutoffMs = nowMs - MAX_ACCURACY_SAMPLE_AGE_MS;
        while (recentAccuracySamples.size() > 1
                && recentAccuracySamples.peekFirst() != null
                && recentAccuracySamples.peekFirst().timeMs < cutoffMs) {
            recentAccuracySamples.removeFirst();
        }
    }

    private double immediateOffTrackMarginMeters(float speedMps) {
        if (speedMps >= FAST_TRAVEL_SPEED_MPS) {
            return HIGH_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS;
        }
        if (speedMps >= WALKING_SPEED_MPS) {
            return MEDIUM_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS;
        }
        return LOW_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS;
    }

    @NonNull
    private DirectionOfProgressAssessment assessDirectionOfProgress(double alongTrackMeters, long nowMs) {
        pruneAlongTrackSamples(nowMs);
        AlongTrackSample anchor = null;
        for (AlongTrackSample candidate : recentAlongTrackSamples) {
            if (nowMs - candidate.timeMs >= DIRECTION_PROGRESS_WINDOW_MS) {
                anchor = candidate;
                break;
            }
        }
        if (anchor == null) {
            return DirectionOfProgressAssessment.unknown();
        }
        double deltaMeters = alongTrackMeters - anchor.alongTrackMeters;
        if (deltaMeters >= MIN_DIRECTION_PROGRESS_METERS) {
            return DirectionOfProgressAssessment.forward(deltaMeters);
        }
        if (deltaMeters <= -MIN_DIRECTION_PROGRESS_METERS) {
            return DirectionOfProgressAssessment.backward(deltaMeters);
        }
        return DirectionOfProgressAssessment.stalled(deltaMeters);
    }

    private void rememberAlongTrackSample(double alongTrackMeters, long nowMs) {
        recentAlongTrackSamples.addLast(new AlongTrackSample(alongTrackMeters, nowMs));
        pruneAlongTrackSamples(nowMs);
    }

    private void pruneAlongTrackSamples(long nowMs) {
        long cutoffMs = nowMs - MAX_DIRECTION_PROGRESS_SAMPLE_AGE_MS;
        while (recentAlongTrackSamples.size() > 1
                && recentAlongTrackSamples.peekFirst() != null
                && recentAlongTrackSamples.peekFirst().timeMs < cutoffMs) {
            recentAlongTrackSamples.removeFirst();
        }
    }

    private enum DirectionOfProgressStatus {
        UNKNOWN,
        FORWARD,
        BACKWARD,
        STALLED
    }

    private static final class DirectionOfProgressAssessment {
        @NonNull
        final DirectionOfProgressStatus status;
        final double alongTrackDeltaMeters;

        private DirectionOfProgressAssessment(
                @NonNull DirectionOfProgressStatus status,
                double alongTrackDeltaMeters
        ) {
            this.status = status;
            this.alongTrackDeltaMeters = alongTrackDeltaMeters;
        }

        @NonNull
        static DirectionOfProgressAssessment unknown() {
            return new DirectionOfProgressAssessment(DirectionOfProgressStatus.UNKNOWN, 0.0);
        }

        @NonNull
        static DirectionOfProgressAssessment forward(double alongTrackDeltaMeters) {
            return new DirectionOfProgressAssessment(DirectionOfProgressStatus.FORWARD, alongTrackDeltaMeters);
        }

        @NonNull
        static DirectionOfProgressAssessment backward(double alongTrackDeltaMeters) {
            return new DirectionOfProgressAssessment(DirectionOfProgressStatus.BACKWARD, alongTrackDeltaMeters);
        }

        @NonNull
        static DirectionOfProgressAssessment stalled(double alongTrackDeltaMeters) {
            return new DirectionOfProgressAssessment(DirectionOfProgressStatus.STALLED, alongTrackDeltaMeters);
        }
    }

    private static final class AlongTrackSample {
        final double alongTrackMeters;
        final long timeMs;

        private AlongTrackSample(double alongTrackMeters, long timeMs) {
            this.alongTrackMeters = alongTrackMeters;
            this.timeMs = timeMs;
        }
    }

    private static final class AccuracySample {
        final double accuracyMeters;
        final long timeMs;

        private AccuracySample(double accuracyMeters, long timeMs) {
            this.accuracyMeters = accuracyMeters;
            this.timeMs = timeMs;
        }
    }
}
