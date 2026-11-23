package vibro.navigator.android.foreground;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.car.app.connection.CarConnection;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import vibro.navigator.nav.foreground.NavigationForegroundControllerTest.TestService;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.settings.AppNotificationSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, shadows = AndroidAutoGuidanceNotificationsTest.ConnectionShadow.class)
public class AndroidAutoGuidanceNotificationsTest {
    private static final VoiceHint LEFT_TURN = new VoiceHint(0, 2, 0, 0.0, 0);

    @Test
    public void projectionChangesAlertChannelDuringTheSameServiceSession() {
        ServiceController<TestService> serviceController = Robolectric.buildService(TestService.class).create();
        TestService service = serviceController.get();
        AppNotificationSettings.setNavigationNotificationsEnabled(service, true);
        NotificationManager manager = service.getSystemService(NotificationManager.class);
        AndroidNavigationForegroundController controller = new AndroidNavigationForegroundController(service);
        controller.ensureChannels();

        NotificationChannel silentChannel = manager.getNotificationChannel(
                AndroidNavigationNotificationChannels.CHANNEL_ID_SILENT_GUIDANCE);
        assertNotNull(silentChannel);
        assertEquals(NotificationManager.IMPORTANCE_LOW, silentChannel.getImportance());
        assertFalse(silentChannel.shouldVibrate());
        assertNull(silentChannel.getSound());

        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertSilentAlert(manager);

        ConnectionShadow.type.setValue(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertEquals(NavigationService.CHANNEL_ID_TURN_LEFT, postedAlert(manager).getChannelId());

        ConnectionShadow.type.setValue(CarConnection.CONNECTION_TYPE_PROJECTION);
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertSilentAlert(manager);
        controller.sendStationaryOrientationNotification(new StationaryOrientationAdvisor.Decision(45.0));
        assertSilentAlert(manager);
        controller.sendOffRouteNotification(NavigationRerouteNotice.fromDecision(
                new RouteDeviationPolicy().evaluate(40.0, 5.0, null, 90.0)));
        assertSilentAlert(manager);
        controller.sendWrongDirectionNotification(new NavigationWrongDirectionNotice(90.0, 270.0, 180.0));
        assertSilentAlert(manager);

        ConnectionShadow.type.setValue(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertEquals(NavigationService.CHANNEL_ID_TURN_LEFT, postedAlert(manager).getChannelId());
        serviceController.destroy();
    }

    @Test
    public void projectedSymbolTestIsStillPostedSilently() {
        ServiceController<TestService> serviceController = Robolectric.buildService(TestService.class).create();
        TestService service = serviceController.get();
        ConnectionShadow.type.setValue(CarConnection.CONNECTION_TYPE_PROJECTION);

        assertTrue(AndroidNavigationNotificationDebugHelper.postRightSymbolTestNotification(service));
        assertSilentAlert(service.getSystemService(NotificationManager.class));
        serviceController.destroy();
    }

    @Test
    public void blockedTurnCategoryStaysBlockedDuringProjection() {
        ServiceController<TestService> serviceController = Robolectric.buildService(TestService.class).create();
        TestService service = serviceController.get();
        AppNotificationSettings.setNavigationNotificationsEnabled(service, true);
        NotificationManager manager = service.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(NavigationService.CHANNEL_ID_TURN_LEFT,
                "Left turns", NotificationManager.IMPORTANCE_NONE));
        AndroidNavigationForegroundController controller = new AndroidNavigationForegroundController(service);
        controller.ensureChannels();

        ConnectionShadow.type.setValue(CarConnection.CONNECTION_TYPE_PROJECTION);
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertFalse(AndroidNavigationNotificationDebugHelper.postLeftSymbolTestNotification(service));
        assertEquals(0, manager.getActiveNotifications().length);

        controller.sendWrongDirectionNotification(new NavigationWrongDirectionNotice(90.0, 270.0, 180.0));
        assertSilentAlert(manager);
        serviceController.destroy();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N_MR1)
    @SuppressWarnings("deprecation") // Before notification channels, the vibration pattern lives on the notification.
    public void legacyTurnAlertVibratesOnlyWhileDisconnected() {
        ServiceController<TestService> serviceController = Robolectric.buildService(TestService.class).create();
        TestService service = serviceController.get();
        AppNotificationSettings.setNavigationNotificationsEnabled(service, true);
        NotificationManager manager = (NotificationManager) service.getSystemService(TestService.NOTIFICATION_SERVICE);
        AndroidNavigationForegroundController controller = new AndroidNavigationForegroundController(service);

        ConnectionShadow.type.setValue(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertNotNull(postedAlert(manager).vibrate);
        ConnectionShadow.type.setValue(CarConnection.CONNECTION_TYPE_PROJECTION);
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertNull(postedAlert(manager).vibrate);
        serviceController.destroy();
    }

    private static void assertSilentAlert(NotificationManager manager) {
        Notification alert = postedAlert(manager);
        assertEquals(AndroidNavigationNotificationChannels.CHANNEL_ID_SILENT_GUIDANCE, alert.getChannelId());
        assertFalse((alert.flags & Notification.FLAG_LOCAL_ONLY) != 0);
        assertNotNull(alert.extras.getCharSequence(Notification.EXTRA_TEXT));
    }

    private static Notification postedAlert(NotificationManager manager) {
        StatusBarNotification[] alerts = manager.getActiveNotifications();
        assertEquals(1, alerts.length);
        return alerts[0].getNotification();
    }

    @Implements(value = CarConnection.class, isInAndroidSdk = false)
    public static final class ConnectionShadow {
        static final MutableLiveData<Integer> type = new MutableLiveData<>();

        @Implementation
        protected LiveData<Integer> getType() {
            return type;
        }
    }
}
