package vibro.navigator.nav.location;

import androidx.annotation.NonNull;

final class NavigationStationarityTracker {
    private static final float MAX_MOVEMENT_POSITION_ACCURACY_METERS = 25f;

    private NavigationMotionState state = NavigationMotionState.UNKNOWN;
    private String reason = "no_evidence";
    private long lastSampleTimeMs = Long.MIN_VALUE;
    private final NavigationNativeSpeedEvidence nativeSpeedEvidence = new NavigationNativeSpeedEvidence();

    void reset() {
        state = NavigationMotionState.UNKNOWN;
        reason = "no_evidence";
        lastSampleTimeMs = Long.MIN_VALUE;
        nativeSpeedEvidence.reset();
    }

    boolean isStationary() {
        return state == NavigationMotionState.STATIONARY;
    }

    NavigationMotionState state() { return state; }

    NavigationMotionState state(long nowMs) {
        return lastSampleTimeMs != Long.MIN_VALUE && lastSampleTimeMs <= nowMs
                && nowMs - lastSampleTimeMs <= NavigationMotionState.MAX_EVIDENCE_AGE_MS
                ? state : NavigationMotionState.UNKNOWN;
    }

    String reason() { return reason; }

    void recordProviderChange(@NonNull NavigationLocation location) {
        // The old provider's coordinate frame cannot confirm movement or a stop here.
        nativeSpeedEvidence.reset();
        state = NavigationMotionState.UNKNOWN;
        reason = "provider_change";
        record(location, false, false);
    }

    void record(@NonNull NavigationLocation location, boolean stationaryCandidate, boolean coherentMovement) {
        long sampleTimeMs = location.getElapsedRealtimeOrTimeMs();
        if (sampleTimeMs <= lastSampleTimeMs) {
            return;
        }
        lastSampleTimeMs = sampleTimeMs;
        boolean nativeMovement = nativeSpeedEvidence.record(location);
        if (coherentMovement || nativeMovement) {
            state = NavigationMotionState.MOVING;
            reason = coherentMovement ? "coherent_raw_movement" : "independent_movement";
            return;
        }
        if (stationaryCandidate && accuracyMeters(location) <= MAX_MOVEMENT_POSITION_ACCURACY_METERS) {
            state = NavigationMotionState.STATIONARY;
            reason = "stationary_observation";
        } else {
            state = NavigationMotionState.UNKNOWN;
            reason = "movement_unconfirmed";
        }
    }

    boolean needsDepartureChecks() {
        return state != NavigationMotionState.MOVING;
    }

    private static float accuracyMeters(NavigationLocation location) {
        float accuracy = location.getAccuracy();
        return location.hasAccuracy() && validAccuracy(accuracy) ? accuracy : Float.POSITIVE_INFINITY;
    }

    private static boolean validAccuracy(float accuracy) {
        return Float.isFinite(accuracy) && accuracy >= 0f;
    }
}
