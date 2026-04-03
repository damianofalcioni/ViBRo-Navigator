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
        final double offTrackThresholdMeters;
        @Nullable
        final Double bearingDiffDegrees;

        private Decision(
                Reason reason,
                double offTrackThresholdMeters,
                @Nullable Double bearingDiffDegrees
        ) {
            this.reason = reason;
            this.offTrackThresholdMeters = offTrackThresholdMeters;
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
            return new Decision(Reason.OFF_TRACK, offTrackThresholdMeters, null);
        }

        if (actualBearingDegrees == null) {
            return new Decision(Reason.NONE, offTrackThresholdMeters, null);
        }

        double bearingDiffDegrees = GeoMath.angularDiffDegrees(actualBearingDegrees, expectedBearingDegrees);
        if (bearingDiffDegrees > BEARING_MISMATCH_THRESHOLD_DEGREES) {
            return new Decision(Reason.BEARING_MISMATCH, offTrackThresholdMeters, bearingDiffDegrees);
        }
        return new Decision(Reason.NONE, offTrackThresholdMeters, bearingDiffDegrees);
    }
}
