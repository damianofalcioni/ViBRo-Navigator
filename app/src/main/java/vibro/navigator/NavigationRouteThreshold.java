package vibro.navigator;

import androidx.annotation.Nullable;

import vibro.navigator.nav.NavCompassState;

final class NavigationRouteThreshold {

    private NavigationRouteThreshold() {
    }

    static float resolveStrokeWidthPx(
            @Nullable NavCompassState compassState,
            float routeRadius,
            float baseStrokeWidthPx
    ) {
        if (compassState == null
                || compassState.visibleRadiusMeters <= 0f
                || compassState.routeThresholdMeters <= 0f) {
            return baseStrokeWidthPx;
        }

        float corridorHalfWidthMeters = Math.max(
                0f,
                compassState.routeThresholdMeters - compassState.accuracyRadiusMeters
        );
        if (corridorHalfWidthMeters <= 0f) {
            return 0f;
        }

        float projectedThresholdWidthPx =
                (2f * corridorHalfWidthMeters / compassState.visibleRadiusMeters) * routeRadius;
        return Math.max(
                0f,
                Math.min(routeRadius * 2f, projectedThresholdWidthPx)
        );
    }

    static boolean shouldDrawOverlay(@Nullable NavCompassState compassState) {
        return compassState != null
                && compassState.routeThresholdMeters > compassState.accuracyRadiusMeters
                && compassState.routeThresholdMeters > 0f
                && compassState.visibleRadiusMeters > 0f;
    }
}
