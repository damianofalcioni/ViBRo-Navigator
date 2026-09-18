package vibro.navigator.nav.compass;

/** Shared camera scale for the compass's animated perspective viewport. */
public final class CompassPerspectiveScale {
    private static final float MAX_DEPTH = 0.25f;
    private static final float MIN_VERTICAL_SCALE = 0.58f;
    private static final float MAX_VIEWPORT_MULTIPLIER = (1f + MAX_DEPTH) / MIN_VERTICAL_SCALE;

    private CompassPerspectiveScale() {
    }

    public static float depth(float progress) {
        return MAX_DEPTH * clampProgress(progress);
    }

    public static float verticalScale(float progress) {
        return 1f - (1f - MIN_VERTICAL_SCALE) * clampProgress(progress);
    }

    public static float viewportMultiplier(float progress) {
        float bounded = clampProgress(progress);
        return 1f / (verticalScale(bounded) - depth(bounded) / MAX_VIEWPORT_MULTIPLIER);
    }

    public static float maximumViewportMultiplier() {
        return MAX_VIEWPORT_MULTIPLIER;
    }

    private static float clampProgress(float progress) {
        return Float.isFinite(progress) ? Math.max(0f, Math.min(1f, progress)) : 0f;
    }
}
