package vibro.navigator.nav.kalman;

import androidx.annotation.Nullable;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;

/** A delayed network observation can refine position, but must never become a new motion sample. */
final class DelayedNetworkCorrectionPolicy {
    // Limit the static-position approximation to a few seconds and charge for possible travel.
    private static final long MAX_DELAY_MS = 3_000L;
    private static final long MAX_AGE_MS = 5_000L;
    private static final double MIN_TRAVEL_ALLOWANCE_MPS = 5;
    private static final double REQUIRED_IMPROVEMENT_METERS = 15;

    private DelayedNetworkCorrectionPolicy() { }

    static double effectiveAccuracy(@Nullable NavigationLocation current, NavigationLocation network,
            long nowMs, long lastCorrectionTimeMs) {
        if (!validObservation(current, network)) {
            return Double.POSITIVE_INFINITY;
        }
        long sampleTime = network.getElapsedRealtimeOrTimeMs();
        long delayMs = current.getElapsedRealtimeOrTimeMs() - sampleTime;
        long ageMs = nowMs - sampleTime;
        if (!validTiming(sampleTime, delayMs, ageMs, lastCorrectionTimeMs)) {
            return Double.POSITIVE_INFINITY;
        }
        double speed = Math.max(MIN_TRAVEL_ALLOWANCE_MPS, LocationFilterPolicy.speed(current));
        double accuracy = LocationFilterPolicy.accuracy(network) + speed * delayMs / 1000.0;
        return isUseful(current, network, accuracy) ? accuracy : Double.POSITIVE_INFINITY;
    }

    private static boolean validObservation(@Nullable NavigationLocation current, NavigationLocation network) {
        return current != null && eligibleProviders(current, network)
                && network.hasAccuracy() && current.hasAccuracy() && LocationFilterPolicy.isValid(network);
    }

    private static boolean validTiming(long sampleTime, long delayMs, long ageMs, long lastCorrectionTimeMs) {
        return sampleTime > lastCorrectionTimeMs && delayMs > 0 && delayMs <= MAX_DELAY_MS
                && ageMs >= delayMs && ageMs <= MAX_AGE_MS;
    }

    private static boolean eligibleProviders(NavigationLocation current, NavigationLocation network) {
        return NavigationLocationProviders.GPS_PROVIDER.equals(current.getProvider())
                && NavigationLocationProviders.NETWORK_PROVIDER.equals(network.getProvider());
    }

    private static boolean isUseful(NavigationLocation current, NavigationLocation network, double accuracy) {
        return accuracy + REQUIRED_IMPROVEMENT_METERS < LocationFilterPolicy.accuracy(current)
                && current.distanceTo(network) <= LocationFilterPolicy.accuracy(current) + accuracy;
    }
}
