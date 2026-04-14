package com.vibenavigator.nav;

final class CompassRadiusTransition {

    private static final float TARGET_TOLERANCE_RATIO = 0.02f;
    private static final float TARGET_TOLERANCE_METERS = 12f;

    private final long durationMs;
    private boolean active;
    private float startRadiusMeters;
    private float targetRadiusMeters;
    private long startTimeMs;

    CompassRadiusTransition(long durationMs) {
        this.durationMs = durationMs;
    }

    void reset() {
        active = false;
        startRadiusMeters = 0f;
        targetRadiusMeters = 0f;
        startTimeMs = 0L;
    }

    float resolve(
            float currentRadiusMeters,
            float nextTargetRadiusMeters,
            boolean animate,
            long nowMs
    ) {
        if (!animate || durationMs <= 0L) {
            reset();
            return nextTargetRadiusMeters;
        }

        if (!active || !sameTarget(nextTargetRadiusMeters)) {
            startRadiusMeters = sanitizeRadius(currentRadiusMeters, nextTargetRadiusMeters);
            targetRadiusMeters = nextTargetRadiusMeters;
            startTimeMs = nowMs;
            active = Math.abs(targetRadiusMeters - startRadiusMeters) > 0.01f;
        }
        if (!active) {
            return targetRadiusMeters;
        }

        float progress = Math.min(1f, Math.max(0f, (nowMs - startTimeMs) / (float) durationMs));
        float easedProgress = progress * progress * (3f - 2f * progress);
        float resolvedRadiusMeters = startRadiusMeters
                + (targetRadiusMeters - startRadiusMeters) * easedProgress;
        if (progress >= 1f) {
            reset();
            return nextTargetRadiusMeters;
        }
        return resolvedRadiusMeters;
    }

    private boolean sameTarget(float nextTargetRadiusMeters) {
        float toleranceMeters = Math.max(
                TARGET_TOLERANCE_METERS,
                Math.abs(targetRadiusMeters) * TARGET_TOLERANCE_RATIO
        );
        return Math.abs(targetRadiusMeters - nextTargetRadiusMeters) <= toleranceMeters;
    }

    private static float sanitizeRadius(float currentRadiusMeters, float fallbackRadiusMeters) {
        return Float.isFinite(currentRadiusMeters) && currentRadiusMeters > 0f
                ? currentRadiusMeters
                : fallbackRadiusMeters;
    }
}
