package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import vibro.navigator.R;
import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.nav.ui.NavigationGestureHintsRobolectricTest.TestNavigationActivity;
import vibro.navigator.nav.startup.NavigationPreflight;
import vibro.navigator.settings.AppNavigationHintSettings;

@RunWith(RobolectricTestRunner.class)
public class NavigationGestureHintsPreferenceRobolectricTest {
    @Before
    public void setUp() {
        AppNavigationHintSettings.setEnabled(ApplicationProvider.getApplicationContext(), true);
    }

    @Test
    public void disabledPanelStaysHiddenAcrossFreshScreensAndRecreation() {
        AppNavigationHintSettings.setEnabled(ApplicationProvider.getApplicationContext(), false);
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            assertVisibility(controller.get(), View.GONE);
            controller.recreate();
            assertVisibility(controller.get(), View.GONE);
        }
        try (ActivityController<TestNavigationActivity> fresh = activity()) {
            assertVisibility(fresh.get(), View.GONE);
        }
        AppNavigationHintSettings.setEnabled(ApplicationProvider.getApplicationContext(), true);
        try (ActivityController<TestNavigationActivity> reenabled = activity()) {
            assertVisibility(reenabled.get(), View.VISIBLE);
        }
    }

    @Test
    public void disablingWhileAwayHidesPanelOnResumeAndReenablingWaitsForNextStart() {
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            assertVisibility(controller.get(), View.VISIBLE);
            controller.pause().stop();
            AppNavigationHintSettings.setEnabled(controller.get(), false);
            controller.start().resume();
            assertVisibility(controller.get(), View.GONE);
            controller.pause().stop();
            AppNavigationHintSettings.setEnabled(controller.get(), true);
            controller.start().resume();
            assertVisibility(controller.get(), View.GONE);
        }
        try (ActivityController<TestNavigationActivity> fresh = activity()) {
            assertVisibility(fresh.get(), View.VISIBLE);
        }
    }

    @Test
    public void newNavigationRequestHonorsTheSavedPreference() {
        shadowOf((Application) ApplicationProvider.getApplicationContext()).grantPermissions(
                NavigationPreflight.PERMISSION_FINE_LOCATION,
                NavigationPreflight.PERMISSION_COARSE_LOCATION,
                NavigationPreflight.PERMISSION_POST_NOTIFICATIONS);
        try (ActivityController<TestNavigationActivity> controller = activity()) {
            controller.get().findViewById(R.id.dismissGestureHints).performClick();
            AppNavigationHintSettings.setEnabled(controller.get(), false);
            Intent request = new Intent(controller.get(), TestNavigationActivity.class);
            request.putExtra(AndroidNavigationRequestIntentContract.EXTRA_PROFILE, "test-profile");
            request.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LAT, 48.2082d);
            request.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LON, 16.3738d);
            controller.newIntent(request);
            assertVisibility(controller.get(), View.GONE);
            AppNavigationHintSettings.setEnabled(controller.get(), true);
            controller.newIntent(request);
            assertVisibility(controller.get(), View.VISIBLE);
        }
    }

    private static void assertVisibility(TestNavigationActivity activity, int visibility) {
        assertEquals(visibility, activity.findViewById(R.id.navigationGestureHintsOverlay).getVisibility());
    }

    private static ActivityController<TestNavigationActivity> activity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TestNavigationActivity.class);
        intent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        return Robolectric.buildActivity(TestNavigationActivity.class, intent).setup().visible();
    }
}
