package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.policy.NavigationSpeedBucket;
import vibro.navigator.nav.policy.NavigationSpeedBucketResolver;

final class CompassMovingScalePolicy {
    @NonNull
    private static final NavigationSpeedBucketResolver SPEED_BUCKET_RESOLVER =
            new NavigationSpeedBucketResolver();

    private CompassMovingScalePolicy() {
    }

    @NonNull
    static State resolve(
            float speedMps,
            @Nullable NavigationSpeedBucket previousMovingSpeedBucket,
            boolean reliableMovingSpeed,
            boolean reusableMovingRadius
    ) {
        NavigationSpeedBucket bucket = resolveSpeedBucket(
                speedMps,
                previousMovingSpeedBucket,
                reliableMovingSpeed,
                reusableMovingRadius
        );
        return new State(bucket, CompassMovingScaleHorizon.secondsFor(bucket));
    }

    static float visibleRadiusMeters(
            float speedMps,
            float movingScaleHorizonSeconds,
            float minimumVisibleRadiusMeters
    ) {
        float safeSpeedMps = Float.isFinite(speedMps) && speedMps > 0f ? speedMps : 0f;
        float targetRadiusMeters = safeSpeedMps * safeHorizonSeconds(movingScaleHorizonSeconds);
        return Math.max(minimumVisibleRadiusMeters, targetRadiusMeters);
    }

    static float referenceSpeedMps(
            float visibleRadiusMeters,
            float movingScaleHorizonSeconds,
            float fallbackRadiusMeters
    ) {
        float safeRadiusMeters = Float.isFinite(visibleRadiusMeters) && visibleRadiusMeters > 0f
                ? visibleRadiusMeters
                : fallbackRadiusMeters;
        return Math.max(1f, safeRadiusMeters / safeHorizonSeconds(movingScaleHorizonSeconds));
    }

    @NonNull
    private static NavigationSpeedBucket resolveSpeedBucket(
            float speedMps,
            @Nullable NavigationSpeedBucket previousMovingSpeedBucket,
            boolean reliableMovingSpeed,
            boolean reusableMovingRadius
    ) {
        if (!reliableMovingSpeed && reusableMovingRadius && previousMovingSpeedBucket != null) {
            return previousMovingSpeedBucket;
        }
        return SPEED_BUCKET_RESOLVER.resolve(speedMps, previousMovingSpeedBucket);
    }

    private static float safeHorizonSeconds(float movingScaleHorizonSeconds) {
        return Float.isFinite(movingScaleHorizonSeconds) && movingScaleHorizonSeconds > 0f
                ? movingScaleHorizonSeconds
                : CompassMovingScaleHorizon.secondsFor(CompassMovingScaleHorizon.DEFAULT_SPEED_BUCKET);
    }

    static final class State {
        @NonNull
        final NavigationSpeedBucket speedBucket;
        final float horizonSeconds;

        State(@NonNull NavigationSpeedBucket speedBucket, float horizonSeconds) {
            this.speedBucket = speedBucket;
            this.horizonSeconds = horizonSeconds;
        }
    }
}
