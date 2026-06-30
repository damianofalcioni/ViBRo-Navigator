package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class SurroundingStreetSpeedBucketResolver {
    private static final float KMH_PER_MPS = 3.6f;
    private static final float LOW_TO_MEDIUM_KMH = 43f;
    private static final float MEDIUM_TO_LOW_KMH = 36f;
    private static final float MEDIUM_TO_HIGH_KMH = 84f;
    private static final float HIGH_TO_MEDIUM_KMH = 72f;
    private static final float LOW_MEDIUM_BOUNDARY_KMH = 40f;
    private static final float MEDIUM_HIGH_BOUNDARY_KMH = 80f;

    @NonNull
    SurroundingStreetSpeedBucket resolve(
            float speedMps,
            @Nullable SurroundingStreetSpeedBucket currentBucket
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
    private static SurroundingStreetSpeedBucket resolveInitial(float speedKmh) {
        if (speedKmh >= MEDIUM_HIGH_BOUNDARY_KMH) {
            return SurroundingStreetSpeedBucket.HIGH;
        }
        return speedKmh >= LOW_MEDIUM_BOUNDARY_KMH
                ? SurroundingStreetSpeedBucket.MEDIUM
                : SurroundingStreetSpeedBucket.LOW;
    }

    @NonNull
    private static SurroundingStreetSpeedBucket resolveFromLow(float speedKmh) {
        if (speedKmh >= MEDIUM_TO_HIGH_KMH) {
            return SurroundingStreetSpeedBucket.HIGH;
        }
        return speedKmh >= LOW_TO_MEDIUM_KMH
                ? SurroundingStreetSpeedBucket.MEDIUM
                : SurroundingStreetSpeedBucket.LOW;
    }

    @NonNull
    private static SurroundingStreetSpeedBucket resolveFromMedium(float speedKmh) {
        if (speedKmh < MEDIUM_TO_LOW_KMH) {
            return SurroundingStreetSpeedBucket.LOW;
        }
        return speedKmh >= MEDIUM_TO_HIGH_KMH
                ? SurroundingStreetSpeedBucket.HIGH
                : SurroundingStreetSpeedBucket.MEDIUM;
    }

    @NonNull
    private static SurroundingStreetSpeedBucket resolveFromHigh(float speedKmh) {
        if (speedKmh < MEDIUM_TO_LOW_KMH) {
            return SurroundingStreetSpeedBucket.LOW;
        }
        return speedKmh < HIGH_TO_MEDIUM_KMH
                ? SurroundingStreetSpeedBucket.MEDIUM
                : SurroundingStreetSpeedBucket.HIGH;
    }
}
