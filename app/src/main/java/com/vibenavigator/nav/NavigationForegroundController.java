package com.vibenavigator.nav;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.vibenavigator.MainActivity;
import com.vibenavigator.NavigationActivity;
import com.vibenavigator.R;
import com.vibenavigator.nav.directions.DirectionInfo;
import com.vibenavigator.nav.directions.DirectionKind;
import com.vibenavigator.nav.directions.VoiceHintMapper;
import com.vibenavigator.nav.route.VoiceHint;
import com.vibenavigator.util.AppLogger;

final class NavigationForegroundController {

    private static final String TAG = "NavForeground";
    private static final String LEGACY_CHANNEL_ID_TURN_LEFT = "vibenavigator.turn.left";
    private static final String LEGACY_CHANNEL_ID_TURN_RIGHT = "vibenavigator.turn.right";

    private final Service service;

    NavigationForegroundController(@NonNull Service service) {
        this.service = service;
    }

    void ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            AppLogger.w(TAG, "NotificationManager unavailable while creating channels");
            return;
        }

        deleteObsoleteChannels(notificationManager);

        NotificationChannel navChannel = new NotificationChannel(
                NavigationService.CHANNEL_ID_NAV,
                service.getString(R.string.notification_channel_nav),
                NotificationManager.IMPORTANCE_LOW
        );
        navChannel.enableVibration(false);
        navChannel.setSound(null, null);
        notificationManager.createNotificationChannel(navChannel);

        NotificationChannel alertChannel = new NotificationChannel(
                NavigationService.CHANNEL_ID_ALERT,
                service.getString(R.string.notification_channel_alert),
                NotificationManager.IMPORTANCE_HIGH
        );
        alertChannel.enableVibration(true);
        alertChannel.setVibrationPattern(genericAlertVibrationPattern());
        alertChannel.setSound(null, null);
        notificationManager.createNotificationChannel(alertChannel);

        NotificationChannel leftChannel = new NotificationChannel(
                NavigationService.CHANNEL_ID_TURN_LEFT,
                service.getString(R.string.notification_channel_turn_left),
                NotificationManager.IMPORTANCE_HIGH
        );
        leftChannel.enableVibration(true);
        leftChannel.setVibrationPattern(leftVibrationPattern());
        leftChannel.setSound(null, null);
        notificationManager.createNotificationChannel(leftChannel);

        NotificationChannel rightChannel = new NotificationChannel(
                NavigationService.CHANNEL_ID_TURN_RIGHT,
                service.getString(R.string.notification_channel_turn_right),
                NotificationManager.IMPORTANCE_HIGH
        );
        rightChannel.enableVibration(true);
        rightChannel.setVibrationPattern(rightVibrationPattern());
        rightChannel.setSound(null, null);
        notificationManager.createNotificationChannel(rightChannel);
        AppLogger.i(TAG, "Notification channels ensured");
    }

    void promoteToForeground(@NonNull NavigationRequest request, boolean paused) {
        service.startForeground(
                NavigationService.NOTIFICATION_ID_ONGOING,
                buildOngoingNotification(request, paused)
        );
    }

    void stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE);
        } else {
            //noinspection deprecation
            service.stopForeground(true);
        }
    }

    boolean isOngoingNotificationVisible() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            AppLogger.w(TAG, "NotificationManager unavailable while checking foreground notification");
            return true;
        }
        try {
            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
            for (StatusBarNotification notification : notifications) {
                if (notification.getId() == NavigationService.NOTIFICATION_ID_ONGOING) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to query active notifications", e);
            return true;
        }
    }

    void sendImminentTurnNotification(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
        DirectionInfo directionInfo = VoiceHintMapper.toDirection(hint);
        String channelId = directionInfo.kind == DirectionKind.LEFT
                ? NavigationService.CHANNEL_ID_TURN_LEFT
                : (directionInfo.kind == DirectionKind.RIGHT
                ? NavigationService.CHANNEL_ID_TURN_RIGHT
                : NavigationService.CHANNEL_ID_ALERT);
        sendTurnNotification(hint, distanceMeters, timeSeconds, channelId);
    }

    void sendStationaryOrientationNotification(@NonNull StationaryOrientationAdvisor.Decision decision) {
        String channelId = decision.turnRight()
                ? NavigationService.CHANNEL_ID_TURN_RIGHT
                : NavigationService.CHANNEL_ID_TURN_LEFT;
        String message = NavigationTextFormatter.formatStationaryOrientationNotification(service, decision);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(service.getString(R.string.app_name))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setVibrate(decision.turnRight() ? rightVibrationPattern() : leftVibrationPattern());
        }

        NotificationManager notificationManager = (NotificationManager) service.getSystemService(Service.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            AppLogger.w(TAG, "NotificationManager unavailable, cannot send stationary orientation notification");
            return;
        }
        notificationManager.notify(NavigationService.NOTIFICATION_ID_TURN, builder.build());
        AppLogger.i(TAG, "Sent stationary orientation notification message=" + message);
    }

    void sendOffRouteNotification(@NonNull NavigationRerouteNotice rerouteNotice) {
        String title = service.getString(R.string.notification_off_route_title);
        String message = NavigationTextFormatter.formatOffRouteNotification(service, rerouteNotice);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, NavigationService.CHANNEL_ID_ALERT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setVibrate(genericAlertVibrationPattern());
        }

        NotificationManager notificationManager =
                (NotificationManager) service.getSystemService(Service.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            AppLogger.w(TAG, "NotificationManager unavailable, cannot send off-route notification");
            return;
        }
        notificationManager.notify(NavigationService.NOTIFICATION_ID_TURN, builder.build());
        AppLogger.i(TAG, "Sent off-route notification reason=" + rerouteNotice.reason + " message=" + message);
    }

    private void deleteObsoleteChannels(@NonNull NotificationManager notificationManager) {
        deleteChannelIfPresent(notificationManager, LEGACY_CHANNEL_ID_TURN_LEFT);
        deleteChannelIfPresent(notificationManager, LEGACY_CHANNEL_ID_TURN_RIGHT);
    }

    private void deleteChannelIfPresent(
            @NonNull NotificationManager notificationManager,
            @NonNull String channelId
    ) {
        if (notificationManager.getNotificationChannel(channelId) != null) {
            notificationManager.deleteNotificationChannel(channelId);
        }
    }

    @NonNull
    private Notification buildOngoingNotification(@NonNull NavigationRequest request, boolean paused) {
        Intent stopNavigationIntent = new Intent(service, NavigationService.class);
        stopNavigationIntent.setAction(NavigationService.ACTION_STOP);
        PendingIntent stopNavigationPendingIntent = PendingIntent.getService(
                service,
                1,
                stopNavigationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent openNavigationIntent = new Intent(service, MainActivity.class);
        openNavigationIntent.putExtra(MainActivity.EXTRA_OPEN_NAVIGATION, true);
        openNavigationIntent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        openNavigationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        request.putInto(openNavigationIntent);
        PendingIntent openNavigationPendingIntent = PendingIntent.getActivity(
                service,
                0,
                openNavigationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(service, NavigationService.CHANNEL_ID_NAV)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(service.getString(
                        paused ? R.string.notification_nav_paused : R.string.notification_nav_running
                ))
                .setContentText(service.getString(
                        paused ? R.string.notification_nav_paused_text : R.string.notification_nav_running_text
                ))
                .setOngoing(true)
                .setDeleteIntent(stopNavigationPendingIntent)
                .setContentIntent(openNavigationPendingIntent)
                .build();
    }

    private void sendTurnNotification(
            @NonNull VoiceHint hint,
            double distanceMeters,
            double timeSeconds,
            @NonNull String channelId
    ) {
        DirectionInfo directionInfo = VoiceHintMapper.toDirection(hint);
        String message = NavigationTextFormatter.formatTurnNotification(service, hint, distanceMeters, timeSeconds);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(service.getString(R.string.app_name))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (directionInfo.kind == DirectionKind.LEFT) {
                builder.setVibrate(leftVibrationPattern());
            } else if (directionInfo.kind == DirectionKind.RIGHT) {
                builder.setVibrate(rightVibrationPattern());
            } else {
                builder.setVibrate(genericAlertVibrationPattern());
            }
        }

        NotificationManager notificationManager = (NotificationManager) service.getSystemService(Service.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            AppLogger.w(TAG, "NotificationManager unavailable, cannot send turn notification");
            return;
        }
        notificationManager.notify(NavigationService.NOTIFICATION_ID_TURN, builder.build());
        AppLogger.d(TAG, "Sent turn notification channel=" + channelId
                + " notificationId=" + NavigationService.NOTIFICATION_ID_TURN
                + " message=" + message);
    }

    @NonNull
    private static long[] genericAlertVibrationPattern() {
        return new long[]{0, 140, 90, 140};
    }

    @NonNull
    private static long[] leftVibrationPattern() {
        return new long[]{0, 80, 80, 220};
    }

    @NonNull
    private static long[] rightVibrationPattern() {
        return new long[]{0, 220, 80, 80};
    }
}
