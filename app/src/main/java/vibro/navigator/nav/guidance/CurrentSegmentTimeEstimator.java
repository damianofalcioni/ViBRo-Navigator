package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;

final class CurrentSegmentTimeEstimator {
    private static final double MIN_ACCELERATION_ABS_METERS_PER_SECOND2 = 0.05;
    private static final double MAX_ACCELERATION_ABS_METERS_PER_SECOND2 = 3.0;
    private static final double MAX_ACCELERATION_ETA_CORRECTION_RATIO = 0.30;

    private CurrentSegmentTimeEstimator() {
    }

    static double estimateSeconds(double distanceMeters, @NonNull RouteMotionEstimate motionEstimate) {
        double safeDistanceMeters = Math.max(0.0, distanceMeters);
        if (safeDistanceMeters <= 0.0) {
            return 0.0;
        }
        double constantSpeedSeconds = safeDistanceMeters / motionEstimate.speedMps;
        if (!hasUsableAcceleration(motionEstimate)) {
            return constantSpeedSeconds;
        }
        double acceleratedSeconds = estimateAcceleratedSeconds(
                safeDistanceMeters,
                motionEstimate.speedMps,
                boundedAccelerationMetersPerSecond2(motionEstimate.accelerationMps2)
        );
        return clampAccelerationAdjustedSeconds(acceleratedSeconds, constantSpeedSeconds);
    }

    private static boolean hasUsableAcceleration(@NonNull RouteMotionEstimate motionEstimate) {
        return motionEstimate.hasAcceleration()
                && Math.abs(motionEstimate.accelerationMps2) >= MIN_ACCELERATION_ABS_METERS_PER_SECOND2;
    }

    private static double boundedAccelerationMetersPerSecond2(float accelerationMps2) {
        return Math.max(
                -MAX_ACCELERATION_ABS_METERS_PER_SECOND2,
                Math.min(MAX_ACCELERATION_ABS_METERS_PER_SECOND2, accelerationMps2)
        );
    }

    private static double estimateAcceleratedSeconds(double distanceMeters, double speedMps, double accelerationMps2) {
        double discriminant = speedMps * speedMps + 2.0 * accelerationMps2 * distanceMeters;
        if (discriminant < 0.0) {
            return Double.NaN;
        }
        double root = Math.sqrt(discriminant);
        return (-speedMps + root) / accelerationMps2;
    }

    private static double clampAccelerationAdjustedSeconds(double acceleratedSeconds, double constantSpeedSeconds) {
        double minimumSeconds = constantSpeedSeconds * (1.0 - MAX_ACCELERATION_ETA_CORRECTION_RATIO);
        double maximumSeconds = constantSpeedSeconds * (1.0 + MAX_ACCELERATION_ETA_CORRECTION_RATIO);
        if (!Double.isFinite(acceleratedSeconds) || acceleratedSeconds <= 0.0) {
            return maximumSeconds;
        }
        return Math.max(minimumSeconds, Math.min(maximumSeconds, acceleratedSeconds));
    }
}
