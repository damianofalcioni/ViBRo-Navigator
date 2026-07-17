package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.logging.AppLogger;

public final class NavigationRouteDeviationHandler {

    private static final String TAG = "NavRouteDeviation";

    private final RouteDeviationPolicy routeDeviationPolicy = new RouteDeviationPolicy();
    private final NavigationDeviationConfirmation deviationConfirmation = new NavigationDeviationConfirmation();
    private final NavigationRouteProgressTracker progressTracker;

    public NavigationRouteDeviationHandler(@NonNull NavigationRouteProgressTracker progressTracker) {
        this.progressTracker = progressTracker;
    }

    public void clearDeviationEvidence() {
        deviationConfirmation.clear();
    }

    @NonNull
    public Decision evaluate(
            @NonNull PolylineIndex.Match match,
            double smoothedAccuracyMeters,
            @NonNull NavigationRouteProgressTracker.DirectionAssessment directionOfProgress,
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

        if (!deviationConfirmation.isConfirmed(deviationDecision, nowMs)) {
            logTentativeDeviation(deviationDecision, directionOfProgress, match);
            progressTracker.rememberAlongTrackSample(match.alongTrackMeters, nowMs);
            return Decision.waitForDeviationConfirmation(deviationDecision.reason);
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

    public static final class Decision {
        private final boolean keepCurrentRoute;
        private final boolean stableOnRouteSample;
        private final boolean deviationConfirmationPending;
        @Nullable
        private final RouteDeviationPolicy.Reason pendingDeviationReason;
        @Nullable
        private final NavigationRerouteNotice rerouteNotice;

        private Decision(
                boolean keepCurrentRoute,
                boolean stableOnRouteSample,
                boolean deviationConfirmationPending,
                @Nullable RouteDeviationPolicy.Reason pendingDeviationReason,
                @Nullable NavigationRerouteNotice rerouteNotice
        ) {
            this.keepCurrentRoute = keepCurrentRoute;
            this.stableOnRouteSample = stableOnRouteSample;
            this.deviationConfirmationPending = deviationConfirmationPending;
            this.pendingDeviationReason = pendingDeviationReason;
            this.rerouteNotice = rerouteNotice;
        }

        @NonNull
        public static Decision continueOnRoute() {
            return new Decision(false, true, false, null, null);
        }

        @NonNull
        public static Decision keepCurrentRoute(boolean stableOnRouteSample) {
            return new Decision(true, stableOnRouteSample, false, null, null);
        }

        @NonNull
        public static Decision waitForDeviationConfirmation(
                @NonNull RouteDeviationPolicy.Reason pendingDeviationReason
        ) {
            return new Decision(true, false, true, pendingDeviationReason, null);
        }

        @NonNull
        public static Decision requestRouteRecalculation(@NonNull NavigationRerouteNotice rerouteNotice) {
            return new Decision(false, false, false, null, rerouteNotice);
        }

        public boolean shouldKeepCurrentRoute() {
            return keepCurrentRoute;
        }

        public boolean isStableOnRouteSample() {
            return stableOnRouteSample;
        }

        public boolean isDeviationConfirmationPending() {
            return deviationConfirmationPending;
        }

        @Nullable
        public RouteDeviationPolicy.Reason getPendingDeviationReason() {
            return pendingDeviationReason;
        }

        public boolean shouldRecalculateRoute() {
            return rerouteNotice != null;
        }

        @Nullable
        public NavigationRerouteNotice getRerouteNotice() {
            return rerouteNotice;
        }
    }
}
