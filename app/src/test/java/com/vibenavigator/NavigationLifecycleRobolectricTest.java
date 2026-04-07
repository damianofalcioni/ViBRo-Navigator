package com.vibenavigator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.content.ServiceConnection;

import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.nav.NavigationRequest;
import com.vibenavigator.nav.NavigationService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowService;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class NavigationLifecycleRobolectricTest {

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
    public void ensureForegroundNotificationRestoresForegroundState() {
        Intent startIntent = new Intent(ApplicationProvider.getApplicationContext(), NavigationService.class);
        startIntent.setAction(NavigationService.ACTION_START);
        startIntent.putExtra(NavigationRequest.EXTRA_PROFILE, "test-profile");
        startIntent.putExtra(NavigationRequest.EXTRA_DEST_LAT, 48.2082d);
        startIntent.putExtra(NavigationRequest.EXTRA_DEST_LON, 16.3738d);

        ServiceController<NavigationService> controller =
                Robolectric.buildService(NavigationService.class, startIntent).create();
        NavigationService service = controller.get();

        service.onStartCommand(startIntent, 0, 1);

        ShadowService shadowService = shadowOf(service);
        assertEquals(NavigationService.NOTIFICATION_ID_ONGOING, shadowService.getLastForegroundNotificationId());
        assertNotNull(shadowService.getLastForegroundNotification());

        service.stopForeground(true);
        assertTrue(shadowService.isForegroundStopped());

        NavigationService.LocalBinder binder =
                (NavigationService.LocalBinder) service.onBind(new Intent(ApplicationProvider.getApplicationContext(), NavigationService.class));
        binder.ensureForegroundNotification();

        assertEquals(NavigationService.NOTIFICATION_ID_ONGOING, shadowService.getLastForegroundNotificationId());
        assertNotNull(shadowService.getLastForegroundNotification());
        assertFalse(shadowService.isForegroundStopped());
    }

    @Test
    public void foregroundNotificationResumeIntentPreservesNavigationRequest() {
        Intent startIntent = new Intent(ApplicationProvider.getApplicationContext(), NavigationService.class);
        startIntent.setAction(NavigationService.ACTION_START);
        startIntent.putExtra(NavigationRequest.EXTRA_PROFILE, "test-profile");
        startIntent.putExtra(NavigationRequest.EXTRA_DEST_NAME, "Vienna Center");
        startIntent.putExtra(NavigationRequest.EXTRA_DEST_LAT, 48.2082d);
        startIntent.putExtra(NavigationRequest.EXTRA_DEST_LON, 16.3738d);
        startIntent.putStringArrayListExtra(
                NavigationRequest.EXTRA_STOPS,
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
        assertEquals("test-profile", resumeIntent.getStringExtra(NavigationRequest.EXTRA_PROFILE));
        assertEquals("Vienna Center", resumeIntent.getStringExtra(NavigationRequest.EXTRA_DEST_NAME));
        assertEquals(48.2082d, resumeIntent.getDoubleExtra(NavigationRequest.EXTRA_DEST_LAT, Double.NaN), 0.0);
        assertEquals(16.3738d, resumeIntent.getDoubleExtra(NavigationRequest.EXTRA_DEST_LON, Double.NaN), 0.0);
        assertEquals(
                Arrays.asList("48.21,16.36", "48.22,16.39"),
                resumeIntent.getStringArrayListExtra(NavigationRequest.EXTRA_STOPS)
        );
    }

    @Test
    public void onTaskRemovedStopsNavigationAndForegroundState() {
        Intent startIntent = new Intent(ApplicationProvider.getApplicationContext(), NavigationService.class);
        startIntent.setAction(NavigationService.ACTION_START);
        startIntent.putExtra(NavigationRequest.EXTRA_PROFILE, "test-profile");
        startIntent.putExtra(NavigationRequest.EXTRA_DEST_LAT, 48.2082d);
        startIntent.putExtra(NavigationRequest.EXTRA_DEST_LON, 16.3738d);

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
