package com.vibenavigator.nav;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.vibenavigator.R;
import com.vibenavigator.nav.directions.DirectionInfo;
import com.vibenavigator.nav.directions.VoiceHintMapper;
import com.vibenavigator.nav.route.VoiceHint;
import com.vibenavigator.util.AppLogger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NavigationNotificationDebugHelper {

    private static final String TAG = "NavNotifDebug";

    private NavigationNotificationDebugHelper() {
    }

    public static void postSymbolTestNotification(@NonNull Context context) {
        NavigationNotificationChannels.ensure(context);

        String summary = buildSymbolTestSummary(context);
        String message = buildSymbolTestMessage(context);
        long now = System.currentTimeMillis();
        int notificationId = (int) (now & 0x7fffffff);
        String notificationTag = "nav-symbol-test-" + now;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                NavigationService.CHANNEL_ID_ALERT
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
            builder.setVibrate(NavigationNotificationChannels.genericAlertVibrationPattern());
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            AppLogger.w(TAG, "NotificationManager unavailable, cannot send symbol test notification");
            return;
        }
        notificationManager.notify(notificationTag, notificationId, builder.build());
        AppLogger.i(TAG, "Sent symbol test notification tag=" + notificationTag + " summary=" + summary);
    }

    @NonNull
    static String buildSymbolTestSummary(@NonNull Context context) {
        StringBuilder summary = new StringBuilder();
        for (DirectionInfo directionInfo : distinctNotificationDirections().values()) {
            if (summary.length() > 0) {
                summary.append(' ');
            }
            summary.append(NavigationTextFormatter.formatDirectionSymbol(directionInfo));
        }
        summary.append(' ')
                .append(NavigationTextFormatter.formatBearingDegrees(context, 42.0));
        return summary.toString();
    }

    @NonNull
    static String buildSymbolTestMessage(@NonNull Context context) {
        StringBuilder message = new StringBuilder();
        for (DirectionInfo directionInfo : distinctNotificationDirections().values()) {
            if (message.length() > 0) {
                message.append('\n');
            }
            message.append(NavigationTextFormatter.formatDirectionSymbol(directionInfo))
                    .append(' ')
                    .append(directionLabel(context, directionInfo));
        }
        message.append('\n')
                .append(context.getString(
                        R.string.format_notification_symbol_test_bearing_line,
                        NavigationTextFormatter.formatBearingDegrees(context, 42.0)
                ));
        return message.toString();
    }

    @NonNull
    private static String directionLabel(@NonNull Context context, @NonNull DirectionInfo directionInfo) {
        return directionInfo.exitNumber > 0
                ? context.getString(directionInfo.labelRes, directionInfo.exitNumber)
                : context.getString(directionInfo.labelRes);
    }

    @NonNull
    private static Map<String, DirectionInfo> distinctNotificationDirections() {
        LinkedHashMap<String, DirectionInfo> directions = new LinkedHashMap<>();
        addDirection(directions, 1, 0);
        addDirection(directions, 2, 0);
        addDirection(directions, 3, 0);
        addDirection(directions, 4, 0);
        addDirection(directions, 5, 0);
        addDirection(directions, 6, 0);
        addDirection(directions, 7, 0);
        addDirection(directions, 10, 0);
        addDirection(directions, 11, 0);
        addDirection(directions, 15, 0);
        addDirection(directions, 12, 0);
        addDirection(directions, 13, 3);
        addDirection(directions, 16, 0);
        addDirection(directions, 100, 0);
        addDirection(directions, Integer.MIN_VALUE, 0);
        return directions;
    }

    private static void addDirection(
            @NonNull Map<String, DirectionInfo> directions,
            int command,
            int exitNumber
    ) {
        DirectionInfo directionInfo = VoiceHintMapper.toDirection(new VoiceHint(0, command, exitNumber, 0.0, 0));
        if (!directions.containsKey(directionInfo.emoji)) {
            directions.put(directionInfo.emoji, directionInfo);
        }
    }
}
