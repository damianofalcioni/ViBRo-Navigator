package vibro.navigator.nav.compass;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class CompassRadiusResolver {
    private static final float MOVING_LOOKAHEAD_SECONDS = 60f;
    private static final float MIN_VISIBLE_RADIUS_METERS = 90f;
    private static final long SMOOTHING_TIME_CONSTANT_MS = 450L;

    private CompassRadiusResolver() {
    }

    @NonNull
    public static State resolve(
            double furthestDistanceMeters,
            @NonNull NavigationLocation currentLocation,
            float speedMps,
            boolean likelyStationary,
            @Nullable Float previousVisibleRadiusMeters,
            @Nullable Float previousReliableMovingRadiusMeters,
            long updateDeltaMs,
            @Nullable CompassRadiusTransition transition,
            long nowMs
    ) {
        float fullRouteVisibleRadiusMeters = (float) Math.max(
                MIN_VISIBLE_RADIUS_METERS,
                furthestDistanceMeters * 1.15
        );
        boolean reliableMovingSpeed = hasReliableMovingSpeed(currentLocation, likelyStationary);
        boolean reusableMovingRadius = isReusableMovingRadius(previousReliableMovingRadiusMeters);
        float targetVisibleRadiusMeters = resolveTargetVisibleRadiusMeters(
                fullRouteVisibleRadiusMeters,
                speedMps,
                likelyStationary,
                reliableMovingSpeed,
                previousReliableMovingRadiusMeters,
                reusableMovingRadius
        );
        boolean fullRouteOverview = likelyStationary
                || (!reliableMovingSpeed && !reusableMovingRadius);
        float visibleRadiusMeters = resolveVisibleRadiusMeters(
                fullRouteVisibleRadiusMeters,
                targetVisibleRadiusMeters,
                reliableMovingSpeed,
                reusableMovingRadius,
                previousVisibleRadiusMeters,
                updateDeltaMs,
                transition,
                fullRouteOverview,
                nowMs
        );
        return new State(
                fullRouteVisibleRadiusMeters,
                visibleRadiusMeters,
                resolveSixtySecondVisibleRadiusMeters(
                        fullRouteVisibleRadiusMeters,
                        speedMps,
                        previousReliableMovingRadiusMeters,
                        reusableMovingRadius
                ),
                !likelyStationary && (reliableMovingSpeed || reusableMovingRadius)
        );
    }

    public static boolean hasReliableMovingSpeed(
            @NonNull NavigationLocation currentLocation,
            boolean likelyStationary
    ) {
        return !likelyStationary
                && currentLocation.hasSpeed()
                && Float.isFinite(currentLocation.getSpeed())
                && currentLocation.getSpeed() > 0f;
    }

    public static float smoothVisibleRadiusMeters(
            float targetVisibleRadiusMeters,
            @Nullable Float previousVisibleRadiusMeters,
            long updateDeltaMs
    ) {
        if (previousVisibleRadiusMeters == null
                || !Float.isFinite(previousVisibleRadiusMeters)
                || previousVisibleRadiusMeters <= 0f
                || updateDeltaMs <= 0L) {
            return targetVisibleRadiusMeters;
        }
        long boundedDeltaMs = Math.min(updateDeltaMs, 5_000L);
        double alpha = 1.0 - Math.exp(-boundedDeltaMs / (double) SMOOTHING_TIME_CONSTANT_MS);
        return (float) (previousVisibleRadiusMeters
                + (targetVisibleRadiusMeters - previousVisibleRadiusMeters) * alpha);
    }

    public static float movingLegendReferenceSpeedMps(float visibleRadiusMeters) {
        return movingLegendReferenceSpeedMps(visibleRadiusMeters, MIN_VISIBLE_RADIUS_METERS);
    }

    public static float movingLegendReferenceSpeedMps(float visibleRadiusMeters, float fallbackRadiusMeters) {
        float safeRadiusMeters = Float.isFinite(visibleRadiusMeters) && visibleRadiusMeters > 0f
                ? visibleRadiusMeters
                : fallbackRadiusMeters;
        return Math.max(1f, safeRadiusMeters / MOVING_LOOKAHEAD_SECONDS);
    }

    private static boolean isReusableMovingRadius(@Nullable Float radiusMeters) {
        return radiusMeters != null && Float.isFinite(radiusMeters) && radiusMeters > 0f;
    }

    private static float resolveTargetVisibleRadiusMeters(
            float fullRouteVisibleRadiusMeters,
            float speedMps,
            boolean likelyStationary,
            boolean reliableMovingSpeed,
            @Nullable Float previousReliableMovingRadiusMeters,
            boolean reusableMovingRadius
    ) {
        if (likelyStationary) {
            return fullRouteVisibleRadiusMeters;
        }
        if (reliableMovingSpeed) {
            return Math.min(fullRouteVisibleRadiusMeters, movingVisibleRadiusMeters(speedMps));
        }
        if (reusableMovingRadius) {
            return Math.min(fullRouteVisibleRadiusMeters, previousReliableMovingRadiusMeters);
        }
        return fullRouteVisibleRadiusMeters;
    }

    private static float resolveSixtySecondVisibleRadiusMeters(
            float fullRouteVisibleRadiusMeters,
            float speedMps,
            @Nullable Float previousReliableMovingRadiusMeters,
            boolean reusableMovingRadius
    ) {
        float movingRadiusMeters = reusableMovingRadius
                ? previousReliableMovingRadiusMeters
                : movingVisibleRadiusMeters(speedMps);
        return Math.min(fullRouteVisibleRadiusMeters, movingRadiusMeters);
    }

    private static float resolveVisibleRadiusMeters(
            float fullRouteVisibleRadiusMeters,
            float targetVisibleRadiusMeters,
            boolean reliableMovingSpeed,
            boolean reusableMovingRadius,
            @Nullable Float previousVisibleRadiusMeters,
            long updateDeltaMs,
            @Nullable CompassRadiusTransition transition,
            boolean fullRouteOverview,
            long nowMs
    ) {
        if (fullRouteOverview) {
            return resolveFullRouteOverviewRadius(
                    fullRouteVisibleRadiusMeters,
                    previousVisibleRadiusMeters,
                    transition,
                    nowMs
            );
        }
        if (transition != null) {
            transition.reset();
        }
        if (reusableMovingRadius && !reliableMovingSpeed) {
            return targetVisibleRadiusMeters;
        }
        return smoothVisibleRadiusMeters(targetVisibleRadiusMeters, previousVisibleRadiusMeters, updateDeltaMs);
    }

    private static float resolveFullRouteOverviewRadius(
            float fullRouteVisibleRadiusMeters,
            @Nullable Float previousVisibleRadiusMeters,
            @Nullable CompassRadiusTransition transition,
            long nowMs
    ) {
        if (transition == null) {
            return fullRouteVisibleRadiusMeters;
        }
        return transition.resolve(
                previousVisibleRadiusMeters != null
                        ? previousVisibleRadiusMeters
                        : fullRouteVisibleRadiusMeters,
                fullRouteVisibleRadiusMeters,
                isReusableMovingRadius(previousVisibleRadiusMeters),
                nowMs
        );
    }

    private static float movingVisibleRadiusMeters(float speedMps) {
        float safeSpeedMps = Float.isFinite(speedMps) && speedMps > 0f ? speedMps : 0f;
        float targetRadiusMeters = safeSpeedMps * MOVING_LOOKAHEAD_SECONDS;
        return Math.max(MIN_VISIBLE_RADIUS_METERS, targetRadiusMeters);
    }

    public static final class State {
        public final float fullRouteVisibleRadiusMeters;
        public final float visibleRadiusMeters;
        public final float sixtySecondVisibleRadiusMeters;
        public final boolean usingMovingScale;

        public State(
                float fullRouteVisibleRadiusMeters,
                float visibleRadiusMeters,
                float sixtySecondVisibleRadiusMeters,
                boolean usingMovingScale
        ) {
            this.fullRouteVisibleRadiusMeters = fullRouteVisibleRadiusMeters;
            this.visibleRadiusMeters = visibleRadiusMeters;
            this.sixtySecondVisibleRadiusMeters = sixtySecondVisibleRadiusMeters;
            this.usingMovingScale = usingMovingScale;
        }
    }
}
