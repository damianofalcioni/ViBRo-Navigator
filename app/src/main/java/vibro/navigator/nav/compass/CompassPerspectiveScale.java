package vibro.navigator.nav.compass;

/** Shared camera scale for the compass's animated perspective viewport. */
public final class CompassPerspectiveScale {
    private static final float DEFAULT_DEPTH = 0.25f;
    private static final float DEFAULT_VERTICAL_SCALE = 0.58f;
    private static final float DEFAULT_VIEWPORT_MULTIPLIER = (1f + DEFAULT_DEPTH) / DEFAULT_VERTICAL_SCALE;
    private static final float MAX_PROGRESS = 1.25f;
    // Prepare the horizon for the strongest tilt, keeping the original projection at progress 1.
    private static final float MAX_VIEWPORT_MULTIPLIER = 1f / (
            1f - (1f - DEFAULT_VERTICAL_SCALE) * MAX_PROGRESS
                    - DEFAULT_DEPTH * MAX_PROGRESS / DEFAULT_VIEWPORT_MULTIPLIER);

    private CompassPerspectiveScale() {
    }

    public static float depth(float progress) {
        // Compensate for the larger source plane so depth per ground meter stays unchanged.
        return DEFAULT_DEPTH * clampProgress(progress) * MAX_VIEWPORT_MULTIPLIER / DEFAULT_VIEWPORT_MULTIPLIER;
    }

    public static float verticalScale(float progress) {
        return 1f - (1f - DEFAULT_VERTICAL_SCALE) * clampProgress(progress);
    }

    public static float viewportMultiplier(float progress) {
        float bounded = clampProgress(progress);
        return 1f / (verticalScale(bounded) - depth(bounded) / MAX_VIEWPORT_MULTIPLIER);
    }

    public static float maximumViewportMultiplier() {
        return MAX_VIEWPORT_MULTIPLIER;
    }

    public static float maximumProgress() {
        return MAX_PROGRESS;
    }

    public static float clampProgress(float progress) {
        return Float.isFinite(progress) ? Math.max(0f, Math.min(MAX_PROGRESS, progress)) : 0f;
    }
}
