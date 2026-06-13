package vibro.navigator.nav.guidance;

import androidx.annotation.Nullable;

public final class NavigationWrongDirectionNotice {
    public final double expectedBearingDegrees;
    @Nullable
    public final Double actualBearingDegrees;
    public final double bearingDiffDegrees;

    public NavigationWrongDirectionNotice(
            double expectedBearingDegrees,
            @Nullable Double actualBearingDegrees,
            double bearingDiffDegrees
    ) {
        this.expectedBearingDegrees = expectedBearingDegrees;
        this.actualBearingDegrees = actualBearingDegrees;
        this.bearingDiffDegrees = bearingDiffDegrees;
    }
}
