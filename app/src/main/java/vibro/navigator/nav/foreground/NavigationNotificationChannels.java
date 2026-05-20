package vibro.navigator.nav.foreground;


import vibro.navigator.nav.service.NavigationService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;

public final class NavigationNotificationChannels {

    private static final String TAG = "NavNotifChannels";
    private static final String LEGACY_CHANNEL_ID_ALERT_V1 = "navigator.alerts.v1";
    private static final String LEGACY_CHANNEL_ID_TURN_LEFT = "navigator.turn.left";
    private static final String LEGACY_CHANNEL_ID_TURN_LEFT_V2 = "navigator.turn.left.v2";
    private static final String LEGACY_CHANNEL_ID_TURN_RIGHT = "navigator.turn.right";
    private static final String LEGACY_CHANNEL_ID_TURN_RIGHT_V2 = "navigator.turn.right.v2";

    private NavigationNotificationChannels() {
    }

    public static void ensure(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            AppLogger.w(TAG, "NotificationManager unavailable while creating channels");
            return;
        }

        deleteObsoleteChannels(notificationManager);

        NotificationChannel navChannel = new NotificationChannel(
                NavigationService.CHANNEL_ID_NAV,
                context.getString(R.string.notification_channel_nav),
                NotificationManager.IMPORTANCE_LOW
        );
        navChannel.enableVibration(false);
        navChannel.setSound(null, null);
        notificationManager.createNotificationChannel(navChannel);

        NotificationChannel alertChannel = new NotificationChannel(
                NavigationService.CHANNEL_ID_ALERT,
                context.getString(R.string.notification_channel_alert),
                NotificationManager.IMPORTANCE_HIGH
        );
        alertChannel.enableVibration(true);
        alertChannel.setVibrationPattern(genericAlertVibrationPattern());
        alertChannel.setSound(null, null);
        notificationManager.createNotificationChannel(alertChannel);

        NotificationChannel leftChannel = new NotificationChannel(
                NavigationService.CHANNEL_ID_TURN_LEFT,
                context.getString(R.string.notification_channel_turn_left),
                NotificationManager.IMPORTANCE_HIGH
        );
        leftChannel.enableVibration(true);
        leftChannel.setVibrationPattern(leftVibrationPattern());
        leftChannel.setSound(null, null);
        notificationManager.createNotificationChannel(leftChannel);

        NotificationChannel rightChannel = new NotificationChannel(
                NavigationService.CHANNEL_ID_TURN_RIGHT,
                context.getString(R.string.notification_channel_turn_right),
                NotificationManager.IMPORTANCE_HIGH
        );
        rightChannel.enableVibration(true);
        rightChannel.setVibrationPattern(rightVibrationPattern());
        rightChannel.setSound(null, null);
        notificationManager.createNotificationChannel(rightChannel);
        AppLogger.i(TAG, "Notification channels ensured");
    }

    @NonNull
    public static long[] genericAlertVibrationPattern() {
        return new long[]{0, 600};
    }

    @NonNull
    public static long[] leftVibrationPattern() {
        return new long[]{0, 300, 100, 300};
    }

    @NonNull
    public static long[] rightVibrationPattern() {
        return new long[]{0, 300, 100, 300, 100, 300};
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static void deleteObsoleteChannels(@NonNull NotificationManager notificationManager) {
        deleteChannelIfPresent(notificationManager, LEGACY_CHANNEL_ID_ALERT_V1);
        deleteChannelIfPresent(notificationManager, LEGACY_CHANNEL_ID_TURN_LEFT);
        deleteChannelIfPresent(notificationManager, LEGACY_CHANNEL_ID_TURN_LEFT_V2);
        deleteChannelIfPresent(notificationManager, LEGACY_CHANNEL_ID_TURN_RIGHT);
        deleteChannelIfPresent(notificationManager, LEGACY_CHANNEL_ID_TURN_RIGHT_V2);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static void deleteChannelIfPresent(
            @NonNull NotificationManager notificationManager,
            @NonNull String channelId
    ) {
        if (notificationManager.getNotificationChannel(channelId) != null) {
            notificationManager.deleteNotificationChannel(channelId);
        }
    }
}
