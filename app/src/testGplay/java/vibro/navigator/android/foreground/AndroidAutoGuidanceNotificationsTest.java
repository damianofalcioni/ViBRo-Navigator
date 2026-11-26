package vibro.navigator.android.foreground;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.service.notification.StatusBarNotification;

import androidx.car.app.connection.CarConnection;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;

import vibro.navigator.android.automotive.AndroidAutoConnectionMonitor;
import vibro.navigator.nav.foreground.NavigationForegroundControllerTest.TestService;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.settings.AppNotificationSettings;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class AndroidAutoGuidanceNotificationsTest {
    private static final VoiceHint LEFT_TURN = new VoiceHint(0, 2, 0, 0.0, 0);

    private final ArrayDeque<Runnable> workerTasks = new ArrayDeque<>();
    private final ConnectionProvider provider = new ConnectionProvider();
    private Application application;

    @Before
    public void startConnectionMonitor() {
        application = ApplicationProvider.getApplicationContext();
        ShadowContentResolver.registerProviderInternal("androidx.car.app.connection", provider);
        ReflectionHelpers.callStaticMethod(AndroidAutoConnectionMonitor.class, "start",
                ReflectionHelpers.ClassParameter.from(Context.class, application),
                ReflectionHelpers.ClassParameter.from(Executor.class, workerTasks::addLast));
    }

    @After
    public void clearConnectionMonitor() {
        ReflectionHelpers.setStaticField(AndroidAutoConnectionMonitor.class, "silenceGuidance", false);
    }

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

        changeHostConnection(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
        assertEquals(1, workerTasks.size());
        workerTasks.removeFirst().run();
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertSilentAlert(manager);

        runWorkerTasks();
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertEquals(NavigationService.CHANNEL_ID_TURN_LEFT, postedAlert(manager).getChannelId());

        changeHostConnection(CarConnection.CONNECTION_TYPE_PROJECTION);
        changeHostConnection(CarConnection.CONNECTION_TYPE_PROJECTION);
        assertEquals(1, workerTasks.size());
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertSilentAlert(manager);
        runWorkerTasks();
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertSilentAlert(manager);
        controller.sendStationaryOrientationNotification(new StationaryOrientationAdvisor.Decision(45.0));
        assertSilentAlert(manager);
        controller.sendOffRouteNotification(NavigationRerouteNotice.fromDecision(
                new RouteDeviationPolicy().evaluate(40.0, 5.0, null, 90.0)));
        assertSilentAlert(manager);
        controller.sendWrongDirectionNotification(new NavigationWrongDirectionNotice(90.0, 270.0, 180.0));
        assertSilentAlert(manager);

        setHostConnection(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertEquals(NavigationService.CHANNEL_ID_TURN_LEFT, postedAlert(manager).getChannelId());
        serviceController.destroy();
    }

    @Test
    public void projectedSymbolTestIsStillPostedSilently() {
        ServiceController<TestService> serviceController = Robolectric.buildService(TestService.class).create();
        TestService service = serviceController.get();
        setHostConnection(CarConnection.CONNECTION_TYPE_PROJECTION);

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

        setHostConnection(CarConnection.CONNECTION_TYPE_PROJECTION);
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

        setHostConnection(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
        controller.sendImminentTurnNotification(LEFT_TURN, 50.0, 5.0);
        assertNotNull(postedAlert(manager).vibrate);
        setHostConnection(CarConnection.CONNECTION_TYPE_PROJECTION);
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

    private void setHostConnection(int type) {
        changeHostConnection(type);
        runWorkerTasks();
    }

    private void changeHostConnection(int type) {
        provider.type = type;
        application.sendBroadcast(new Intent(CarConnection.ACTION_CAR_CONNECTION_UPDATED));
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void runWorkerTasks() {
        while (!workerTasks.isEmpty()) {
            workerTasks.removeFirst().run();
        }
    }

    private static final class ConnectionProvider extends ContentProvider {
        private int type = CarConnection.CONNECTION_TYPE_NOT_CONNECTED;

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] arguments, String order) {
            assertEquals(CarConnection.CAR_CONNECTION_STATE, projection[0]);
            MatrixCursor state = new MatrixCursor(new String[]{CarConnection.CAR_CONNECTION_STATE});
            state.newRow().add(type);
            return state;
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] arguments) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(Uri uri, String selection, String[] arguments) {
            throw new UnsupportedOperationException();
        }
    }
}
