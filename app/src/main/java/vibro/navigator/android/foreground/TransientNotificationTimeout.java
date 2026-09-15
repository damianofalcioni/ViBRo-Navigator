package vibro.navigator.android.foreground;

import android.app.NotificationManager;
import android.app.Service;

import androidx.annotation.NonNull;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.service.NavigationService;

final class TransientNotificationTimeout {

    private static final long TRANSIENT_NOTIFICATION_DURATION_MS = 5_000L;

    @NonNull
    private final TaskScheduler scheduler;
    @NonNull
    private final Runnable removeNotification;
    private Runnable pendingRemoval;
    private long generation;

    static TransientNotificationTimeout forService(@NonNull Service service) {
        return new TransientNotificationTimeout(
                AndroidTaskScheduler.main(),
                () -> cancelNotification(service)
        );
    }

    TransientNotificationTimeout(
            @NonNull TaskScheduler scheduler,
            @NonNull Runnable removeNotification
    ) {
        this.scheduler = scheduler;
        this.removeNotification = removeNotification;
    }

    void schedule() {
        if (pendingRemoval != null) {
            scheduler.removeCallbacks(pendingRemoval);
        }
        final long scheduledGeneration = ++generation;
        Runnable removal = () -> removeIfCurrent(scheduledGeneration);
        pendingRemoval = removal;
        scheduler.postDelayed(removal, TRANSIENT_NOTIFICATION_DURATION_MS);
    }

    private void removeIfCurrent(long scheduledGeneration) {
        if (scheduledGeneration != generation) {
            return;
        }
        pendingRemoval = null;
        removeNotification.run();
    }

    private static void cancelNotification(@NonNull Service service) {
        NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.cancel(NavigationService.NOTIFICATION_ID_TURN);
        }
    }
}
