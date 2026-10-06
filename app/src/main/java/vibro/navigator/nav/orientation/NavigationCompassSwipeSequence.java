package vibro.navigator.nav.orientation;

/** Recognizes deliberate horizontal travel; vertical drags cancel the gesture. */
public final class NavigationCompassSwipeSequence {
    private final float minimumTravel;
    private boolean active;
    private float downX;
    private float downY;

    public NavigationCompassSwipeSequence(float minimumTravel) {
        this.minimumTravel = minimumTravel;
    }

    public void down(float x, float y) {
        downX = x;
        downY = y;
        active = true;
    }

    public boolean move(float x, float y) {
        float horizontalTravel = Math.abs(x - downX);
        float verticalTravel = Math.abs(y - downY);
        if (verticalTravel >= minimumTravel && verticalTravel >= horizontalTravel) {
            cancel();
        }
        // A 2:1 ratio keeps diagonal scrolling from changing the view.
        return active && horizontalTravel >= minimumTravel && horizontalTravel >= verticalTravel * 2f;
    }

    /** Left advances the view cycle, right reverses it; zero means no swipe. */
    public int up(float x, float y) {
        boolean swipe = move(x, y);
        cancel();
        return swipe ? (x < downX ? 1 : -1) : 0;
    }

    public void cancel() {
        active = false;
    }
}
