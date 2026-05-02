package vibro.navigator.nav.guidance;

import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;

public final class RouteDeviationPolicy {

    private static final double MIN_OFF_TRACK_THRESHOLD_METERS = 10.0;
    private static final double OFF_TRACK_ACCURACY_SLACK_METERS = 8.0;
    private static final double DEFAULT_ACCURACY_METERS = 20.0;
    private static final double BEARING_MISMATCH_THRESHOLD_DEGREES = 60.0;
    private static final double MAX_TRUSTED_BEARING_ACCURACY_METERS = 15.0;

    public enum Reason {
        NONE,
        OFF_TRACK,
        BEARING_MISMATCH
    }

    public static final class Decision {
        final Reason reason;
        final double distanceToTrackMeters;
        final double offTrackThresholdMeters;
        final double expectedBearingDegrees;
        @Nullable
        final Double actualBearingDegrees;
        @Nullable
        final Double bearingDiffDegrees;

        private Decision(
                Reason reason,
                double distanceToTrackMeters,
                double offTrackThresholdMeters,
                double expectedBearingDegrees,
                @Nullable Double actualBearingDegrees,
                @Nullable Double bearingDiffDegrees
        ) {
            this.reason = reason;
            this.distanceToTrackMeters = distanceToTrackMeters;
            this.offTrackThresholdMeters = offTrackThresholdMeters;
            this.expectedBearingDegrees = expectedBearingDegrees;
            this.actualBearingDegrees = actualBearingDegrees;
            this.bearingDiffDegrees = bearingDiffDegrees;
        }

        public boolean shouldRecalculateRoute() {
            return reason != Reason.NONE;
        }
    }

    public Decision evaluate(
            double distanceToTrackMeters,
            double accuracyMeters,
            @Nullable Double actualBearingDegrees,
            double expectedBearingDegrees
    ) {
        double safeAccuracyMeters = accuracyMeters > 0.0 ? accuracyMeters : DEFAULT_ACCURACY_METERS;
        double offTrackThresholdMeters = resolveOffTrackThresholdMeters(safeAccuracyMeters);
        if (distanceToTrackMeters > offTrackThresholdMeters) {
            return new Decision(
                    Reason.OFF_TRACK,
                    distanceToTrackMeters,
                    offTrackThresholdMeters,
                    expectedBearingDegrees,
                    actualBearingDegrees,
                    null
            );
        }

        if (actualBearingDegrees == null) {
            return new Decision(
                    Reason.NONE,
                    distanceToTrackMeters,
                    offTrackThresholdMeters,
                    expectedBearingDegrees,
                    null,
                    null
            );
        }
        if (safeAccuracyMeters > MAX_TRUSTED_BEARING_ACCURACY_METERS) {
            return new Decision(
                    Reason.NONE,
                    distanceToTrackMeters,
                    offTrackThresholdMeters,
                    expectedBearingDegrees,
                    actualBearingDegrees,
                    null
            );
        }

        double bearingDiffDegrees = GeoMath.angularDiffDegrees(actualBearingDegrees, expectedBearingDegrees);
        if (bearingDiffDegrees > BEARING_MISMATCH_THRESHOLD_DEGREES) {
            return new Decision(
                    Reason.BEARING_MISMATCH,
                    distanceToTrackMeters,
                    offTrackThresholdMeters,
                    expectedBearingDegrees,
                    actualBearingDegrees,
                    bearingDiffDegrees
            );
        }
        return new Decision(
                Reason.NONE,
                distanceToTrackMeters,
                offTrackThresholdMeters,
                expectedBearingDegrees,
                actualBearingDegrees,
                bearingDiffDegrees
        );
    }

    public static double resolveOffTrackThresholdMeters(double accuracyMeters) {
        double safeAccuracyMeters = accuracyMeters > 0.0 ? accuracyMeters : DEFAULT_ACCURACY_METERS;
        return Math.max(
                safeAccuracyMeters + OFF_TRACK_ACCURACY_SLACK_METERS,
                MIN_OFF_TRACK_THRESHOLD_METERS
        );
    }
}
