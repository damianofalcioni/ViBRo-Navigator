package vibro.navigator.nav.foreground;


import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.intent.NavigationRequestIntentContract;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.format.NavigationTextFormatter;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import vibro.navigator.main.MainActivity;
import vibro.navigator.nav.ui.NavigationActivity;
import vibro.navigator.R;
import vibro.navigator.nav.directions.DirectionInfo;
import vibro.navigator.nav.directions.DirectionKind;
import vibro.navigator.nav.directions.VoiceHintMapper;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.logging.AppLogger;

public final class NavigationForegroundController {

    private static final String TAG = "NavForeground";

    private final Service service;

    public NavigationForegroundController(@NonNull Service service) {
        this.service = service;
    }

    public void ensureChannels() {
        NavigationNotificationChannels.ensure(service);
    }

    @SuppressWarnings("deprecation")
    public void promoteToForeground(@NonNull NavigationRequest request, boolean paused) {
        Notification notification = buildOngoingNotification(request, paused);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            service.startForeground(
                    NavigationService.NOTIFICATION_ID_ONGOING,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            );
            return;
        }
        service.startForeground(NavigationService.NOTIFICATION_ID_ONGOING, notification);
    }

    @SuppressWarnings("deprecation")
    public void stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE);
        } else {
            //noinspection deprecation
            service.stopForeground(true);
        }
    }

    public boolean isOngoingNotificationVisible() {
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

    public void sendImminentTurnNotification(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
        DirectionInfo directionInfo = VoiceHintMapper.toDirection(hint);
        String channelId = directionInfo.kind == DirectionKind.LEFT
                ? NavigationService.CHANNEL_ID_TURN_LEFT
                : (directionInfo.kind == DirectionKind.RIGHT
                ? NavigationService.CHANNEL_ID_TURN_RIGHT
                : NavigationService.CHANNEL_ID_ALERT);
        sendTurnNotification(hint, distanceMeters, timeSeconds, channelId);
    }

    public void sendStationaryOrientationNotification(@NonNull StationaryOrientationAdvisor.Decision decision) {
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
            builder.setVibrate(decision.turnRight()
                    ? NavigationNotificationChannels.rightVibrationPattern()
                    : NavigationNotificationChannels.leftVibrationPattern());
        }

        NotificationManager notificationManager = (NotificationManager) service.getSystemService(Service.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            AppLogger.w(TAG, "NotificationManager unavailable, cannot send stationary orientation notification");
            return;
        }
        notificationManager.notify(NavigationService.NOTIFICATION_ID_TURN, builder.build());
        AppLogger.i(TAG, "Sent stationary orientation notification message=" + message);
    }

    public void sendOffRouteNotification(@NonNull NavigationRerouteNotice rerouteNotice) {
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
            builder.setVibrate(NavigationNotificationChannels.genericAlertVibrationPattern());
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
        NavigationRequestIntentContract.putInto(openNavigationIntent, request);
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
                builder.setVibrate(NavigationNotificationChannels.leftVibrationPattern());
            } else if (directionInfo.kind == DirectionKind.RIGHT) {
                builder.setVibrate(NavigationNotificationChannels.rightVibrationPattern());
            } else {
                builder.setVibrate(NavigationNotificationChannels.genericAlertVibrationPattern());
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

}
