package vibro.navigator.nav.compass.ui;

import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.app.Activity;
import android.os.Looper;
import android.widget.Button;
import android.widget.FrameLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassDoubleTapTest {
    @Test
    public void onlyDoubleTapSwitchesCompactAndFullscreenInEveryMode() {
        for (boolean fullscreen : new boolean[]{false, true}) {
            for (boolean moving : new boolean[]{false, true}) {
                for (boolean perspective : new boolean[]{false, true}) {
                    NavigationCompassTouchListenerTest.Fixture fixture = fixture(moving, perspective, fullscreen);
                    fixture.tap(100f);
                    assertEquals(0, fixture.clicks);
                    fixture.send(MotionEvent.ACTION_DOWN, new float[]{100f}, 100f);
                    assertEquals(0, fixture.clicks);
                    assertTrue(fixture.view.isPressed());
                    fixture.send(MotionEvent.ACTION_UP, new float[]{100f}, 100f);
                    assertEquals(1, fixture.clicks);
                    assertFalse(fixture.view.isPressed());
                }
            }
        }
    }

    @Test
    public void slowOrDistantTapsDoNotSwitch() {
        NavigationCompassTouchListenerTest.Fixture fixture = fixture(true, false, false);
        fixture.tap(50f);
        fixture.tap(250f);
        fixture.eventTime += ViewConfiguration.getDoubleTapTimeout();
        fixture.tap(250f);
        assertEquals(0, fixture.clicks);
        fixture.tap(250f);
        assertEquals(1, fixture.clicks);
    }

    @Test
    public void pinchAfterFirstTapClearsPairEvenWithoutZoom() {
        NavigationCompassTouchListenerTest.Fixture fixture = fixture(false, false, true);
        fixture.tap(100f);
        fixture.begin();
        fixture.end();
        fixture.tap(100f);
        assertEquals(0, fixture.clicks);
        fixture.tap(100f);
        assertEquals(1, fixture.clicks);
    }

    @Test
    public void secondTapBecomingPinchCannotSwitch() {
        NavigationCompassTouchListenerTest.Fixture fixture = fixture(true, true, true);
        fixture.tap(100f);
        fixture.begin();
        fixture.move(10f, 290f, 100f);
        fixture.end();
        assertEquals(0, fixture.clicks);
        assertEquals(200f, fixture.radius(), 0f);
    }

    @Test
    public void dragAndCancelClearPendingTap() {
        NavigationCompassTouchListenerTest.Fixture fixture = fixture(true, false, false);
        fixture.tap(100f);
        fixture.send(MotionEvent.ACTION_DOWN, new float[]{100f}, 100f);
        fixture.send(MotionEvent.ACTION_MOVE, new float[]{200f}, 100f);
        assertFalse(fixture.view.isPressed());
        fixture.send(MotionEvent.ACTION_UP, new float[]{100f}, 100f);
        fixture.tap(100f);
        assertEquals(0, fixture.clicks);
        fixture.send(MotionEvent.ACTION_CANCEL, new float[]{100f}, 100f);
        fixture.tap(100f);
        assertEquals(0, fixture.clicks);
    }

    @Test
    public void longPressAndOutOfBoundsReleaseCannotSwitch() {
        NavigationCompassTouchListenerTest.Fixture fixture = fixture(true, false, false);
        fixture.tap(100f);
        fixture.send(MotionEvent.ACTION_DOWN, new float[]{100f}, 100f);
        fixture.eventTime += ViewConfiguration.getLongPressTimeout();
        fixture.send(MotionEvent.ACTION_UP, new float[]{100f}, 100f);
        fixture.tap(100f);
        assertEquals(0, fixture.clicks);
        fixture.send(MotionEvent.ACTION_DOWN, new float[]{299f}, 100f);
        fixture.send(MotionEvent.ACTION_UP, new float[]{301f}, 100f);
        fixture.tap(299f);
        assertEquals(0, fixture.clicks);
    }

    @Test
    public void tappingForegroundControlDoesNotReachFullscreenCompass() {
        NavigationCompassTouchListenerTest.Fixture fixture = fixture(true, true, true);
        Activity activity = (Activity) fixture.view.getContext();
        FrameLayout root = new FrameLayout(activity);
        Button panel = new Button(activity);
        int[] panelClicks = {0};
        panel.setOnClickListener(v -> panelClicks[0]++);
        activity.setContentView(root);
        root.addView(fixture.view);
        root.addView(panel);
        root.layout(0, 0, 300, 300);
        fixture.view.layout(0, 0, 300, 300);
        panel.layout(0, 0, 150, 60);
        for (int i = 0; i < 2; i++) {
            touch(root, MotionEvent.ACTION_DOWN, 100L + i * 100L);
            touch(root, MotionEvent.ACTION_UP, 120L + i * 100L);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
        }
        assertEquals(2, panelClicks[0]);
        assertEquals(0, fixture.clicks);
        fixture.tap(100f);
        fixture.tap(100f);
        assertEquals(1, fixture.clicks);
    }

    private static void touch(FrameLayout root, int action, long time) {
        MotionEvent event = MotionEvent.obtain(time, time, action, 100f, 25f, 0);
        root.dispatchTouchEvent(event);
        event.recycle();
    }

    private static NavigationCompassTouchListenerTest.Fixture fixture(
            boolean moving, boolean perspective, boolean fullscreen) {
        return new NavigationCompassTouchListenerTest.Fixture(moving, perspective, fullscreen);
    }
}
