package vibro.navigator.nav.compass;

/** Shared prepared viewport and forward distance references for both tilted projections. */
public final class CompassPerspectiveScale {
    private static final float DEFAULT_VERTICAL_SCALE = 0.58f;
    private static final float MAX_PROGRESS = 1.25f;
    // One prepared ground plane covers every inclination without rebuilding geometry per frame.
    private static final float MAX_VIEWPORT_MULTIPLIER = 1f / (1f - (1f - DEFAULT_VERTICAL_SCALE) * MAX_PROGRESS);
    // At maximum tilt, the far edge reaches 6 * 0.475 / 1.75 = 1.63 base radii.
    // This covers the compact top edge plus its 0.4-radius center offset and lateral geometry.
    private static final float CENTRAL_VIEWPORT_MULTIPLIER = 6f;
    // The nearest prepared edge retains positive camera depth (at least 0.25).
    private static final float MAX_DEPTH_FACTOR = 0.75f;

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

    public static float maximumViewportMultiplier(boolean centralPerspective) {
        return centralPerspective ? CENTRAL_VIEWPORT_MULTIPLIER : maximumViewportMultiplier();
    }

    public static float depthFactor(float progress) {
        return MAX_DEPTH_FACTOR * clampProgress(progress) / MAX_PROGRESS;
    }

    public static float viewportMultiplier(float progress, boolean centralPerspective) {
        if (!centralPerspective) {
            return viewportMultiplier(progress);
        }
        // Solve s * r / (1 + depth * r / preparedRadius) = baseRadius.
        return 1f / (verticalScale(progress) - depthFactor(progress) / CENTRAL_VIEWPORT_MULTIPLIER);
    }

    public static float maximumProgress() {
        return MAX_PROGRESS;
    }

    public static float clampProgress(float progress) {
        return Float.isFinite(progress) ? Math.max(0f, Math.min(MAX_PROGRESS, progress)) : 0f;
    }
}
