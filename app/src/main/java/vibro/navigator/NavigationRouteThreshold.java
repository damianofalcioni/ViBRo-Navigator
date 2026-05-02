package vibro.navigator;


import vibro.navigator.nav.compass.NavCompassState;
import androidx.annotation.Nullable;


final class NavigationRouteThreshold {

    private NavigationRouteThreshold() {
    }

    static float resolveStrokeWidthPx(
            @Nullable NavCompassState compassState,
            float routeRadius,
            float baseStrokeWidthPx
    ) {
        if (compassState == null
                || compassState.radiusState.visibleRadiusMeters <= 0f
                || compassState.radiusState.routeThresholdMeters <= 0f) {
            return baseStrokeWidthPx;
        }

        float corridorHalfWidthMeters = Math.max(
                0f,
                compassState.radiusState.routeThresholdMeters - compassState.radiusState.accuracyRadiusMeters
        );
        if (corridorHalfWidthMeters <= 0f) {
            return 0f;
        }

        float projectedThresholdWidthPx =
                (2f * corridorHalfWidthMeters / compassState.radiusState.visibleRadiusMeters) * routeRadius;
        return Math.max(
                0f,
                Math.min(routeRadius * 2f, projectedThresholdWidthPx)
        );
    }

    static boolean shouldDrawOverlay(@Nullable NavCompassState compassState) {
        return compassState != null
                && compassState.radiusState.routeThresholdMeters > compassState.radiusState.accuracyRadiusMeters
                && compassState.radiusState.routeThresholdMeters > 0f
                && compassState.radiusState.visibleRadiusMeters > 0f;
    }
}
