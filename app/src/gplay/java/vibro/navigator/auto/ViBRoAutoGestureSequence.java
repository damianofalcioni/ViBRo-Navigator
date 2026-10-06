package vibro.navigator.auto;

import vibro.navigator.nav.orientation.NavigationCompassSwipeSequence;

/** Adapts host deltas, which have neither pointer counts nor gesture-end callbacks. */
final class ViBRoAutoGestureSequence {
    private enum Gesture { UNDECIDED, SWIPE, TILT, SCALE }

    private static final float ZOOM_STEP_RATIO = 1.25f;
    private final NavigationCompassSwipeSequence swipes;
    private final float touchSlop;
    private final long idleTimeoutMs;
    private long lastInputMs = -1L;
    private Gesture gesture = Gesture.UNDECIDED;
    private float distanceX;
    private float distanceY;
    private float tiltDistance;
    private double scale = 1.0;
    private boolean scaleConsumed;

    ViBRoAutoGestureSequence(float minimumSwipe, float touchSlop, long idleTimeoutMs) {
        swipes = new NavigationCompassSwipeSequence(minimumSwipe);
        this.touchSlop = touchSlop;
        // The host reports streams of deltas; a quiet double-tap interval ends a burst.
        this.idleTimeoutMs = idleTimeoutMs;
    }

    int scroll(float dx, float dy, long nowMs) {
        tiltDistance = 0f;
        if (!Float.isFinite(dx) || !Float.isFinite(dy)) {
            return 0;
        }
        beginInput(nowMs);
        if (gesture == Gesture.SCALE || gesture == Gesture.SWIPE) {
            return 0;
        }
        if (gesture == Gesture.TILT) {
            tiltDistance = dy;
            return 0;
        }
        return recognizeScroll(dx, dy);
    }

    private int recognizeScroll(float dx, float dy) {
        distanceX += dx;
        distanceY += dy;
        if (isVerticalTravel()) {
            gesture = Gesture.TILT;
            tiltDistance = distanceY;
            swipes.cancel();
            return 0;
        }
        // Host scroll distances have the opposite sign to finger travel.
        if (!swipes.move(-distanceX, -distanceY)) {
            return 0;
        }
        gesture = Gesture.SWIPE;
        return swipes.up(-distanceX, -distanceY);
    }

    float tiltDistance() {
        return tiltDistance;
    }

    private boolean isVerticalTravel() {
        return Math.abs(distanceY) > touchSlop && Math.abs(distanceY) > Math.abs(distanceX);
    }

    int scale(float factor, long nowMs) {
        if (!Float.isFinite(factor) || factor <= 0f) {
            return 0;
        }
        beginInput(nowMs);
        gesture = Gesture.SCALE;
        swipes.cancel();
        if (scaleConsumed) {
            return 0;
        }
        scale *= factor;
        int step = scale >= ZOOM_STEP_RATIO ? 1 : (scale <= 1.0 / ZOOM_STEP_RATIO ? -1 : 0);
        scaleConsumed = step != 0;
        return step;
    }

    boolean isRecent(long nowMs) {
        return lastInputMs >= 0L && nowMs >= lastInputMs && nowMs - lastInputMs <= idleTimeoutMs;
    }

    void clear() {
        lastInputMs = -1L;
        gesture = Gesture.UNDECIDED;
        distanceX = 0f;
        distanceY = 0f;
        tiltDistance = 0f;
        scale = 1.0;
        scaleConsumed = false;
        swipes.cancel();
    }

    private void beginInput(long nowMs) {
        if (!isRecent(nowMs)) {
            clear();
            swipes.down(0f, 0f);
        }
        lastInputMs = nowMs;
    }
}
