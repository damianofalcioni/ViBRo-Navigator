package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowSystemClock;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.nav.ui.NavigationGestureHintsRobolectricTest.TestNavigationActivity;
import vibro.navigator.settings.AppNavigationHintSettings;

@RunWith(RobolectricTestRunner.class)
public class NavigationGestureHintsTimeoutRobolectricTest {
    @Before
    public void setUp() {
        AppNavigationHintSettings.setEnabled(ApplicationProvider.getApplicationContext(), true);
    }

    @Test
    public void resumeHidesExpiredGuideBeforePendingTimerIsDelivered() {
        try (ActivityController<TestNavigationActivity> controller = builder().setup().visible()) {
            controller.pause().stop();
            ShadowSystemClock.advanceBy(Duration.ofSeconds(10));
            controller.start().resume();
            assertVisibility(controller.get(), View.GONE);
        }
    }

    @Test
    public void guideDismissesAtTenSeconds() {
        try (ActivityController<TestNavigationActivity> controller = builder().setup().visible()) {
            TextView caption = controller.get().findViewById(R.id.dismissGestureHints);
            assertEquals("Touch anywhere to dismiss or wait 10 seconds", caption.getText().toString());
            idle(1000);
            assertEquals("Touch anywhere to dismiss or wait 9 seconds", caption.getText().toString());
            idle(8000);
            assertVisibility(controller.get(), View.VISIBLE);
            assertEquals("Touch anywhere to dismiss or wait 1 second", caption.getText().toString());
            idle(1000);
            assertVisibility(controller.get(), View.GONE);
        }
    }

    @Test
    public void recreationKeepsTheOriginalDeadline() {
        try (ActivityController<TestNavigationActivity> controller = builder().setup().visible()) {
            idle(2000);
            controller.recreate();
            TextView caption = controller.get().findViewById(R.id.dismissGestureHints);
            assertEquals("Touch anywhere to dismiss or wait 8 seconds", caption.getText().toString());
            idle(7000);
            assertVisibility(controller.get(), View.VISIBLE);
            idle(1000);
            assertVisibility(controller.get(), View.GONE);
            controller.recreate();
            assertVisibility(controller.get(), View.GONE);
        }
    }

    @Test
    public void restoredGuideStaysHiddenIfItsDeadlineHasPassed() {
        Bundle state = new Bundle();
        try (ActivityController<TestNavigationActivity> controller = builder().setup().visible()) {
            idle(2000);
            controller.saveInstanceState(state);
        }
        idle(8000);
        try (ActivityController<TestNavigationActivity> restored = builder().setup(state).visible()) {
            assertVisibility(restored.get(), View.GONE);
        }
    }

    @Test
    public void touchDismissalStaysHiddenAndNewScreenGetsAFreshTimeout() {
        try (ActivityController<TestNavigationActivity> controller = builder().setup().visible()) {
            idle(2000);
            controller.get().findViewById(R.id.dismissGestureHints).performClick();
            idle(8000);
            assertVisibility(controller.get(), View.GONE);
        }
        try (ActivityController<TestNavigationActivity> fresh = builder().setup().visible()) {
            idle(9000);
            assertVisibility(fresh.get(), View.VISIBLE);
            idle(1000);
            assertVisibility(fresh.get(), View.GONE);
        }
    }

    private static void idle(long milliseconds) {
        shadowOf(Looper.getMainLooper()).idleFor(milliseconds, TimeUnit.MILLISECONDS);
    }

    private static void assertVisibility(TestNavigationActivity activity, int visibility) {
        assertEquals(visibility, activity.findViewById(R.id.navigationGestureHintsOverlay).getVisibility());
    }

    private static ActivityController<TestNavigationActivity> builder() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TestNavigationActivity.class);
        intent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        return Robolectric.buildActivity(TestNavigationActivity.class, intent);
    }
}
