package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.app.NotificationManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class NavigationNotificationDebugHelperTest {

    @Test
    public void buildSymbolTestSummaryIncludesAllNotificationSymbols() {
        String summary = NavigationNotificationDebugHelper.buildSymbolTestSummary(
                ApplicationProvider.getApplicationContext()
        );

        assertEquals("⬆ ← ↖ <← → ↗ →> U↙ U↘ U↓ !!× ○>3 ◉ ■ ? 42°", summary);
    }

    @Test
    public void buildSymbolTestMessageIncludesLabelsAndBearingSample() {
        String message = NavigationNotificationDebugHelper.buildSymbolTestMessage(
                ApplicationProvider.getApplicationContext()
        );

        assertTrue(message.contains("⬆ Continue"));
        assertTrue(message.contains("← Turn left"));
        assertTrue(message.contains("↖ Slight left"));
        assertTrue(message.contains("<← Sharp left"));
        assertTrue(message.contains("→ Turn right"));
        assertTrue(message.contains("↗ Slight right"));
        assertTrue(message.contains("→> Sharp right"));
        assertTrue(message.contains("U↙ U-turn left"));
        assertTrue(message.contains("U↘ U-turn right"));
        assertTrue(message.contains("U↓ U-turn"));
        assertTrue(message.contains("!!× Off route"));
        assertTrue(message.contains("○>3 Roundabout, exit 3"));
        assertTrue(message.contains("◉ Beeline"));
        assertTrue(message.contains("■ Arrive"));
        assertTrue(message.contains("? Unknown direction"));
        assertTrue(message.contains("42° bearing sample"));
    }

    @Test
    public void postSymbolTestNotificationPostsHighPriorityAlertNotification() {
        NotificationManager notificationManager = ApplicationProvider.getApplicationContext()
                .getSystemService(NotificationManager.class);
        assertNotNull(notificationManager);

        NavigationNotificationDebugHelper.postSymbolTestNotification(
                ApplicationProvider.getApplicationContext()
        );

        StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
        assertEquals(1, notifications.length);
        Notification notification = notifications[0].getNotification();
        assertEquals(NavigationService.CHANNEL_ID_ALERT, notification.getChannelId());
        assertEquals(
                ApplicationProvider.getApplicationContext().getString(R.string.notification_symbol_test_title),
                notification.extras.getString(Notification.EXTRA_TITLE)
        );
        assertEquals(
                "⬆ ← ↖ <← → ↗ →> U↙ U↘ U↓ !!× ○>3 ◉ ■ ? 42°",
                notification.extras.getString(Notification.EXTRA_TEXT)
        );
    }
}
