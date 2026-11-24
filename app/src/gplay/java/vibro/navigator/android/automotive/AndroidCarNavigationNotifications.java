package vibro.navigator.android.automotive;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;

import androidx.car.app.notification.CarAppExtender;
import androidx.car.app.notification.CarPendingIntent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import vibro.navigator.R;
import vibro.navigator.auto.ViBRoCarAppService;
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.ui.NavigationActivity;

/** Separate ongoing car notification leaves transient phone alerts on their own lifecycle. */
public final class AndroidCarNavigationNotifications {
    public static final int NOTIFICATION_ID = 3;
    private final Context context;
    private final NotificationManager manager;

    public AndroidCarNavigationNotifications(Context context) {
        this.context = context;
        manager = context.getSystemService(NotificationManager.class);
    }

    public void update(NavState state) {
        if (manager == null) {
            return;
        }
        Intent open = new Intent(context, NavigationActivity.class)
                .putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent carIntent = CarPendingIntent.getCarApp(context, NOTIFICATION_ID,
                new Intent(Intent.ACTION_VIEW).setComponent(new ComponentName(context, ViBRoCarAppService.class)),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(context, NavigationService.CHANNEL_ID_NAV)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(AndroidAppLanguage.getString(context, R.string.app_name))
                .setContentText(AndroidAppLanguage.getString(context, R.string.notification_nav_running_text))
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
                .setContentIntent(contentIntent)
                .extend(new CarAppExtender.Builder().setContentIntent(carIntent)
                        .setContentText(state.routeStatus.guidance.nextLine)
                        .setImportance(NotificationManagerCompat.IMPORTANCE_LOW).build()).build();
        manager.notify(NOTIFICATION_ID, notification);
    }

    public void clear() {
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }
}
