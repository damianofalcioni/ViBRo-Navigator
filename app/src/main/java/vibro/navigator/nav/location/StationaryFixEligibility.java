package vibro.navigator.nav.location;

import java.util.Objects;

/** Missing native speed is unknown; an armed motion trigger and position history must prove a stop. */
final class StationaryFixEligibility {
    private StationaryFixEligibility() { }

    static String rejection(NavigationLocation raw, NavigationLocation filtered, long nowMs) {
        if (!fresh(raw, nowMs)) {
            return "measurement_not_fresh";
        }
        if (!sameObservation(raw, filtered)) {
            return "observation_mismatch";
        }
        if (NavigationLocationProviders.NETWORK_PROVIDER.equals(raw.getProvider())) {
            return "network_only";
        }
        if (!accurate(raw) || !accurate(filtered)) {
            return "position_accuracy";
        }
        if (!(raw.distanceTo(filtered) <= 3f)) {
            return "position_disagreement";
        }
        return speedRejection(raw);
    }

    private static boolean sameObservation(NavigationLocation raw, NavigationLocation filtered) {
        return Objects.equals(raw.getProvider(), filtered.getProvider())
                && raw.getElapsedRealtimeMs() == filtered.getElapsedRealtimeMs();
    }

    private static boolean fresh(NavigationLocation raw, long nowMs) {
        long ageMs = nowMs - raw.getElapsedRealtimeMs();
        return raw.getElapsedRealtimeMs() >= 0L && ageMs >= 0L && ageMs <= 5_000L;
    }

    private static boolean accurate(NavigationLocation fix) {
        return fix.hasAccuracy() && Float.isFinite(fix.getAccuracy())
                && fix.getAccuracy() > 0f && fix.getAccuracy() <= 20f;
    }

    private static String speedRejection(NavigationLocation fix) {
        if (fix.hasSpeed() && !stoppedSpeed(fix)) {
            return "native_speed_moving_or_invalid";
        }
        if (fix.hasSpeedAccuracy() && !usableSpeedAccuracy(fix.getSpeedAccuracyMetersPerSecond())) {
            return "speed_accuracy";
        }
        return null;
    }

    private static boolean stoppedSpeed(NavigationLocation fix) {
        float speed = fix.getSpeed();
        return Float.isFinite(speed) && speed >= 0f && speed <= 0.3f
                && !NavigationNativeSpeedEvidence.hasConfidentPositiveSpeed(fix);
    }

    private static boolean usableSpeedAccuracy(float accuracy) {
        return Float.isFinite(accuracy) && accuracy >= 0f && accuracy <= 0.5f;
    }
}
