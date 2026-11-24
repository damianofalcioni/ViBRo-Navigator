package vibro.navigator.android.automotive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;

import androidx.car.app.notification.CarAppExtender;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import vibro.navigator.R;
import vibro.navigator.android.foreground.AndroidNavigationNotificationChannels;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.ui.NavigationActivity;
import vibro.navigator.settings.AppNotificationSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 33})
public class AndroidCarNavigationNotificationsGplayTest {
    @Test
    public void ongoingCarNotificationUpdatesAndClearsWithoutRemovingWearableAlerts() {
        Application context = ApplicationProvider.getApplicationContext();
        AndroidNavigationNotificationChannels.ensure(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.cancelAll();
        manager.notify(NavigationService.NOTIFICATION_ID_TURN,
                new NotificationCompat.Builder(context, NavigationService.CHANNEL_ID_ALERT)
                        .setSmallIcon(R.drawable.ic_notification).setContentText("Wearable alert").build());
        AndroidCarNavigationNotifications notifications = new AndroidCarNavigationNotifications(context);
        AppNotificationSettings.setNavigationNotificationsEnabled(context, false);
        notifications.update(state("Turn left"));
        Notification notification = shadowOf(manager).getNotification(AndroidCarNavigationNotifications.NOTIFICATION_ID);
        assertEquals(NotificationCompat.CATEGORY_NAVIGATION, notification.category);
        assertTrue((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0);
        assertEquals(context.getString(R.string.notification_nav_running_text),
                notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString());
        assertTrue(shadowOf(notification.contentIntent).getSavedIntent()
                .getBooleanExtra(NavigationActivity.EXTRA_RESUME_EXISTING, false));
        CarAppExtender extension = new CarAppExtender(notification);
        assertEquals("Turn left", extension.getContentText().toString());
        assertEquals(NotificationManagerCompat.IMPORTANCE_LOW, extension.getImportance());
        assertNotEquals(notification.contentIntent, extension.getContentIntent());
        notifications.update(state("Turn right"));
        assertEquals("Turn right", new CarAppExtender(shadowOf(manager)
                .getNotification(AndroidCarNavigationNotifications.NOTIFICATION_ID)).getContentText().toString());
        notifications.clear();
        assertEquals(1, manager.getActiveNotifications().length);
        assertEquals(NavigationService.NOTIFICATION_ID_TURN, manager.getActiveNotifications()[0].getId());
    }

    private static NavState state(String cue) {
        return new NavState(new NavRouteStatus(new NavGuidanceStatus(cue, ""),
                new NavProgressStatus("", "", ""), null),
                new NavGpsStatus("", NavState.NO_DEADLINE), new NavPauseStatus(false));
    }
}
