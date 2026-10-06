package vibro.navigator.nav.compass.ui;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.util.Collections;

import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.orientation.NavigationCompassGestureState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassTouchListenerTest {
    @Test
    public void pinchZoomsInAndOutWithinBoundsOnCompactAndFullscreenViews() {
        for (boolean fullscreen : new boolean[]{false, true}) {
            Fixture fixture = new Fixture(true, false, fullscreen);
            for (float radius : new float[]{200f, 100f, 100f}) {
                fixture.pinch(10f, 290f);
                assertEquals(radius, fixture.radius(), 0.01f);
            }
            for (float radius : new float[]{200f, 400f, 800f, 1600f, 1600f}) {
                fixture.pinch(130f, 170f);
                assertEquals(radius, fixture.radius(), 0.01f);
            }
            assertTrue(fixture.redraws > 0);
        }
    }

    @Test
    public void continuedPinchChangesOnlyOneLevelEvenWhenReversed() {
        Fixture fixture = new Fixture(true, true, false);
        fixture.begin();
        fixture.move(10f, 290f, 100f);
        assertEquals(200f, fixture.radius(), 0f);
        fixture.move(-100f, 400f, 100f);
        fixture.move(130f, 170f, 100f);
        assertEquals(200f, fixture.radius(), 0f);
        assertEquals(1, fixture.redraws);
        fixture.end();
        fixture.pinch(10f, 290f);
        assertEquals(100f, fixture.radius(), 0f);
    }

    @Test
    public void fullRouteIgnoresPinchIn2dAnd3d() {
        for (boolean perspective : new boolean[]{false, true}) {
            Fixture fixture = new Fixture(false, perspective, false);
            fixture.begin();
            fixture.move(10f, 290f, 100f);
            assertEquals(400f, fixture.radius(), 0f);
            assertEquals(0, fixture.redraws);
        }
    }

    @Test
    public void parallelVerticalSlideTilts3dWithoutZooming() {
        Fixture fixture = new Fixture(true, true, true);
        fixture.begin();
        fixture.move(100f, 200f, 200f);
        assertEquals(0.5f, fixture.gestures.perspectiveProgress(1f), 0.001f);
        assertEquals(400f, fixture.radius(), 0f);
        fixture.move(100f, 200f, 400f);
        assertEquals(0.25f, fixture.gestures.perspectiveProgress(1f), 0.001f);
        fixture.move(100f, 200f, 50f);
        assertEquals(1.25f, fixture.gestures.perspectiveProgress(1f), 0.001f);
    }

    @Test
    public void slidingUpFromDefaultIncreasesTiltInCompactAndFullscreenViews() {
        for (boolean fullscreen : new boolean[]{false, true}) {
            Fixture fixture = new Fixture(true, true, fullscreen);
            fixture.begin();
            fixture.move(100f, 200f, 50f);
            assertEquals(1.25f, fixture.gestures.perspectiveProgress(1f), 0.001f);
            fixture.move(100f, 200f, 100f);
            assertEquals(1f, fixture.gestures.perspectiveProgress(1f), 0.001f);
            fixture.move(100f, 200f, 150f);
            assertEquals(0.75f, fixture.gestures.perspectiveProgress(1f), 0.001f);
            assertEquals(400f, fixture.radius(), 0f);
        }
    }

    @Test
    public void parallelSlideAndSmallJitterDoNothingIn2d() {
        Fixture fixture = new Fixture(true, false, false);
        fixture.begin();
        fixture.move(99f, 201f, 101f);
        fixture.move(100f, 200f, 200f);
        assertEquals(400f, fixture.radius(), 0f);
        assertEquals(0, fixture.redraws);
    }

    @Test
    public void liftingOneFingerCannotClickOrContinueZoomAndNextDoubleTapWorks() {
        Fixture fixture = new Fixture(true, false, false);
        fixture.begin();
        fixture.move(80f, 220f, 100f);
        fixture.send(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new float[]{80f, 220f}, 100f);
        fixture.send(MotionEvent.ACTION_MOVE, new float[]{0f}, 100f);
        fixture.send(MotionEvent.ACTION_UP, new float[]{0f}, 100f);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(0, fixture.clicks);
        assertEquals(200f, fixture.radius(), 0f);
        fixture.tap(100f);
        assertEquals(0, fixture.clicks);
        fixture.tap(100f);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(1, fixture.clicks);
    }

    @Test
    public void thirdFingerAndCancellationStopTracking() {
        Fixture fixture = new Fixture(true, true, false);
        fixture.begin();
        fixture.send(MotionEvent.ACTION_POINTER_DOWN | (2 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new float[]{100f, 200f, 250f}, 100f);
        fixture.move(10f, 290f, 100f);
        fixture.send(MotionEvent.ACTION_CANCEL, new float[]{100f, 200f}, 100f);
        fixture.move(10f, 290f, 200f);
        assertEquals(400f, fixture.radius(), 0f);
        assertEquals(0, fixture.redraws);
    }

    @Test
    public void accessibilityClickStillCyclesTheView() {
        Fixture fixture = new Fixture(true, true, false);
        assertTrue(fixture.view.performClick());
        assertEquals(1, fixture.clicks);
        assertEquals(400f, fixture.radius(), 0f);
    }

    static final class Fixture {
        final NavigationCompassGestureState gestures = new NavigationCompassGestureState();
        final NavCompassState source;
        final boolean perspective;
        final NavigationCompassView view;
        int clicks;
        int redraws;
        long eventTime;

        Fixture(boolean moving, boolean perspective, boolean fullscreen) {
            this.perspective = perspective;
            source = NavCompassState.fromProjectedPoints(0f, null, 5f, 400f, 5f, moving, 10f,
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 300f, 0f, true);
            gestures.apply(source, perspective);
            Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
            view = new NavigationCompassView(activity);
            view.setFullscreenRouteModeEnabled(fullscreen);
            activity.setContentView(view);
            view.layout(0, 0, 300, 300);
            view.setOnClickListener(v -> clicks++);
            view.setOnTouchListener(new NavigationCompassTouchListener(view, gestures, () -> redraws++));
        }

        float radius() {
            return gestures.apply(source, perspective).radiusState.visibleRadiusMeters;
        }

        void begin() {
            send(MotionEvent.ACTION_DOWN, new float[]{100f}, 100f);
            send(MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new float[]{100f, 200f}, 100f);
        }

        void move(float firstX, float secondX, float y) {
            send(MotionEvent.ACTION_MOVE, new float[]{firstX, secondX}, y);
        }

        void pinch(float firstX, float secondX) {
            begin();
            move(firstX, secondX, 100f);
            end();
        }

        void end() {
            send(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new float[]{100f, 200f}, 100f);
            send(MotionEvent.ACTION_UP, new float[]{100f}, 100f);
        }

        void tap(float x) {
            send(MotionEvent.ACTION_DOWN, new float[]{x}, 100f);
            send(MotionEvent.ACTION_UP, new float[]{x}, 100f);
        }

        void send(int action, float[] xs, float y) {
            MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[xs.length];
            MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[xs.length];
            for (int i = 0; i < xs.length; i++) {
                properties[i] = new MotionEvent.PointerProperties();
                properties[i].id = i + 3;
                properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
                coords[i] = new MotionEvent.PointerCoords();
                coords[i].x = xs[i];
                coords[i].y = y;
            }
            eventTime += 20L;
            MotionEvent event = MotionEvent.obtain(0L, eventTime, action, xs.length,
                    properties, coords, 0, 0, 1f, 1f, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0);
            view.dispatchTouchEvent(event);
            event.recycle();
        }
    }
}
