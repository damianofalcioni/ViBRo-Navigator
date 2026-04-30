package vibro.navigator.nav;

import androidx.annotation.NonNull;

final class NavigationDeviationConfirmation {

    private static final int DEVIATION_CONFIRMATION_SAMPLES = 2;
    private static final float WALKING_SPEED_MPS = 2.0f;
    private static final float FAST_TRAVEL_SPEED_MPS = 8.0f;
    private static final double LOW_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS = 12.0;
    private static final double MEDIUM_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS = 8.0;
    private static final double HIGH_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS = 5.0;

    @NonNull
    private RouteDeviationPolicy.Reason pendingDeviationReason = RouteDeviationPolicy.Reason.NONE;
    private int pendingDeviationSampleCount;

    boolean isConfirmed(@NonNull RouteDeviationPolicy.Decision decision, float speedMps) {
        if (decision.reason == RouteDeviationPolicy.Reason.OFF_TRACK
                && decision.distanceToTrackMeters >= decision.offTrackThresholdMeters
                + immediateOffTrackMarginMeters(speedMps)) {
            return true;
        }
        if (pendingDeviationReason != decision.reason) {
            pendingDeviationReason = decision.reason;
            pendingDeviationSampleCount = 1;
            return false;
        }
        pendingDeviationSampleCount++;
        return pendingDeviationSampleCount >= DEVIATION_CONFIRMATION_SAMPLES;
    }

    int pendingSampleCount() {
        return pendingDeviationSampleCount;
    }

    void clear() {
        pendingDeviationReason = RouteDeviationPolicy.Reason.NONE;
        pendingDeviationSampleCount = 0;
    }

    private double immediateOffTrackMarginMeters(float speedMps) {
        if (speedMps >= FAST_TRAVEL_SPEED_MPS) {
            return HIGH_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS;
        }
        if (speedMps >= WALKING_SPEED_MPS) {
            return MEDIUM_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS;
        }
        return LOW_SPEED_IMMEDIATE_OFF_TRACK_MARGIN_METERS;
    }
}
