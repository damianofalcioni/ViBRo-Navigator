package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.CompassRadiusTransition;

final class CompassZoomAnimationPolicy {
    private CompassZoomAnimationPolicy() {
    }

    @Nullable
    static Float previousVisibleRadius(
            boolean animationEnabled,
            @Nullable Float previousVisibleRadiusMeters
    ) {
        return animationEnabled ? previousVisibleRadiusMeters : null;
    }

    static long updateDeltaMs(boolean animationEnabled, long updateDeltaMs) {
        return animationEnabled ? updateDeltaMs : 0L;
    }

    @Nullable
    static CompassRadiusTransition transition(
            boolean animationEnabled,
            @NonNull CompassRadiusTransition transition
    ) {
        if (animationEnabled) {
            return transition;
        }
        transition.reset();
        return null;
    }
}
