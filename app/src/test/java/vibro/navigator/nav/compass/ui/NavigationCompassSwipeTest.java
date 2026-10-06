package vibro.navigator.nav.compass.ui;

import android.app.Activity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassSwipeTest {
    @Test
    public void bothDirectionsWorkAcrossAllCompassModesInPortrait() {
        assertBothDirections();
    }

    @Test
    @Config(qualifiers = "land")
    public void bothDirectionsWorkAcrossAllCompassModesInLandscape() {
        assertBothDirections();
    }

    @Test
    @Config(qualifiers = "xxxhdpi")
    public void swipeThresholdScalesWithScreenDensity() {
        var fixture = fixture();
        swipe(fixture, 200f, 100f);
        assertEquals(0, fixture.swipes);
        swipe(fixture, 250f, 50f);
        assertEquals(1, fixture.swipes);
        assertEquals(1, fixture.lastSwipeStep);
    }

    @Test
    public void swipeClearsTapPairAndFollowingDoubleTapStillWorks() {
        var fixture = fixture();
        fixture.tap(200f);
        swipe(fixture, 200f, 100f);
        fixture.tap(100f);
        assertEquals(0, fixture.clicks);
        fixture.tap(100f);
        assertEquals(1, fixture.clicks);
        assertEquals(1, fixture.swipes);
    }

    @Test
    public void cancelledVerticalAndShortDragsDoNotSwitch() {
        var fixture = fixture();
        fixture.send(MotionEvent.ACTION_DOWN, new float[]{100f}, 100f);
        fixture.send(MotionEvent.ACTION_MOVE, new float[]{200f}, 100f);
        fixture.send(MotionEvent.ACTION_CANCEL, new float[]{200f}, 100f);
        fixture.send(MotionEvent.ACTION_UP, new float[]{200f}, 100f);
        swipe(fixture, 100f, 130f);
        fixture.send(MotionEvent.ACTION_DOWN, new float[]{100f}, 100f);
        fixture.send(MotionEvent.ACTION_MOVE, new float[]{100f}, 250f);
        fixture.send(MotionEvent.ACTION_UP, new float[]{250f}, 100f);
        assertEquals(0, fixture.swipes);
        assertEquals(0, fixture.clicks);
    }

    @Test
    public void secondFingerCancelsSwipeEvenAfterHorizontalTravel() {
        var fixture = fixture();
        fixture.send(MotionEvent.ACTION_DOWN, new float[]{100f}, 100f);
        fixture.send(MotionEvent.ACTION_MOVE, new float[]{200f}, 100f);
        fixture.send(MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new float[]{200f, 250f}, 100f);
        fixture.send(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new float[]{200f, 250f}, 100f);
        fixture.send(MotionEvent.ACTION_UP, new float[]{250f}, 100f);
        assertEquals(0, fixture.swipes);
        assertEquals(0, fixture.clicks);
        swipe(fixture, 100f, 200f);
        assertEquals(1, fixture.swipes);
    }

    @Test
    public void detachingCompassCancelsUnfinishedSwipe() {
        var fixture = fixture();
        fixture.send(MotionEvent.ACTION_DOWN, new float[]{100f}, 100f);
        fixture.send(MotionEvent.ACTION_MOVE, new float[]{200f}, 100f);
        ((Activity) fixture.view.getContext()).setContentView(new FrameLayout(fixture.view.getContext()));
        fixture.send(MotionEvent.ACTION_UP, new float[]{200f}, 100f);
        assertEquals(0, fixture.swipes);
    }

    @Test
    public void swipingForegroundControlDoesNotReachCompass() {
        var fixture = fixture();
        Activity activity = (Activity) fixture.view.getContext();
        FrameLayout root = new FrameLayout(activity);
        activity.setContentView(root);
        root.addView(fixture.view);
        root.addView(new Button(activity));
        root.layout(0, 0, 300, 300);
        fixture.view.layout(0, 0, 300, 300);
        root.getChildAt(1).layout(0, 0, 300, 60);
        touch(root, MotionEvent.ACTION_DOWN, 200f, 100L);
        touch(root, MotionEvent.ACTION_MOVE, 100f, 120L);
        touch(root, MotionEvent.ACTION_UP, 100f, 140L);
        assertEquals(0, fixture.swipes);
        assertEquals(0, fixture.clicks);
    }

    private static void assertBothDirections() {
        for (boolean fullscreen : new boolean[]{false, true}) {
            for (boolean moving : new boolean[]{false, true}) {
                for (boolean perspective : new boolean[]{false, true}) {
                    var fixture = new NavigationCompassTouchListenerTest.Fixture(moving, perspective, fullscreen);
                    swipe(fixture, 200f, 100f);
                    assertEquals(1, fixture.swipes);
                    assertEquals(1, fixture.lastSwipeStep);
                    swipe(fixture, 100f, 200f);
                    assertEquals(2, fixture.swipes);
                    assertEquals(-1, fixture.lastSwipeStep);
                    assertEquals(0, fixture.clicks);
                    assertEquals(0, fixture.redraws);
                    assertFalse(fixture.view.isPressed());
                }
            }
        }
    }

    private static NavigationCompassTouchListenerTest.Fixture fixture() {
        return new NavigationCompassTouchListenerTest.Fixture(true, true, true);
    }

    private static void swipe(NavigationCompassTouchListenerTest.Fixture fixture, float startX, float endX) {
        int previousSwipes = fixture.swipes;
        fixture.send(MotionEvent.ACTION_DOWN, new float[]{startX}, 100f);
        fixture.send(MotionEvent.ACTION_MOVE, new float[]{endX}, 100f);
        assertFalse(fixture.view.isPressed());
        assertEquals(previousSwipes, fixture.swipes);
        fixture.send(MotionEvent.ACTION_UP, new float[]{endX}, 100f);
    }

    private static void touch(FrameLayout root, int action, float x, long time) {
        MotionEvent event = MotionEvent.obtain(100L, time, action, x, 25f, 0);
        root.dispatchTouchEvent(event);
        event.recycle();
    }
}
