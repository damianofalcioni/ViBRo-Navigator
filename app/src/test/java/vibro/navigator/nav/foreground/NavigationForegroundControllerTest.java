package vibro.navigator.nav.foreground;


import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.service.NavigationService;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;

import vibro.navigator.android.foreground.AndroidNavigationForegroundController;
import vibro.navigator.nav.route.VoiceHint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class NavigationForegroundControllerTest {

    @Test
    public void ensureChannelsCreatesSilentDirectionalChannelsAndRemovesLegacyIds() {
        ServiceController<TestService> serviceController = Robolectric.buildService(TestService.class).create();
        TestService service = serviceController.get();
        NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
        assertNotNull(notificationManager);

        notificationManager.createNotificationChannel(new NotificationChannel(
                "navigator.alerts.v1",
                "Legacy alerts",
                NotificationManager.IMPORTANCE_HIGH
        ));
        notificationManager.createNotificationChannel(new NotificationChannel(
                "navigator.turn.left",
                "Legacy left",
                NotificationManager.IMPORTANCE_HIGH
        ));
        notificationManager.createNotificationChannel(new NotificationChannel(
                "navigator.turn.left.v2",
                "Legacy left v2",
                NotificationManager.IMPORTANCE_HIGH
        ));
        notificationManager.createNotificationChannel(new NotificationChannel(
                "navigator.turn.right",
                "Legacy right",
                NotificationManager.IMPORTANCE_HIGH
        ));
        notificationManager.createNotificationChannel(new NotificationChannel(
                "navigator.turn.right.v2",
                "Legacy right v2",
                NotificationManager.IMPORTANCE_HIGH
        ));

        NavigationForegroundController controller = new AndroidNavigationForegroundController(service);
        controller.ensureChannels();

        assertNull(notificationManager.getNotificationChannel("navigator.alerts.v1"));
        assertNull(notificationManager.getNotificationChannel("navigator.turn.left"));
        assertNull(notificationManager.getNotificationChannel("navigator.turn.left.v2"));
        assertNull(notificationManager.getNotificationChannel("navigator.turn.right"));
        assertNull(notificationManager.getNotificationChannel("navigator.turn.right.v2"));

        NotificationChannel navChannel = notificationManager.getNotificationChannel(NavigationService.CHANNEL_ID_NAV);
        assertNotNull(navChannel);
        assertFalse(navChannel.shouldVibrate());
        assertNull(navChannel.getSound());

        NotificationChannel alertChannel = notificationManager.getNotificationChannel(NavigationService.CHANNEL_ID_ALERT);
        assertNotNull(alertChannel);
        assertTrue(alertChannel.shouldVibrate());
        assertArrayEquals(new long[]{0L, 600L}, alertChannel.getVibrationPattern());
        assertNull(alertChannel.getSound());

        NotificationChannel leftChannel = notificationManager.getNotificationChannel(NavigationService.CHANNEL_ID_TURN_LEFT);
        assertNotNull(leftChannel);
        assertArrayEquals(new long[]{0L, 300L, 100L, 300L}, leftChannel.getVibrationPattern());
        assertNull(leftChannel.getSound());

        NotificationChannel rightChannel = notificationManager.getNotificationChannel(NavigationService.CHANNEL_ID_TURN_RIGHT);
        assertNotNull(rightChannel);
        assertArrayEquals(new long[]{0L, 300L, 100L, 300L, 100L, 300L}, rightChannel.getVibrationPattern());
        assertNull(rightChannel.getSound());

        serviceController.destroy();
    }

    @Test
    public void notificationsUseExpectedChannels() {
        ServiceController<TestService> serviceController = Robolectric.buildService(TestService.class).create();
        TestService service = serviceController.get();
        NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
        assertNotNull(notificationManager);

        NavigationForegroundController controller = new AndroidNavigationForegroundController(service);
        controller.ensureChannels();

        controller.sendImminentTurnNotification(new VoiceHint(0, 2, 0, 0.0, 0), 50.0, 5.0);
        assertEquals(
                NavigationService.CHANNEL_ID_TURN_LEFT,
                lastPostedNotification(notificationManager).getChannelId()
        );

        controller.sendImminentTurnNotification(new VoiceHint(0, 5, 0, 0.0, 0), 50.0, 5.0);
        assertEquals(
                NavigationService.CHANNEL_ID_TURN_RIGHT,
                lastPostedNotification(notificationManager).getChannelId()
        );

        controller.sendImminentTurnNotification(new VoiceHint(0, 1, 0, 0.0, 0), 50.0, 5.0);
        assertEquals(
                NavigationService.CHANNEL_ID_ALERT,
                lastPostedNotification(notificationManager).getChannelId()
        );

        RouteDeviationPolicy policy = new RouteDeviationPolicy();
        NavigationRerouteNotice rerouteNotice = NavigationRerouteNotice.fromDecision(
                policy.evaluate(40.0, 5.0, null, 90.0)
        );
        controller.sendOffRouteNotification(rerouteNotice);
        assertEquals(
                NavigationService.CHANNEL_ID_ALERT,
                lastPostedNotification(notificationManager).getChannelId()
        );

        controller.sendWrongDirectionNotification(new NavigationWrongDirectionNotice(90.0, 270.0, 180.0));
        assertEquals(
                NavigationService.CHANNEL_ID_ALERT,
                lastPostedNotification(notificationManager).getChannelId()
        );

        serviceController.destroy();
    }

    private static Notification lastPostedNotification(NotificationManager notificationManager) {
        StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
        assertEquals(1, notifications.length);
        return notifications[0].getNotification();
    }

    public static final class TestService extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}
