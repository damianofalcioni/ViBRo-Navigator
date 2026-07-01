package vibro.navigator.nav.policy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationSpeedBucketResolver {
    private static final float KMH_PER_MPS = 3.6f;
    private static final float LOW_TO_MEDIUM_KMH = 43f;
    private static final float MEDIUM_TO_LOW_KMH = 36f;
    private static final float MEDIUM_TO_HIGH_KMH = 84f;
    private static final float HIGH_TO_MEDIUM_KMH = 72f;
    private static final float LOW_MEDIUM_BOUNDARY_KMH = 40f;
    private static final float MEDIUM_HIGH_BOUNDARY_KMH = 80f;

    @NonNull
    public NavigationSpeedBucket resolve(
            float speedMps,
            @Nullable NavigationSpeedBucket currentBucket
    ) {
        float speedKmh = speedKmh(speedMps);
        if (currentBucket == null) {
            return resolveInitial(speedKmh);
        }
        switch (currentBucket) {
            case LOW:
                return resolveFromLow(speedKmh);
            case MEDIUM:
                return resolveFromMedium(speedKmh);
            case HIGH:
                return resolveFromHigh(speedKmh);
            default:
                return resolveInitial(speedKmh);
        }
    }

    private static float speedKmh(float speedMps) {
        return Float.isFinite(speedMps) && speedMps > 0f ? speedMps * KMH_PER_MPS : 0f;
    }

    @NonNull
    private static NavigationSpeedBucket resolveInitial(float speedKmh) {
        if (speedKmh >= MEDIUM_HIGH_BOUNDARY_KMH) {
            return NavigationSpeedBucket.HIGH;
        }
        return speedKmh >= LOW_MEDIUM_BOUNDARY_KMH
                ? NavigationSpeedBucket.MEDIUM
                : NavigationSpeedBucket.LOW;
    }

    @NonNull
    private static NavigationSpeedBucket resolveFromLow(float speedKmh) {
        if (speedKmh >= MEDIUM_TO_HIGH_KMH) {
            return NavigationSpeedBucket.HIGH;
        }
        return speedKmh >= LOW_TO_MEDIUM_KMH
                ? NavigationSpeedBucket.MEDIUM
                : NavigationSpeedBucket.LOW;
    }

    @NonNull
    private static NavigationSpeedBucket resolveFromMedium(float speedKmh) {
        if (speedKmh < MEDIUM_TO_LOW_KMH) {
            return NavigationSpeedBucket.LOW;
        }
        return speedKmh >= MEDIUM_TO_HIGH_KMH
                ? NavigationSpeedBucket.HIGH
                : NavigationSpeedBucket.MEDIUM;
    }

    @NonNull
    private static NavigationSpeedBucket resolveFromHigh(float speedKmh) {
        if (speedKmh < MEDIUM_TO_LOW_KMH) {
            return NavigationSpeedBucket.LOW;
        }
        return speedKmh < HIGH_TO_MEDIUM_KMH
                ? NavigationSpeedBucket.MEDIUM
                : NavigationSpeedBucket.HIGH;
    }
}
