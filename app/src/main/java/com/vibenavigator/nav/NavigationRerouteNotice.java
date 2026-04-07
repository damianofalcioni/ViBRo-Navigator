package com.vibenavigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class NavigationRerouteNotice {

    @NonNull
    final RouteDeviationPolicy.Reason reason;
    final double distanceToTrackMeters;
    final double offTrackThresholdMeters;
    @Nullable
    final Double bearingDiffDegrees;
    final double expectedBearingDegrees;
    @Nullable
    final Double actualBearingDegrees;

    private NavigationRerouteNotice(
            @NonNull RouteDeviationPolicy.Reason reason,
            double distanceToTrackMeters,
            double offTrackThresholdMeters,
            @Nullable Double bearingDiffDegrees,
            double expectedBearingDegrees,
            @Nullable Double actualBearingDegrees
    ) {
        this.reason = reason;
        this.distanceToTrackMeters = distanceToTrackMeters;
        this.offTrackThresholdMeters = offTrackThresholdMeters;
        this.bearingDiffDegrees = bearingDiffDegrees;
        this.expectedBearingDegrees = expectedBearingDegrees;
        this.actualBearingDegrees = actualBearingDegrees;
    }

    @NonNull
    static NavigationRerouteNotice fromDecision(@NonNull RouteDeviationPolicy.Decision decision) {
        return new NavigationRerouteNotice(
                decision.reason,
                decision.distanceToTrackMeters,
                decision.offTrackThresholdMeters,
                decision.bearingDiffDegrees,
                decision.expectedBearingDegrees,
                decision.actualBearingDegrees
        );
    }
}
