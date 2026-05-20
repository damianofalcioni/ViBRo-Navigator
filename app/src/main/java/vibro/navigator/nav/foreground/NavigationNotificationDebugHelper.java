package vibro.navigator.nav.foreground;


import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.format.NavigationTextFormatter;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import vibro.navigator.R;
import vibro.navigator.nav.directions.DirectionInfo;
import vibro.navigator.nav.directions.DirectionKind;
import vibro.navigator.nav.directions.VoiceHintMapper;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.logging.AppLogger;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class NavigationNotificationDebugHelper {

    private static final String TAG = "NavNotifDebug";
    private static final double BEARING_SAMPLE_DEGREES = 42.0;

    private NavigationNotificationDebugHelper() {
    }

    public enum SymbolTestGroup {
        LEFT,
        OTHER,
        RIGHT
    }

    public static void postLeftSymbolTestNotification(@NonNull Context context) {
        postSymbolTestNotification(context, SymbolTestGroup.LEFT);
    }

    public static void postOtherSymbolTestNotification(@NonNull Context context) {
        postSymbolTestNotification(context, SymbolTestGroup.OTHER);
    }

    public static void postRightSymbolTestNotification(@NonNull Context context) {
        postSymbolTestNotification(context, SymbolTestGroup.RIGHT);
    }

    public static void postSymbolTestNotification(
            @NonNull Context context,
            @NonNull SymbolTestGroup group
    ) {
        NavigationNotificationChannels.ensure(context);

        String summary = buildSymbolTestSummary(context, group);
        String message = buildSymbolTestMessage(context, group);
        long now = System.currentTimeMillis();
        int notificationId = (int) (now & 0x7fffffff);
        String notificationTag = "nav-symbol-test-"
                + group.name().toLowerCase(Locale.US)
                + "-"
                + now;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                channelIdForGroup(group)
        )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_symbol_test_title))
                .setContentText(summary)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message)
                        .setSummaryText(summary))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setVibrate(vibrationPatternForGroup(group));
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            AppLogger.w(TAG, "NotificationManager unavailable, cannot send symbol test notification");
            return;
        }
        notificationManager.notify(notificationTag, notificationId, builder.build());
        AppLogger.i(TAG, "Sent symbol test notification group=" + group
                + " tag=" + notificationTag
                + " summary=" + summary);
    }

    @NonNull
    public static String buildSymbolTestSummary(
            @NonNull Context context,
            @NonNull SymbolTestGroup group
    ) {
        StringBuilder summary = new StringBuilder();
        for (DirectionInfo directionInfo : distinctNotificationDirections(group).values()) {
            if (summary.length() > 0) {
                summary.append(' ');
            }
            summary.append(NavigationTextFormatter.formatDirectionSymbol(directionInfo));
        }
        if (group == SymbolTestGroup.OTHER) {
            summary.append(' ')
                    .append(NavigationTextFormatter.formatBearingDegrees(context, BEARING_SAMPLE_DEGREES));
        }
        return summary.toString();
    }

    @NonNull
    public static String buildSymbolTestMessage(
            @NonNull Context context,
            @NonNull SymbolTestGroup group
    ) {
        StringBuilder message = new StringBuilder();
        for (DirectionInfo directionInfo : distinctNotificationDirections(group).values()) {
            if (message.length() > 0) {
                message.append('\n');
            }
            message.append(NavigationTextFormatter.formatDirectionSymbol(directionInfo))
                    .append(' ')
                    .append(directionLabel(context, directionInfo));
        }
        if (group == SymbolTestGroup.OTHER) {
            message.append('\n')
                    .append(context.getString(
                            R.string.format_notification_symbol_test_bearing_line,
                            NavigationTextFormatter.formatBearingDegrees(context, BEARING_SAMPLE_DEGREES)
                    ));
        }
        return message.toString();
    }

    @NonNull
    private static String directionLabel(@NonNull Context context, @NonNull DirectionInfo directionInfo) {
        return directionInfo.exitNumber > 0
                ? context.getString(directionInfo.labelRes, directionInfo.exitNumber)
                : context.getString(directionInfo.labelRes);
    }

    @NonNull
    private static Map<String, DirectionInfo> distinctNotificationDirections(@NonNull SymbolTestGroup group) {
        LinkedHashMap<String, DirectionInfo> directions = new LinkedHashMap<>();
        if (group == SymbolTestGroup.LEFT) {
            addDirection(directions, group, 2, 0);
            addDirection(directions, group, 3, 0);
            addDirection(directions, group, 4, 0);
            addDirection(directions, group, 8, 0);
            addDirection(directions, group, 10, 0);
            addDirection(directions, group, 14, 2);
            addDirection(directions, group, 17, 0);
        } else if (group == SymbolTestGroup.RIGHT) {
            addDirection(directions, group, 5, 0);
            addDirection(directions, group, 6, 0);
            addDirection(directions, group, 7, 0);
            addDirection(directions, group, 9, 0);
            addDirection(directions, group, 11, 0);
            addDirection(directions, group, 13, 3);
            addDirection(directions, group, 18, 0);
        } else {
            addDirection(directions, group, 1, 0);
            addDirection(directions, group, 15, 0);
            addDirection(directions, group, 12, 0);
            addDirection(directions, group, 16, 0);
            addDirection(directions, group, 100, 0);
            addDirection(directions, group, 101, 0);
            addDirection(directions, group, Integer.MIN_VALUE, 0);
        }
        return directions;
    }

    private static void addDirection(
            @NonNull Map<String, DirectionInfo> directions,
            @NonNull SymbolTestGroup group,
            int command,
            int exitNumber
    ) {
        DirectionInfo directionInfo = VoiceHintMapper.toDirection(new VoiceHint(0, command, exitNumber, 0.0, 0));
        String symbol = NavigationTextFormatter.formatDirectionSymbol(directionInfo);
        if (matchesGroup(directionInfo.kind, group) && !directions.containsKey(symbol)) {
            directions.put(symbol, directionInfo);
        }
    }

    private static boolean matchesGroup(@NonNull DirectionKind kind, @NonNull SymbolTestGroup group) {
        if (group == SymbolTestGroup.LEFT) {
            return kind == DirectionKind.LEFT;
        }
        if (group == SymbolTestGroup.RIGHT) {
            return kind == DirectionKind.RIGHT;
        }
        return kind != DirectionKind.LEFT && kind != DirectionKind.RIGHT;
    }

    @NonNull
    private static String channelIdForGroup(@NonNull SymbolTestGroup group) {
        if (group == SymbolTestGroup.LEFT) {
            return NavigationService.CHANNEL_ID_TURN_LEFT;
        }
        if (group == SymbolTestGroup.RIGHT) {
            return NavigationService.CHANNEL_ID_TURN_RIGHT;
        }
        return NavigationService.CHANNEL_ID_ALERT;
    }

    @NonNull
    private static long[] vibrationPatternForGroup(@NonNull SymbolTestGroup group) {
        if (group == SymbolTestGroup.LEFT) {
            return NavigationNotificationChannels.leftVibrationPattern();
        }
        if (group == SymbolTestGroup.RIGHT) {
            return NavigationNotificationChannels.rightVibrationPattern();
        }
        return NavigationNotificationChannels.genericAlertVibrationPattern();
    }
}
