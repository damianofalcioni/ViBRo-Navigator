package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationRerouteNotice {

    @NonNull
    public final RouteDeviationPolicy.Reason reason;
    public final double distanceToTrackMeters;
    public final double offTrackThresholdMeters;
    @Nullable
    public final Double bearingDiffDegrees;
    public final double expectedBearingDegrees;
    @Nullable
    public final Double actualBearingDegrees;
    public final boolean routeRecalculationExpected;

    private NavigationRerouteNotice(
            @NonNull RouteDeviationPolicy.Reason reason,
            double distanceToTrackMeters,
            double offTrackThresholdMeters,
            @Nullable Double bearingDiffDegrees,
            double expectedBearingDegrees,
            @Nullable Double actualBearingDegrees,
            boolean routeRecalculationExpected
    ) {
        this.reason = reason;
        this.distanceToTrackMeters = distanceToTrackMeters;
        this.offTrackThresholdMeters = offTrackThresholdMeters;
        this.bearingDiffDegrees = bearingDiffDegrees;
        this.expectedBearingDegrees = expectedBearingDegrees;
        this.actualBearingDegrees = actualBearingDegrees;
        this.routeRecalculationExpected = routeRecalculationExpected;
    }

    @NonNull
    public static NavigationRerouteNotice fromDecision(@NonNull RouteDeviationPolicy.Decision decision) {
        return new NavigationRerouteNotice(
                decision.reason,
                decision.distanceToTrackMeters,
                decision.offTrackThresholdMeters,
                decision.bearingDiffDegrees,
                decision.expectedBearingDegrees,
                decision.actualBearingDegrees,
                true
        );
    }

    @NonNull
    public NavigationRerouteNotice asNotificationOnly() {
        if (!routeRecalculationExpected) {
            return this;
        }
        return new NavigationRerouteNotice(
                reason,
                distanceToTrackMeters,
                offTrackThresholdMeters,
                bearingDiffDegrees,
                expectedBearingDegrees,
                actualBearingDegrees,
                false
        );
    }
}
