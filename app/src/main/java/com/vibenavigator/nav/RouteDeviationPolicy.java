package com.vibenavigator.nav;

import androidx.annotation.Nullable;

import com.vibenavigator.geo.GeoMath;

final class RouteDeviationPolicy {

    private static final double BASE_OFF_TRACK_THRESHOLD_METERS = 10.0;
    private static final double DEFAULT_ACCURACY_METERS = 20.0;
    private static final double BEARING_MISMATCH_THRESHOLD_DEGREES = 60.0;

    enum Reason {
        NONE,
        OFF_TRACK,
        BEARING_MISMATCH
    }

    static final class Decision {
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

        boolean shouldRecalculateRoute() {
            return reason != Reason.NONE;
        }
    }

    Decision evaluate(
            double distanceToTrackMeters,
            float accuracyMeters,
            @Nullable Double actualBearingDegrees,
            double expectedBearingDegrees
    ) {
        double safeAccuracyMeters = accuracyMeters > 0f ? accuracyMeters : DEFAULT_ACCURACY_METERS;
        double offTrackThresholdMeters = BASE_OFF_TRACK_THRESHOLD_METERS + safeAccuracyMeters;
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
}
