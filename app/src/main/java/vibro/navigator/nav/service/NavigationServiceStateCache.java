package vibro.navigator.nav.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassHeadingRefresh;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavGpsTelemetry;
import vibro.navigator.nav.location.NavigationMotionState;

final class NavigationServiceStateCache {
    @Nullable
    private NavState currentState;
    @Nullable
    private CompassOrientationCue sourceOrientationCue;

    void storeStructuralState(
            @NonNull NavState state,
            @Nullable CompassOrientationCue orientationCue
    ) {
        currentState = state;
        sourceOrientationCue = orientationCue;
    }

    void storeHeadingState(@NonNull NavState state) {
        currentState = state;
    }

    @Nullable
    NavState currentState() {
        return currentState;
    }

    boolean canRefreshHeadingOnly(@Nullable CompassOrientationCue orientationCue, long nowMs) {
        return currentState != null
                && sameCue(sourceOrientationCue, orientationCue)
                && !hasExpiredLiveSpeed(currentState.gpsStatus.telemetry, nowMs)
                && !hasPendingRadiusChange(currentState);
    }

    private static boolean hasExpiredLiveSpeed(NavGpsTelemetry telemetry, long nowMs) {
        // Rebuild once at expiry. The rebuilt snapshot has unavailable speed,
        // so subsequent compass samples can keep using the lightweight path.
        return Float.isFinite(telemetry.speedMps) && telemetry.fixElapsedRealtimeMs >= 0
                && !NavigationMotionState.isFresh(telemetry.fixElapsedRealtimeMs, nowMs);
    }

    void clear() {
        currentState = null;
        sourceOrientationCue = null;
    }

    private static boolean hasPendingRadiusChange(@NonNull NavState state) {
        NavCompassState compassState = state.routeStatus.compassState;
        return compassState != null && NavCompassHeadingRefresh.hasPendingRadiusChange(compassState);
    }

    private static boolean sameCue(
            @Nullable CompassOrientationCue first,
            @Nullable CompassOrientationCue second
    ) {
        return first == null
                ? second == null
                : second != null
                && Float.compare(first.targetHeadingDegrees, second.targetHeadingDegrees) == 0;
    }
}
