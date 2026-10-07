package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class NavigationStationarityTracker {
    private static final float MAX_STATIONARY_SPEED_MPS = 0.35f;
    private static final float MAX_UNCONFIRMED_START_SPEED_MPS = 2.5f;
    private static final float MAX_MOVEMENT_POSITION_ACCURACY_METERS = 25f;
    private static final double MIN_CONFIRMED_DISPLACEMENT_METERS = 1.5;

    private boolean stationary;
    private long lastSampleTimeMs = Long.MIN_VALUE;
    @Nullable
    private NavigationLocation stopReference;

    void reset() {
        stationary = false;
        lastSampleTimeMs = Long.MIN_VALUE;
        stopReference = null;
    }

    boolean isStationary() {
        return stationary;
    }

    void record(@NonNull NavigationLocation location, @Nullable NavigationLocation previous,
            boolean stationaryCandidate) {
        long sampleTimeMs = location.getElapsedRealtimeOrTimeMs();
        if (sampleTimeMs <= lastSampleTimeMs) {
            return;
        }
        lastSampleTimeMs = sampleTimeMs;
        NavigationLocation reference = stopReference == null ? previous : stopReference;
        if (hasConfidentMovingSpeed(location) || hasClearDisplacement(reference, location)) {
            stationary = false;
            stopReference = null;
            return;
        }
        if (shouldRetainStop(location, previous, stationaryCandidate)) {
            stationary = true;
            retainBestStopReference(location);
        }
    }

    private boolean shouldRetainStop(NavigationLocation location,
            @Nullable NavigationLocation previous, boolean stationaryCandidate) {
        return stationary || stationaryCandidate || isUncertainFirstFix(location, previous);
    }

    private static boolean isUncertainFirstFix(NavigationLocation location,
            @Nullable NavigationLocation previous) {
        return previous == null && location.getSpeed() <= MAX_UNCONFIRMED_START_SPEED_MPS
                && accuracyMeters(location) > MAX_MOVEMENT_POSITION_ACCURACY_METERS;
    }

    private void retainBestStopReference(NavigationLocation location) {
        // Keep the stop origin through ambiguous fixes so slow movement can accumulate.
        if (stopReference == null || accuracyMeters(location) < accuracyMeters(stopReference)) {
            stopReference = new NavigationLocation(location);
        }
    }

    private static boolean hasConfidentMovingSpeed(NavigationLocation location) {
        if (!location.hasSpeed() || !location.hasSpeedAccuracy()) {
            return false;
        }
        float speed = location.getSpeed();
        float uncertainty = location.getSpeedAccuracyMetersPerSecond();
        // Confirm nonzero motion; applying the travel-speed threshold to the
        // uncertainty bound too needlessly delays reliable slow walking.
        return Float.isFinite(speed) && validAccuracy(uncertainty)
                && speed > MAX_STATIONARY_SPEED_MPS && speed - 2.0 * uncertainty > 0.0;
    }

    private static boolean hasClearDisplacement(@Nullable NavigationLocation reference,
            NavigationLocation location) {
        if (reference == null || location.getElapsedRealtimeOrTimeMs()
                <= reference.getElapsedRealtimeOrTimeMs()) {
            return false;
        }
        double uncertainty = accuracyMeters(reference) + accuracyMeters(location);
        if (!Double.isFinite(uncertainty)
                || accuracyMeters(location) > MAX_MOVEMENT_POSITION_ACCURACY_METERS) {
            return false;
        }
        return reference.distanceTo(location) > Math.max(MIN_CONFIRMED_DISPLACEMENT_METERS, uncertainty);
    }

    private static float accuracyMeters(NavigationLocation location) {
        float accuracy = location.getAccuracy();
        return location.hasAccuracy() && validAccuracy(accuracy) ? accuracy : Float.POSITIVE_INFINITY;
    }

    private static boolean validAccuracy(float accuracy) {
        return Float.isFinite(accuracy) && accuracy >= 0f;
    }
}
