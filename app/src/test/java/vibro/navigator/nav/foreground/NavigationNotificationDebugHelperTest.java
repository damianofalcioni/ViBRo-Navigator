package vibro.navigator.nav.foreground;


import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.foreground.NavigationNotificationDebugHelper.SymbolTestGroup;
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
    public void buildSymbolTestSummariesSplitNotificationSymbolsByVibrationGroup() {
        Context context = ApplicationProvider.getApplicationContext();

        assertEquals(
                "← ↖ <← U↙ <○2",
                NavigationNotificationDebugHelper.buildSymbolTestSummary(context, SymbolTestGroup.LEFT)
        );
        assertEquals(
                "⬆ U↓ !!× ◉ ■ ? 42°",
                NavigationNotificationDebugHelper.buildSymbolTestSummary(context, SymbolTestGroup.OTHER)
        );
        assertEquals(
                "→ ↗ →> U↘ ○>3",
                NavigationNotificationDebugHelper.buildSymbolTestSummary(context, SymbolTestGroup.RIGHT)
        );
    }

    @Test
    public void buildSymbolTestMessagesIncludeLabelsForEachGroup() {
        Context context = ApplicationProvider.getApplicationContext();
        String leftMessage = NavigationNotificationDebugHelper.buildSymbolTestMessage(context, SymbolTestGroup.LEFT);
        String otherMessage = NavigationNotificationDebugHelper.buildSymbolTestMessage(context, SymbolTestGroup.OTHER);
        String rightMessage = NavigationNotificationDebugHelper.buildSymbolTestMessage(context, SymbolTestGroup.RIGHT);

        assertTrue(leftMessage.contains("← " + context.getString(R.string.direction_turn_left)));
        assertTrue(leftMessage.contains("↖ " + context.getString(R.string.direction_slight_left)));
        assertTrue(leftMessage.contains("<← " + context.getString(R.string.direction_sharp_left)));
        assertTrue(leftMessage.contains("U↙ " + context.getString(R.string.direction_uturn_left)));
        assertTrue(leftMessage.contains("<○2 " + context.getString(R.string.direction_roundabout_exit_left, 2)));
        assertTrue(otherMessage.contains("⬆ " + context.getString(R.string.direction_continue)));
        assertTrue(otherMessage.contains("U↓ " + context.getString(R.string.direction_uturn)));
        assertTrue(otherMessage.contains("!!× " + context.getString(R.string.direction_offroute)));
        assertTrue(otherMessage.contains("◉ " + context.getString(R.string.direction_beeline)));
        assertTrue(otherMessage.contains("■ " + context.getString(R.string.direction_arrive)));
        assertTrue(otherMessage.contains("? " + context.getString(R.string.direction_unknown)));
        assertTrue(otherMessage.contains(context.getString(
                R.string.format_notification_symbol_test_bearing_line,
                NavigationTextFormatter.formatBearingDegrees(context, 42.0)
        )));
        assertTrue(rightMessage.contains("→ " + context.getString(R.string.direction_turn_right)));
        assertTrue(rightMessage.contains("↗ " + context.getString(R.string.direction_slight_right)));
        assertTrue(rightMessage.contains("→> " + context.getString(R.string.direction_sharp_right)));
        assertTrue(rightMessage.contains("U↘ " + context.getString(R.string.direction_uturn_right)));
        assertTrue(rightMessage.contains("○>3 " + context.getString(R.string.direction_roundabout_exit, 3)));
    }

    @Test
    public void postSymbolTestNotificationUsesLeftChannelForLeftSymbols() {
        assertPostedGroup(SymbolTestGroup.LEFT, NavigationService.CHANNEL_ID_TURN_LEFT, "← ↖ <← U↙ <○2");
    }

    @Test
    public void postSymbolTestNotificationUsesAlertChannelForOtherSymbols() {
        assertPostedGroup(SymbolTestGroup.OTHER, NavigationService.CHANNEL_ID_ALERT, "⬆ U↓ !!× ◉ ■ ? 42°");
    }

    @Test
    public void postSymbolTestNotificationUsesRightChannelForRightSymbols() {
        assertPostedGroup(SymbolTestGroup.RIGHT, NavigationService.CHANNEL_ID_TURN_RIGHT, "→ ↗ →> U↘ ○>3");
    }

    private static void assertPostedGroup(
            SymbolTestGroup group,
            String expectedChannelId,
            String expectedSummary
    ) {
        NotificationManager notificationManager = ApplicationProvider.getApplicationContext()
                .getSystemService(NotificationManager.class);
        assertNotNull(notificationManager);
        notificationManager.cancelAll();

        NavigationNotificationDebugHelper.postSymbolTestNotification(ApplicationProvider.getApplicationContext(), group);

        StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
        assertEquals(1, notifications.length);
        Notification notification = notifications[0].getNotification();
        assertEquals(expectedChannelId, notification.getChannelId());
        assertEquals(
                ApplicationProvider.getApplicationContext().getString(R.string.notification_symbol_test_title),
                notification.extras.getString(Notification.EXTRA_TITLE)
        );
        assertEquals(
                expectedSummary,
                notification.extras.getString(Notification.EXTRA_TEXT)
        );
    }
}
