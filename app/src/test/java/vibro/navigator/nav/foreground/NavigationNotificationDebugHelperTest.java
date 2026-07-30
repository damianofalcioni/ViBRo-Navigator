package vibro.navigator.nav.foreground;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import vibro.navigator.R;
import vibro.navigator.android.foreground.AndroidNavigationNotificationDebugHelper;
import vibro.navigator.android.foreground.AndroidNavigationNotificationDebugHelper.SymbolTestGroup;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.settings.AppNotificationSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class NavigationNotificationDebugHelperTest {
    @Test
    public void postSymbolTestNotificationUsesLeftChannelForLeftSymbols() {
        assertPostedGroup(
                SymbolTestGroup.LEFT,
                NavigationService.CHANNEL_ID_TURN_LEFT,
                "← ↖ <← U↙ <○2",
                R.string.notification_symbol_test_left_title
        );
    }

    @Test
    public void postSymbolTestNotificationUsesAlertChannelForOtherSymbols() {
        assertPostedGroup(
                SymbolTestGroup.OTHER,
                NavigationService.CHANNEL_ID_ALERT,
                "⬆ U↓ !!× ◉ ■ ? 42°",
                R.string.notification_symbol_test_other_title
        );
    }

    @Test
    public void postSymbolTestNotificationUsesRightChannelForRightSymbols() {
        assertPostedGroup(
                SymbolTestGroup.RIGHT,
                NavigationService.CHANNEL_ID_TURN_RIGHT,
                "→ ↗ →> U↘ ○>3",
                R.string.notification_symbol_test_right_title
        );
    }

    @Test
    public void postSymbolTestNotificationIgnoresNavigationNotificationsSetting() {
        Context context = ApplicationProvider.getApplicationContext();
        AppNotificationSettings.setNavigationNotificationsEnabled(context, false);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        assertNotNull(notificationManager);
        notificationManager.cancelAll();

        boolean sent = AndroidNavigationNotificationDebugHelper.postSymbolTestNotification(
                context,
                SymbolTestGroup.OTHER
        );

        assertEquals(1, notificationManager.getActiveNotifications().length);
        assertTrue(sent);
    }

    private static void assertPostedGroup(
            SymbolTestGroup group,
            String expectedChannelId,
            String expectedSummary,
            int expectedTitleRes
    ) {
        NotificationManager notificationManager = ApplicationProvider.getApplicationContext()
                .getSystemService(NotificationManager.class);
        assertNotNull(notificationManager);
        AppNotificationSettings.setNavigationNotificationsEnabled(
                ApplicationProvider.getApplicationContext(),
                true
        );
        notificationManager.cancelAll();

        AndroidNavigationNotificationDebugHelper.postSymbolTestNotification(
                ApplicationProvider.getApplicationContext(),
                group
        );

        StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
        assertEquals(1, notifications.length);
        Notification notification = notifications[0].getNotification();
        assertEquals(expectedChannelId, notification.getChannelId());
        assertNotNull(notification.getSmallIcon());
        assertEquals(R.drawable.ic_notification, notification.getSmallIcon().getResId());
        assertEquals(
                ApplicationProvider.getApplicationContext().getString(expectedTitleRes),
                notification.extras.getString(Notification.EXTRA_TITLE)
        );
        assertEquals(
                expectedSummary,
                notification.extras.getString(Notification.EXTRA_TEXT)
        );
    }
}
