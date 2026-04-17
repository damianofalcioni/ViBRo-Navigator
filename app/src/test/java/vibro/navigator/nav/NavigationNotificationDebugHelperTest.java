package vibro.navigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.app.NotificationManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class NavigationNotificationDebugHelperTest {

    @Test
    public void buildSymbolTestSummaryIncludesAllNotificationSymbols() {
        Context context = ApplicationProvider.getApplicationContext();
        String summary = NavigationNotificationDebugHelper.buildSymbolTestSummary(context);

        assertEquals("⬆ ← ↖ <← → ↗ →> U↙ U↘ U↓ !!× ○>3 ◉ ■ ? 42°", summary);
    }

    @Test
    public void buildSymbolTestMessageIncludesLabelsAndBearingSample() {
        Context context = ApplicationProvider.getApplicationContext();
        String message = NavigationNotificationDebugHelper.buildSymbolTestMessage(context);

        assertTrue(message.contains("⬆ " + context.getString(R.string.direction_continue)));
        assertTrue(message.contains("← " + context.getString(R.string.direction_turn_left)));
        assertTrue(message.contains("↖ " + context.getString(R.string.direction_slight_left)));
        assertTrue(message.contains("<← " + context.getString(R.string.direction_sharp_left)));
        assertTrue(message.contains("→ " + context.getString(R.string.direction_turn_right)));
        assertTrue(message.contains("↗ " + context.getString(R.string.direction_slight_right)));
        assertTrue(message.contains("→> " + context.getString(R.string.direction_sharp_right)));
        assertTrue(message.contains("U↙ " + context.getString(R.string.direction_uturn_left)));
        assertTrue(message.contains("U↘ " + context.getString(R.string.direction_uturn_right)));
        assertTrue(message.contains("U↓ " + context.getString(R.string.direction_uturn)));
        assertTrue(message.contains("!!× " + context.getString(R.string.direction_offroute)));
        assertTrue(message.contains("○>3 " + context.getString(R.string.direction_roundabout_exit, 3)));
        assertTrue(message.contains("◉ " + context.getString(R.string.direction_beeline)));
        assertTrue(message.contains("■ " + context.getString(R.string.direction_arrive)));
        assertTrue(message.contains("? " + context.getString(R.string.direction_unknown)));
        assertTrue(message.contains(context.getString(
                R.string.format_notification_symbol_test_bearing_line,
                NavigationTextFormatter.formatBearingDegrees(context, 42.0)
        )));
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
