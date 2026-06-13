package vibro.navigator.nav.guidance;

import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;

public final class StraightLineWrongDirectionDetector {
    private static final double WRONG_DIRECTION_THRESHOLD_DEGREES = 135.0;
    private static final double MIN_TARGET_DISTANCE_METERS = 30.0;
    private static final double MIN_TARGET_DISTANCE_ACCURACY_FACTOR = 2.0;
    private static final float MIN_MOVING_SPEED_MPS = 0.8f;
    private static final int CONFIRMATION_SAMPLES = 2;

    private int consecutiveWrongDirectionSamples;
    private boolean notificationActive;

    @Nullable
    public NavigationWrongDirectionNotice evaluate(
            double distanceToTargetMeters,
            float accuracyMeters,
            float speedMps,
            @Nullable Double expectedBearingDegrees,
            @Nullable Double actualBearingDegrees
    ) {
        if (!hasUsableEvidence(
                distanceToTargetMeters,
                accuracyMeters,
                speedMps,
                expectedBearingDegrees,
                actualBearingDegrees
        )) {
            clear();
            return null;
        }

        double bearingDiffDegrees = GeoMath.angularDiffDegrees(actualBearingDegrees, expectedBearingDegrees);
        if (bearingDiffDegrees <= WRONG_DIRECTION_THRESHOLD_DEGREES) {
            clear();
            return null;
        }

        consecutiveWrongDirectionSamples++;
        if (consecutiveWrongDirectionSamples < CONFIRMATION_SAMPLES || notificationActive) {
            return null;
        }

        notificationActive = true;
        return new NavigationWrongDirectionNotice(
                expectedBearingDegrees,
                actualBearingDegrees,
                bearingDiffDegrees
        );
    }

    public void reset() {
        clear();
    }

    private boolean hasUsableEvidence(
            double distanceToTargetMeters,
            float accuracyMeters,
            float speedMps,
            @Nullable Double expectedBearingDegrees,
            @Nullable Double actualBearingDegrees
    ) {
        return expectedBearingDegrees != null
                && actualBearingDegrees != null
                && Double.isFinite(expectedBearingDegrees)
                && Double.isFinite(actualBearingDegrees)
                && Double.isFinite(distanceToTargetMeters)
                && distanceToTargetMeters > minimumTargetDistanceMeters(accuracyMeters)
                && Float.isFinite(speedMps)
                && speedMps >= MIN_MOVING_SPEED_MPS;
    }

    private static double minimumTargetDistanceMeters(float accuracyMeters) {
        double safeAccuracyMeters = Float.isFinite(accuracyMeters) && accuracyMeters > 0f
                ? accuracyMeters
                : 0.0;
        return Math.max(MIN_TARGET_DISTANCE_METERS, safeAccuracyMeters * MIN_TARGET_DISTANCE_ACCURACY_FACTOR);
    }

    private void clear() {
        consecutiveWrongDirectionSamples = 0;
        notificationActive = false;
    }
}
