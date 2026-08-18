package vibro.navigator.nav.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassHeadingRefresh;
import vibro.navigator.nav.model.NavState;

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

    boolean canRefreshHeadingOnly(@Nullable CompassOrientationCue orientationCue) {
        return currentState != null
                && sameCue(sourceOrientationCue, orientationCue)
                && !hasPendingRadiusChange(currentState);
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
