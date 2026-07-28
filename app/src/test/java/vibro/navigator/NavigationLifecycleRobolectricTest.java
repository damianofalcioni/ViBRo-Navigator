package vibro.navigator;

import vibro.navigator.main.MainActivity;
import vibro.navigator.nav.ui.NavigationActivity;


import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.nav.model.NavigationRoutingMode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.ServiceConnection;
import android.view.WindowManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.core.app.ServiceCompat;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowService;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class NavigationLifecycleRobolectricTest {
    private static final String TEST_PROFILE = "test-profile";

    @Test
    public void backPressMovesNavigationActivityTaskToBackground() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TestNavigationActivity.class);
        intent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);

        ActivityController<TestNavigationActivity> controller =
                Robolectric.buildActivity(TestNavigationActivity.class, intent).setup();
        TestNavigationActivity activity = controller.get();

        activity.onBackPressed();

        assertTrue(activity.moveTaskToBackCalled);
        assertFalse(activity.isFinishing());
    }

    @Test
    public void navigationActivityShowsOverLockScreenOnModernAndroid() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TestNavigationActivity.class);
        intent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);

        ActivityController<TestNavigationActivity> controller =
                Robolectric.buildActivity(TestNavigationActivity.class, intent).setup();
        TestNavigationActivity activity = controller.get();

        assertTrue(shadowOf(activity).getShowWhenLocked());
        assertTrue(shadowOf(activity).getTurnScreenOn());
    }

    @Test
    @Config(sdk = 26)
    @SuppressWarnings("deprecation")
    public void navigationActivityShowsOverLockScreenOnLegacyAndroid() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TestNavigationActivity.class);
        intent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);

        ActivityController<TestNavigationActivity> controller =
                Robolectric.buildActivity(TestNavigationActivity.class, intent).setup();
        TestNavigationActivity activity = controller.get();

        assertTrue(shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED));
        assertTrue(shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON));
    }

    @Test
    public void ensureForegroundNotificationRestoresForegroundState() {
        Intent startIntent = new Intent(ApplicationProvider.getApplicationContext(), NavigationService.class);
        startIntent.setAction(NavigationService.ACTION_START);
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_PROFILE, TEST_PROFILE);
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LAT, 48.2082d);
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LON, 16.3738d);

        ServiceController<NavigationService> controller =
                Robolectric.buildService(NavigationService.class, startIntent).create();
        NavigationService service = controller.get();

        service.onStartCommand(startIntent, 0, 1);

        ShadowService shadowService = shadowOf(service);
        assertEquals(NavigationService.NOTIFICATION_ID_ONGOING, shadowService.getLastForegroundNotificationId());
        assertNotNull(shadowService.getLastForegroundNotification());
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION, service.getForegroundServiceType());

        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE);
        assertTrue(shadowService.isForegroundStopped());

        NavigationServiceBinder binder =
                (NavigationServiceBinder) service.onBind(new Intent(ApplicationProvider.getApplicationContext(), NavigationService.class));
        binder.ensureForegroundNotification();

        assertEquals(NavigationService.NOTIFICATION_ID_ONGOING, shadowService.getLastForegroundNotificationId());
        assertNotNull(shadowService.getLastForegroundNotification());
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION, service.getForegroundServiceType());
        assertFalse(shadowService.isForegroundStopped());
    }

    @Test
    public void foregroundNotificationResumeIntentPreservesNavigationRequest() {
        Intent startIntent = new Intent(ApplicationProvider.getApplicationContext(), NavigationService.class);
        startIntent.setAction(NavigationService.ACTION_START);
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_PROFILE, TEST_PROFILE);
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_NAME, "Vienna Center");
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LAT, 48.2082d);
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LON, 16.3738d);
        startIntent.putStringArrayListExtra(
                AndroidNavigationRequestIntentContract.EXTRA_STOPS,
                new ArrayList<>(Arrays.asList("48.2100,16.3600", "48.2200,16.3900"))
        );

        ServiceController<NavigationService> controller =
                Robolectric.buildService(NavigationService.class, startIntent).create();
        NavigationService service = controller.get();

        service.onStartCommand(startIntent, 0, 1);

        ShadowService shadowService = shadowOf(service);
        Intent resumeIntent = shadowOf(shadowService.getLastForegroundNotification().contentIntent).getSavedIntent();

        assertTrue(resumeIntent.getBooleanExtra(MainActivity.EXTRA_OPEN_NAVIGATION, false));
        assertTrue(resumeIntent.getBooleanExtra(NavigationActivity.EXTRA_RESUME_EXISTING, false));
        assertEquals(TEST_PROFILE, resumeIntent.getStringExtra(AndroidNavigationRequestIntentContract.EXTRA_PROFILE));
        assertEquals("Vienna Center", resumeIntent.getStringExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_NAME));
        assertEquals(48.2082d, resumeIntent.getDoubleExtra(
                AndroidNavigationRequestIntentContract.EXTRA_DEST_LAT,
                Double.NaN
        ), 0.0);
        assertEquals(16.3738d, resumeIntent.getDoubleExtra(
                AndroidNavigationRequestIntentContract.EXTRA_DEST_LON,
                Double.NaN
        ), 0.0);
        assertEquals(
                Arrays.asList("48.21,16.36", "48.22,16.39"),
                resumeIntent.getStringArrayListExtra(AndroidNavigationRequestIntentContract.EXTRA_STOPS)
        );
    }

    @Test
    public void foregroundNotificationResumeIntentPreservesStraightLineMode() {
        Intent startIntent = new Intent(ApplicationProvider.getApplicationContext(), NavigationService.class);
        startIntent.setAction(NavigationService.ACTION_START);
        startIntent.putExtra(
                AndroidNavigationRequestIntentContract.EXTRA_ROUTING_MODE,
                NavigationRoutingMode.STRAIGHT_LINE.serializedName()
        );
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LAT, 48.2082d);
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LON, 16.3738d);

        ServiceController<NavigationService> controller =
                Robolectric.buildService(NavigationService.class, startIntent).create();
        NavigationService service = controller.get();

        service.onStartCommand(startIntent, 0, 1);

        ShadowService shadowService = shadowOf(service);
        Intent resumeIntent = shadowOf(shadowService.getLastForegroundNotification().contentIntent).getSavedIntent();

        assertEquals(
                NavigationRoutingMode.STRAIGHT_LINE.serializedName(),
                resumeIntent.getStringExtra(AndroidNavigationRequestIntentContract.EXTRA_ROUTING_MODE)
        );
        assertFalse(resumeIntent.hasExtra(AndroidNavigationRequestIntentContract.EXTRA_PROFILE));
    }

    @Test
    public void onTaskRemovedStopsNavigationAndForegroundState() {
        Intent startIntent = new Intent(ApplicationProvider.getApplicationContext(), NavigationService.class);
        startIntent.setAction(NavigationService.ACTION_START);
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_PROFILE, TEST_PROFILE);
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LAT, 48.2082d);
        startIntent.putExtra(AndroidNavigationRequestIntentContract.EXTRA_DEST_LON, 16.3738d);

        ServiceController<NavigationService> controller =
                Robolectric.buildService(NavigationService.class, startIntent).create();
        NavigationService service = controller.get();

        service.onStartCommand(startIntent, 0, 1);

        ShadowService shadowService = shadowOf(service);
        assertNotNull(shadowService.getLastForegroundNotification());

        service.onTaskRemoved(new Intent());

        assertTrue(shadowService.isForegroundStopped());
        assertTrue(shadowService.isStoppedBySelf());
    }

    public static class TestNavigationActivity extends NavigationActivity {
        boolean moveTaskToBackCalled;

        @Override
        public boolean bindService(Intent service, ServiceConnection conn, int flags) {
            return false;
        }

        @Override
        public boolean moveTaskToBack(boolean nonRoot) {
            moveTaskToBackCalled = true;
            return true;
        }
    }
}

