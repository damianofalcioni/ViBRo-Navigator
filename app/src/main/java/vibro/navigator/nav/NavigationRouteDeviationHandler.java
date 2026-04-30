package vibro.navigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.util.AppLogger;

final class NavigationRouteDeviationHandler {

    private static final String TAG = "NavRouteDeviation";

    private final RouteDeviationPolicy routeDeviationPolicy = new RouteDeviationPolicy();
    private final NavigationDeviationConfirmation deviationConfirmation = new NavigationDeviationConfirmation();
    private final NavigationRouteProgressTracker progressTracker;

    NavigationRouteDeviationHandler(@NonNull NavigationRouteProgressTracker progressTracker) {
        this.progressTracker = progressTracker;
    }

    void clearDeviationEvidence() {
        deviationConfirmation.clear();
    }

    @NonNull
    Decision evaluate(
            @NonNull PolylineIndex.Match match,
            double smoothedAccuracyMeters,
            @NonNull NavigationRouteProgressTracker.DirectionAssessment directionOfProgress,
            float speedMps,
            double expectedBearingDegrees,
            @Nullable Double actualBearingDegrees,
            long nowMs
    ) {
        RouteDeviationPolicy.Decision deviationDecision = routeDeviationPolicy.evaluate(
                match.distanceToTrackMeters,
                smoothedAccuracyMeters,
                actualBearingDegrees,
                expectedBearingDegrees
        );

        Decision bearingMismatchProgress = evaluateBearingMismatchProgress(
                deviationDecision,
                directionOfProgress,
                match,
                nowMs
        );
        if (bearingMismatchProgress != null) {
            return bearingMismatchProgress;
        }

        if (deviationDecision.reason == RouteDeviationPolicy.Reason.NONE) {
            deviationConfirmation.clear();
            return Decision.continueOnRoute();
        }

        if (!deviationConfirmation.isConfirmed(deviationDecision, speedMps)) {
            logTentativeDeviation(deviationDecision, directionOfProgress, match);
            progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            return Decision.keepCurrentRoute(false);
        }

        logConfirmedDeviation(deviationDecision, directionOfProgress, match, expectedBearingDegrees, actualBearingDegrees);
        deviationConfirmation.clear();
        progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
        return Decision.requestRouteRecalculation(NavigationRerouteNotice.fromDecision(deviationDecision));
    }

    @Nullable
    private Decision evaluateBearingMismatchProgress(
            @NonNull RouteDeviationPolicy.Decision deviationDecision,
            @NonNull NavigationRouteProgressTracker.DirectionAssessment directionOfProgress,
            @NonNull PolylineIndex.Match match,
            long nowMs
    ) {
        if (deviationDecision.reason != RouteDeviationPolicy.Reason.BEARING_MISMATCH) {
            return null;
        }
        if (directionOfProgress.status == NavigationRouteProgressTracker.DirectionStatus.FORWARD) {
            AppLogger.i(TAG, "Ignoring bearing mismatch because along-track progress is forward delta="
                    + directionOfProgress.alongTrackDeltaMeters);
            deviationConfirmation.clear();
            progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            return Decision.keepCurrentRoute(true);
        }
        if (directionOfProgress.status == NavigationRouteProgressTracker.DirectionStatus.UNKNOWN) {
            AppLogger.i(TAG, "Holding bearing mismatch until direction-of-progress is known");
            deviationConfirmation.clear();
            progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            return Decision.keepCurrentRoute(false);
        }
        return null;
    }

    private void logTentativeDeviation(
            @NonNull RouteDeviationPolicy.Decision deviationDecision,
            @NonNull NavigationRouteProgressTracker.DirectionAssessment directionOfProgress,
            @NonNull PolylineIndex.Match match
    ) {
        AppLogger.i(TAG, "Tentative deviation detected reason=" + deviationDecision.reason
                + " distance=" + match.distanceToTrackMeters
                + " threshold=" + deviationDecision.offTrackThresholdMeters
                + " bearingDiff=" + deviationDecision.bearingDiffDegrees
                + " direction=" + directionOfProgress.status
                + " alongTrackDelta=" + directionOfProgress.alongTrackDeltaMeters
                + " samples=" + deviationConfirmation.pendingSampleCount());
    }

    private void logConfirmedDeviation(
            @NonNull RouteDeviationPolicy.Decision deviationDecision,
            @NonNull NavigationRouteProgressTracker.DirectionAssessment directionOfProgress,
            @NonNull PolylineIndex.Match match,
            double expectedBearingDegrees,
            @Nullable Double actualBearingDegrees
    ) {
        if (deviationDecision.reason == RouteDeviationPolicy.Reason.OFF_TRACK) {
            AppLogger.w(TAG, "Off-track detected distance=" + match.distanceToTrackMeters
                    + " threshold=" + deviationDecision.offTrackThresholdMeters);
        } else if (deviationDecision.reason == RouteDeviationPolicy.Reason.BEARING_MISMATCH) {
            AppLogger.w(TAG, "Bearing mismatch detected diff=" + deviationDecision.bearingDiffDegrees
                    + " expected=" + expectedBearingDegrees
                    + " actual=" + actualBearingDegrees
                    + " direction=" + directionOfProgress.status
                    + " alongTrackDelta=" + directionOfProgress.alongTrackDeltaMeters);
        }
    }

    static final class Decision {
        private final boolean keepCurrentRoute;
        private final boolean stableOnRouteSample;
        @Nullable
        private final NavigationRerouteNotice rerouteNotice;

        private Decision(
                boolean keepCurrentRoute,
                boolean stableOnRouteSample,
                @Nullable NavigationRerouteNotice rerouteNotice
        ) {
            this.keepCurrentRoute = keepCurrentRoute;
            this.stableOnRouteSample = stableOnRouteSample;
            this.rerouteNotice = rerouteNotice;
        }

        @NonNull
        static Decision continueOnRoute() {
            return new Decision(false, true, null);
        }

        @NonNull
        static Decision keepCurrentRoute(boolean stableOnRouteSample) {
            return new Decision(true, stableOnRouteSample, null);
        }

        @NonNull
        static Decision requestRouteRecalculation(@NonNull NavigationRerouteNotice rerouteNotice) {
            return new Decision(false, false, rerouteNotice);
        }

        boolean shouldKeepCurrentRoute() {
            return keepCurrentRoute;
        }

        boolean isStableOnRouteSample() {
            return stableOnRouteSample;
        }

        boolean shouldRecalculateRoute() {
            return rerouteNotice != null;
        }

        @Nullable
        NavigationRerouteNotice getRerouteNotice() {
            return rerouteNotice;
        }
    }
}
