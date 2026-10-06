package vibro.navigator.nav.orientation;

/** Recognizes pairs of short, stationary taps; a gesture cancels the whole pair. */
public final class NavigationCompassTapSequence {
    private final float touchSlop;
    private final float doubleTapSlop;
    private final long doubleTapTimeout;
    private final long longPressTimeout;
    private boolean active;
    private boolean pending;
    private boolean secondTap;
    private float downX;
    private float downY;
    private long downTime;
    private float previousX;
    private float previousY;
    private long previousUpTime;

    public NavigationCompassTapSequence(float touchSlop, float doubleTapSlop,
                                       long doubleTapTimeout, long longPressTimeout) {
        this.touchSlop = touchSlop;
        this.doubleTapSlop = doubleTapSlop;
        this.doubleTapTimeout = doubleTapTimeout;
        this.longPressTimeout = longPressTimeout;
    }

    public void down(float x, float y, long time) {
        secondTap = pending && time >= previousUpTime && time - previousUpTime <= doubleTapTimeout
                && within(x, y, previousX, previousY, doubleTapSlop);
        pending = false;
        active = true;
        downX = x;
        downY = y;
        downTime = time;
    }

    public void move(float x, float y) {
        if (!within(x, y, downX, downY, touchSlop)) {
            cancel();
        }
    }

    public boolean up(float x, float y, long time) {
        move(x, y);
        if (!active || time < downTime || time - downTime >= longPressTimeout) {
            cancel();
            return false;
        }
        active = false;
        if (secondTap) {
            secondTap = false;
            return true;
        }
        pending = true;
        previousX = downX;
        previousY = downY;
        previousUpTime = time;
        return false;
    }

    public void cancel() {
        active = false;
        pending = false;
        secondTap = false;
    }

    public boolean isActive() {
        return active;
    }

    private static boolean within(float x, float y, float originX, float originY, float slop) {
        return Math.hypot(x - originX, y - originY) <= slop;
    }
}
