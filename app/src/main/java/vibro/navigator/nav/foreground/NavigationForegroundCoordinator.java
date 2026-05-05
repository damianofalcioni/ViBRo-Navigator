package vibro.navigator.nav.foreground;


import vibro.navigator.nav.policy.NavigationLifecyclePolicy;
import android.os.Handler;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;

public final class NavigationForegroundCoordinator {

    public interface Host {
        boolean isOngoingNotificationVisible();

        void promoteToForeground();

        void stopNavigation();

        void stopSelf();
    }

    private static final String TAG = "NavForegroundCoord";

    private final Handler handler;
    private final NavigationLifecyclePolicy lifecyclePolicy;
    private final long notificationCheckIntervalMs;
    private final Host host;
    private final Runnable notificationMonitor = new Runnable() {
        @Override
        public void run() {
            NavigationLifecyclePolicy.ForegroundAction action =
                    lifecyclePolicy.onForegroundNotificationCheck(host.isOngoingNotificationVisible());
            if (action == NavigationLifecyclePolicy.ForegroundAction.STOP_NAVIGATION) {
                AppLogger.w(TAG, "Foreground notification is missing, stopping navigation");
                host.stopNavigation();
                host.stopSelf();
                return;
            }
            handler.postDelayed(this, notificationCheckIntervalMs);
        }
    };

    public NavigationForegroundCoordinator(
            @NonNull Handler handler,
            @NonNull NavigationLifecyclePolicy lifecyclePolicy,
            long notificationCheckIntervalMs,
            @NonNull Host host
    ) {
        this.handler = handler;
        this.lifecyclePolicy = lifecyclePolicy;
        this.notificationCheckIntervalMs = notificationCheckIntervalMs;
        this.host = host;
    }

    public void onNavigationUiConnected() {
        NavigationLifecyclePolicy.ForegroundAction action =
                lifecyclePolicy.onNavigationUiConnected(host.isOngoingNotificationVisible());
        if (action == NavigationLifecyclePolicy.ForegroundAction.PROMOTE_TO_FOREGROUND) {
            AppLogger.i(TAG, "Foreground notification refresh requested through binder");
            host.promoteToForeground();
        }
    }

    public void startMonitoring() {
        stopMonitoring();
        handler.postDelayed(notificationMonitor, notificationCheckIntervalMs);
    }

    public void stopMonitoring() {
        handler.removeCallbacks(notificationMonitor);
    }

    public boolean shouldStopOnTaskRemoved() {
        return lifecyclePolicy.onTaskRemoved() == NavigationLifecyclePolicy.TaskRemovedAction.STOP_NAVIGATION;
    }
}
