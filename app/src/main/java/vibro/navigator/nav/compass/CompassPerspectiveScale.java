package vibro.navigator.nav.compass;

/** Shared viewport scale for the compass's animated orthographic tilt. */
public final class CompassPerspectiveScale {
    private static final float DEFAULT_VERTICAL_SCALE = 0.58f;
    private static final float MAX_PROGRESS = 1.25f;
    // One prepared ground plane covers every inclination without rebuilding geometry per frame.
    private static final float MAX_VIEWPORT_MULTIPLIER = 1f / (1f - (1f - DEFAULT_VERTICAL_SCALE) * MAX_PROGRESS);

    private CompassPerspectiveScale() {
    }

    public static float verticalScale(float progress) {
        return 1f - (1f - DEFAULT_VERTICAL_SCALE) * clampProgress(progress);
    }

    public static float viewportMultiplier(float progress) {
        return 1f / verticalScale(progress);
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
