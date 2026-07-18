package vibro.navigator.nav.guidance;

final class TurnDistanceReliability {
    private static final double MIN_TRUSTED_TURN_DISTANCE_METERS = 5.0;
    private static final double MIN_SLOW_SPEED_TURN_DISTANCE_METERS = 0.75;
    static final double MIN_ACTIONABLE_NOTICE_SECONDS = 2.0;

    private TurnDistanceReliability() {
    }

    static boolean isInitialReliable(double distanceToNextMeters, float accuracyMeters) {
        double safeAccuracyMeters = Float.isFinite(accuracyMeters) && accuracyMeters > 0f
                ? accuracyMeters
                : 0.0;
        double minTrustedDistanceMeters = Math.max(MIN_TRUSTED_TURN_DISTANCE_METERS, safeAccuracyMeters);
        return distanceToNextMeters > minTrustedDistanceMeters;
    }

    static boolean isImminentReliable(
            double distanceToNextMeters,
            float speedMps,
            double timeToNextSeconds
    ) {
        return distanceToNextMeters > minimumTrustedImminentDistanceMeters(
                speedMps,
                distanceToNextMeters,
                timeToNextSeconds
        );
    }

    private static double minimumTrustedImminentDistanceMeters(
            float speedMps,
            double distanceToNextMeters,
            double timeToNextSeconds
    ) {
        double effectiveSpeedMps = resolveEffectiveSpeedMps(speedMps, distanceToNextMeters, timeToNextSeconds);
        if (effectiveSpeedMps <= 0.0) {
            return MIN_TRUSTED_TURN_DISTANCE_METERS;
        }
        double slowSpeedDistanceMeters = Math.max(
                MIN_SLOW_SPEED_TURN_DISTANCE_METERS,
                effectiveSpeedMps * MIN_ACTIONABLE_NOTICE_SECONDS
        );
        return Math.min(MIN_TRUSTED_TURN_DISTANCE_METERS, slowSpeedDistanceMeters);
    }

    private static double resolveEffectiveSpeedMps(
            float speedMps,
            double distanceToNextMeters,
            double timeToNextSeconds
    ) {
        if (Float.isFinite(speedMps) && speedMps > 0f) {
            return speedMps;
        }
        if (!Double.isFinite(timeToNextSeconds) || timeToNextSeconds <= 0.0) {
            return 0.0;
        }
        return distanceToNextMeters / timeToNextSeconds;
    }
}
