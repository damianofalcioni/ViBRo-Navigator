package vibro.navigator.nav.orientation;

/** Short reversible tilt animation, driven by the existing compass frame ticker. */
final class NavigationCompassPerspectiveTransition {
    private static final long DURATION_MS = 320L;

    private boolean active;
    private float progress;
    private float startProgress;
    private float targetProgress;
    private long startElapsedMs;

    void start(boolean perspective, long nowElapsedMs) {
        advance(nowElapsedMs);
        float target = perspective ? 1f : 0f;
        if (progress == target) {
            active = false;
            return;
        }
        startProgress = progress;
        targetProgress = target;
        startElapsedMs = nowElapsedMs;
        active = true;
    }

    void advance(long nowElapsedMs) {
        if (!active) {
            return;
        }
        float elapsedRatio = Math.max(0f, Math.min(1f, (nowElapsedMs - startElapsedMs) / (float) DURATION_MS));
        float easedRatio = elapsedRatio * (2f - elapsedRatio);
        progress = startProgress + (targetProgress - startProgress) * easedRatio;
        if (elapsedRatio >= 1f) {
            progress = targetProgress;
            active = false;
        }
    }

    boolean isActive() {
        return active;
    }

    float progress() {
        return progress;
    }

    void reset() {
        active = false;
        progress = 0f;
    }
}
