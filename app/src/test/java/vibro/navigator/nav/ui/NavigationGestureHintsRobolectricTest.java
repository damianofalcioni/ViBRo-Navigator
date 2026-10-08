package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.about.AboutActivity;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.settings.AppCompassSettings;
import vibro.navigator.settings.AppNavigationHintSettings;
import vibro.navigator.settings.AppThemeSettings;

@RunWith(RobolectricTestRunner.class)
public class NavigationGestureHintsRobolectricTest {
    @Before
    public void setUp() {
        AppNavigationHintSettings.setEnabled(ApplicationProvider.getApplicationContext(), true);
        AppThemeSettings.setLightThemeEnabled(ApplicationProvider.getApplicationContext(), false);
        AppCompassSettings.setFullscreenRouteEnabled(ApplicationProvider.getApplicationContext(), false);
    }

    @Test
    public void compactPortraitCentersHintsOverCompass() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            assertCompactPlacement(controller.get(), 400, 800);
        }
    }

    @Test
    @Config(qualifiers = "land")
    public void compactLandscapeCentersHintsOverCompass() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            assertCompactPlacement(controller.get(), 800, 400);
        }
    }

    @Test
    @Config(qualifiers = "ldrtl-land")
    public void compactLandscapeCentersHintsInRightToLeftLayouts() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            controller.get().findViewById(android.R.id.content).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            assertCompactPlacement(controller.get(), 800, 400);
        }
    }

    @Test
    public void fullscreenPortraitCentersHintsInScreen() {
        assertFullscreenPlacement(400, 800);
    }

    @Test
    @Config(qualifiers = "land")
    public void fullscreenLandscapeCentersHintsInScreen() {
        assertFullscreenPlacement(800, 400);
    }

    @Test
    public void firstTouchOnHintsStillReachesCompass() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            TestNavigationActivity activity = controller.get();
            layout(activity, 400, 800);
            View compass = activity.findViewById(R.id.navigationCompassView);
            int[] touches = {0};
            compass.setOnTouchListener((view, event) -> {
                touches[0]++;
                return true;
            });

            touch(activity, compass, MotionEvent.ACTION_DOWN);

            assertEquals(View.GONE, overlay(activity).getVisibility());
            assertEquals(1, touches[0]);
            touch(activity, compass, MotionEvent.ACTION_UP);
            assertEquals(2, touches[0]);
        }
    }

    @Test
    public void firstTouchOutsideHintsStillOpensSettings() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            TestNavigationActivity activity = controller.get();
            layout(activity, 400, 800);
            View settings = activity.findViewById(R.id.navigationSettingsButton);

            touch(activity, settings, MotionEvent.ACTION_DOWN);
            touch(activity, settings, MotionEvent.ACTION_UP);
            shadowOf(Looper.getMainLooper()).idleFor(150, TimeUnit.MILLISECONDS);

            assertEquals(View.GONE, overlay(activity).getVisibility());
            assertTrue(shadowOf(activity).getNextStartedActivity()
                    .getBooleanExtra(AboutActivity.EXTRA_SCROLL_TO_SETTINGS, false));
        }
    }

    @Test
    public void dismissalSurvivesRecreationAndBackgroundResume() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            controller.get().findViewById(R.id.dismissGestureHints).performClick();
            controller.pause().stop().start().resume();
            assertEquals(View.GONE, overlay(controller.get()).getVisibility());
            controller.recreate();
            assertEquals(View.GONE, overlay(controller.get()).getVisibility());
        }
        try (ActivityController<TestNavigationActivity> fresh = activity()) {
            assertEquals(View.VISIBLE, overlay(fresh.get()).getVisibility());
        }
    }

    @Test
    public void themeRecreationPreservesVisibilityAndRefreshesColors() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            AppThemeSettings.setLightThemeEnabled(controller.get(), true);
            controller.recreate();
            assertEquals(View.VISIBLE, overlay(controller.get()).getVisibility());
            TextView dismiss = controller.get().findViewById(R.id.dismissGestureHints);
            assertEquals(AndroidAppTheme.color(controller.get(), R.attr.vibroTextPrimaryColor),
                    dismiss.getCurrentTextColor());
            assertEquals(0.75f, dismiss.getAlpha(), 0.001f);
            dismiss.performClick();
            AppThemeSettings.setLightThemeEnabled(controller.get(), false);
            controller.recreate();
            assertEquals(View.GONE, overlay(controller.get()).getVisibility());
        }
    }

    @Test
    public void undismissedHintsSurviveSavedState() {
        Bundle state = new Bundle();
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            controller.saveInstanceState(state);
        }
        try (ActivityController<TestNavigationActivity> restored = builder().setup(state).visible()) {
            assertEquals(View.VISIBLE, overlay(restored.get()).getVisibility());
        }
    }

    private static void assertCompactPlacement(TestNavigationActivity activity, int width, int height) {
        layout(activity, width, height);
        View compass = activity.findViewById(R.id.navigationCompassView);
        assertEquals(View.VISIBLE, overlay(activity).getVisibility());
        assertSameCenter(compass, activity.findViewById(R.id.navigationGestureHints));
        assertEquals(compass.getWidth(), overlay(activity).getWidth());
        assertEquals(compass.getHeight(), overlay(activity).getHeight());
    }

    private static void assertFullscreenPlacement(int width, int height) {
        AppCompassSettings.setFullscreenRouteEnabled(ApplicationProvider.getApplicationContext(), true);
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            TestNavigationActivity activity = controller.get();
            layout(activity, width, height);
            assertSameCenter(activity.findViewById(android.R.id.content),
                    activity.findViewById(R.id.navigationGestureHints));
        }
    }

    private static void assertSameCenter(View anchor, View card) {
        int[] anchorLocation = new int[2];
        int[] cardLocation = new int[2];
        anchor.getLocationInWindow(anchorLocation);
        card.getLocationInWindow(cardLocation);
        assertEquals(anchorLocation[0] + anchor.getWidth() / 2f,
                cardLocation[0] + card.getWidth() / 2f, 1f);
        assertEquals(anchorLocation[1] + anchor.getHeight() / 2f,
                cardLocation[1] + card.getHeight() / 2f, 1f);
        assertTrue(card.getWidth() > 0);
        assertTrue(card.getHeight() > 0);
    }

    private static void layout(TestNavigationActivity activity, int width, int height) {
        View decor = activity.getWindow().getDecorView();
        for (int pass = 0; pass < 2; pass++) {
            decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, width, height);
            decor.getViewTreeObserver().dispatchOnGlobalLayout();
        }
    }

    private static void touch(TestNavigationActivity activity, View target, int action) {
        int[] location = new int[2];
        target.getLocationInWindow(location);
        MotionEvent event = MotionEvent.obtain(0, 0, action,
                location[0] + target.getWidth() / 2f,
                location[1] + target.getHeight() / 2f, 0);
        activity.dispatchTouchEvent(event);
        event.recycle();
    }

    private static FrameLayout overlay(TestNavigationActivity activity) {
        return activity.findViewById(R.id.navigationGestureHintsOverlay);
    }

    private static ActivityController<TestNavigationActivity> activity() {
        return builder().setup().visible();
    }

    private static ActivityController<TestNavigationActivity> builder() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TestNavigationActivity.class);
        intent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        return Robolectric.buildActivity(TestNavigationActivity.class, intent);
    }

    public static class TestNavigationActivity extends NavigationActivity {
        @Override
        public boolean bindService(Intent service, ServiceConnection connection, int flags) {
            return false;
        }
    }
}
