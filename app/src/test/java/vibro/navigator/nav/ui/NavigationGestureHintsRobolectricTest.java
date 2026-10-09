package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Switch;
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
    public void compactPortraitCentersAllHintsWithoutEmptySpace() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            assertCenteredPlacement(controller.get(), 400, 800);
        }
    }

    @Test
    @Config(qualifiers = "land")
    public void compactLandscapeCentersAllHintsWithoutEmptySpace() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            assertCenteredPlacement(controller.get(), 800, 400);
        }
    }

    @Test
    @Config(qualifiers = "ldrtl-land")
    public void compactLandscapeShowsAllHintsInRightToLeftLayouts() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            controller.get().findViewById(android.R.id.content).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            assertCenteredPlacement(controller.get(), 800, 400);
        }
    }

    @Test
    public void fullscreenPortraitCentersAllHintsWithoutEmptySpace() {
        assertFullscreenPlacement(400, 800);
    }

    @Test
    @Config(qualifiers = "land")
    public void fullscreenLandscapeCentersAllHintsWithoutEmptySpace() {
        assertFullscreenPlacement(800, 400);
    }

    @Test
    public void firstTouchOutsideHintsStillReachesCompass() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            TestNavigationActivity activity = controller.get();
            layout(activity, 400, 800);
            View compass = activity.findViewById(R.id.navigationCompassView);
            int[] touches = {0};
            compass.setOnTouchListener((view, event) -> {
                touches[0]++;
                return true;
            });

            touchAt(activity, compass, MotionEvent.ACTION_DOWN, 2, 2);

            assertEquals(View.GONE, overlay(activity).getVisibility());
            assertEquals(1, touches[0]);
            touchAt(activity, compass, MotionEvent.ACTION_UP, 2, 2);
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
            assertStreetColors(controller.get());
            AppThemeSettings.setLightThemeEnabled(controller.get(), true);
            controller.recreate();
            assertEquals(View.VISIBLE, overlay(controller.get()).getVisibility());
            TextView dismiss = controller.get().findViewById(R.id.dismissGestureHints);
            assertEquals(AndroidAppTheme.color(controller.get(), R.attr.vibroTextPrimaryColor),
                    dismiss.getCurrentTextColor());
            assertEquals(0.75f, dismiss.getAlpha(), 0.001f);
            assertStreetColors(controller.get());
            dismiss.performClick();
            AppThemeSettings.setLightThemeEnabled(controller.get(), false);
            controller.recreate();
            assertEquals(View.GONE, overlay(controller.get()).getVisibility());
        }
    }

    @Test
    public void dontShowAgainTouchDisablesSettingAndSurvivesNewScreens() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            TestNavigationActivity activity = controller.get();
            layout(activity, 400, 800);
            View action = activity.findViewById(R.id.dontShowGestureHintsAgain);
            touch(activity, action, MotionEvent.ACTION_DOWN);
            assertEquals(View.VISIBLE, overlay(activity).getVisibility());
            touch(activity, action, MotionEvent.ACTION_UP);
            shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS);
            assertEquals(View.GONE, overlay(activity).getVisibility());
            assertFalse(AppNavigationHintSettings.isEnabled(activity));
            controller.recreate();
            assertEquals(View.GONE, overlay(controller.get()).getVisibility());
        }
        try (ActivityController<TestNavigationActivity> fresh = activity()) {
            assertEquals(View.GONE, overlay(fresh.get()).getVisibility());
        }
        Intent settingsIntent = new Intent(ApplicationProvider.getApplicationContext(), AboutActivity.class);
        settingsIntent.putExtra(AboutActivity.EXTRA_SCROLL_TO_SETTINGS, true);
        try (ActivityController<AboutActivity> settings =
                     Robolectric.buildActivity(AboutActivity.class, settingsIntent).setup()) {
            Switch switchView = settings.get().findViewById(R.id.aboutShowHintPanelSwitch);
            assertFalse(switchView.isChecked());
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    public void smallPortraitShowsAllContentWithoutScrolling() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            TestNavigationActivity activity = controller.get();
            assertCenteredPlacement(activity, 320, 480);
        }
    }

    private static void assertStreetColors(TestNavigationActivity activity) {
        int[][] swatches = {
                {R.id.gestureHintHighwaySwatch, R.attr.vibroCompassStreetHighwayColor},
                {R.id.gestureHintNormalStreetSwatch, R.attr.vibroCompassStreetNormalColor},
                {R.id.gestureHintWalkingCyclingSwatch, R.attr.vibroCompassStreetWalkingCyclingColor},
                {R.id.gestureHintSpecialRoutingSwatch, R.attr.vibroCompassStreetSpecialRoutingColor}
        };
        for (int[] swatch : swatches) {
            TextView view = activity.findViewById(swatch[0]);
            assertEquals(AndroidAppTheme.color(activity, swatch[1]), view.getCurrentTextColor());
            assertEquals(1f, view.getAlpha(), 0.001f);
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

    private static void assertCenteredPlacement(TestNavigationActivity activity, int width, int height) {
        layout(activity, width, height);
        View content = activity.findViewById(android.R.id.content);
        View card = activity.findViewById(R.id.navigationGestureHints);
        assertEquals(View.VISIBLE, overlay(activity).getVisibility());
        assertEquals(content.getWidth(), overlay(activity).getWidth());
        assertEquals(content.getHeight(), overlay(activity).getHeight());
        assertTrue(card.getWidth() < content.getWidth());
        assertTrue(card.getHeight() < content.getHeight());
        int[] contentLocation = new int[2];
        int[] cardLocation = new int[2];
        content.getLocationInWindow(contentLocation);
        card.getLocationInWindow(cardLocation);
        assertEquals(contentLocation[0] + content.getWidth() / 2f,
                cardLocation[0] + card.getWidth() / 2f, 1f);
        assertEquals(contentLocation[1] + content.getHeight() / 2f,
                cardLocation[1] + card.getHeight() / 2f, 1f);
        View sections = activity.findViewById(R.id.navigationGestureHintSections);
        View dismiss = activity.findViewById(R.id.dismissGestureHints);
        ViewGroup.MarginLayoutParams dismissParams = (ViewGroup.MarginLayoutParams) dismiss.getLayoutParams();
        assertEquals(sections.getBottom() + dismissParams.topMargin, dismiss.getTop());
        assertFullyVisible(card, new Rect(cardLocation[0], cardLocation[1],
                cardLocation[0] + card.getWidth(), cardLocation[1] + card.getHeight()));
    }

    private static void assertFullscreenPlacement(int width, int height) {
        AppCompassSettings.setFullscreenRouteEnabled(ApplicationProvider.getApplicationContext(), true);
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            TestNavigationActivity activity = controller.get();
            assertCenteredPlacement(activity, width, height);
        }
    }

    private static void assertFullyVisible(View view, Rect page) {
        assertFalse(view instanceof ScrollView);
        int[] location = new int[2];
        view.getLocationInWindow(location);
        Rect bounds = new Rect(location[0], location[1],
                location[0] + view.getWidth(), location[1] + view.getHeight());
        assertTrue("Clipped hint view " + view.getId() + ": " + bounds + " outside " + page,
                page.contains(bounds));
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                assertFullyVisible(group.getChildAt(index), bounds);
            }
        }
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
        touchAt(activity, target, action, target.getWidth() / 2f, target.getHeight() / 2f);
    }

    private static void touchAt(TestNavigationActivity activity, View target, int action, float x, float y) {
        int[] location = new int[2];
        target.getLocationInWindow(location);
        MotionEvent event = MotionEvent.obtain(0, 0, action,
                location[0] + x, location[1] + y, 0);
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
