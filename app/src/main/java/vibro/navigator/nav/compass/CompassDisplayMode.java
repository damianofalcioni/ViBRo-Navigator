package vibro.navigator.nav.compass;

import androidx.annotation.Nullable;

public final class CompassDisplayMode {
    public final float headingDegrees;
    @Nullable
    public final Float headingAccuracyDegrees;
    public final float referenceSpeedMps;
    public final float fullRouteReferenceSpeedMps;
    public final float sixtySecondReferenceSpeedMps;
    public final boolean movingScaleActive;
    public final boolean straightLineMode;

    public CompassDisplayMode(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float fullRouteReferenceSpeedMps,
            float sixtySecondReferenceSpeedMps,
            boolean movingScaleActive
    ) {
        this(
                headingDegrees,
                headingAccuracyDegrees,
                referenceSpeedMps,
                fullRouteReferenceSpeedMps,
                sixtySecondReferenceSpeedMps,
                movingScaleActive,
                false
        );
    }

    public CompassDisplayMode(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float fullRouteReferenceSpeedMps,
            float sixtySecondReferenceSpeedMps,
            boolean movingScaleActive,
            boolean straightLineMode
    ) {
        this.headingDegrees = headingDegrees;
        this.headingAccuracyDegrees = headingAccuracyDegrees;
        this.referenceSpeedMps = referenceSpeedMps;
        this.fullRouteReferenceSpeedMps = fullRouteReferenceSpeedMps;
        this.sixtySecondReferenceSpeedMps = sixtySecondReferenceSpeedMps;
        this.movingScaleActive = movingScaleActive;
        this.straightLineMode = straightLineMode;
    }
}
