package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.policy.NavigationSpeedBucket;

public final class CompassDisplayMode {
    public final float headingDegrees;
    @Nullable
    public final Float headingAccuracyDegrees;
    public final float referenceSpeedMps;
    public final float fullRouteReferenceSpeedMps;
    public final float movingScaleReferenceSpeedMps;
    public final float movingScaleHorizonSeconds;
    @NonNull
    public final NavigationSpeedBucket movingScaleSpeedBucket;
    public final boolean movingScaleActive;
    public final boolean straightLineMode;

    public CompassDisplayMode(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float fullRouteReferenceSpeedMps,
            float movingScaleReferenceSpeedMps,
            boolean movingScaleActive
    ) {
        this(
                headingDegrees,
                headingAccuracyDegrees,
                referenceSpeedMps,
                fullRouteReferenceSpeedMps,
                movingScaleReferenceSpeedMps,
                CompassMovingScaleHorizon.secondsFor(CompassMovingScaleHorizon.DEFAULT_SPEED_BUCKET),
                CompassMovingScaleHorizon.DEFAULT_SPEED_BUCKET,
                movingScaleActive,
                false
        );
    }

    public CompassDisplayMode(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float fullRouteReferenceSpeedMps,
            float movingScaleReferenceSpeedMps,
            float movingScaleHorizonSeconds,
            @NonNull NavigationSpeedBucket movingScaleSpeedBucket,
            boolean movingScaleActive,
            boolean straightLineMode
    ) {
        this.headingDegrees = headingDegrees;
        this.headingAccuracyDegrees = headingAccuracyDegrees;
        this.referenceSpeedMps = referenceSpeedMps;
        this.fullRouteReferenceSpeedMps = fullRouteReferenceSpeedMps;
        this.movingScaleReferenceSpeedMps = movingScaleReferenceSpeedMps;
        this.movingScaleHorizonSeconds = movingScaleHorizonSeconds;
        this.movingScaleSpeedBucket = movingScaleSpeedBucket;
        this.movingScaleActive = movingScaleActive;
        this.straightLineMode = straightLineMode;
    }
}
