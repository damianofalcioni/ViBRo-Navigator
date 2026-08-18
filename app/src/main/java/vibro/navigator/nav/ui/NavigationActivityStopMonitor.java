package vibro.navigator.nav.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.service.NavigationServiceBinder;

final class NavigationActivityStopMonitor {
    interface Host {
        @Nullable
        NavigationServiceBinder currentBinder();

        boolean isAutoStartNavigation();

        boolean shouldResumeExistingNavigation();

        boolean isFinishing();

        void render(@NonNull NavState state);

        void finish();
    }

    private static final String TAG = "NavigationActivity";

    @NonNull
    private final TaskScheduler uiScheduler;
    @NonNull
    private final Host host;
    @NonNull
    private final NavigationService.Listener listener = new NavigationService.Listener() {
        @Override
        public void onState(@NonNull NavState state) {
            receivedNavigationState = true;
            uiScheduler.post(() -> host.render(state));
        }

        @Override
        public void onNavigationStopped() {
            uiScheduler.post(() -> finishAfterNavigationStopped("Navigation stopped externally"));
        }
    };

    private boolean receivedNavigationState;

    NavigationActivityStopMonitor(@NonNull TaskScheduler uiScheduler, @NonNull Host host) {
        this.uiScheduler = uiScheduler;
        this.host = host;
    }

    @NonNull
    NavigationService.Listener listener() {
        return listener;
    }

    boolean finishIfBoundServiceHasStopped() {
        NavigationServiceBinder binder = host.currentBinder();
        if (binder == null || binder.isNavigationStarted() || host.isAutoStartNavigation()) {
            return false;
        }
        if (!receivedNavigationState && !host.shouldResumeExistingNavigation()) {
            return false;
        }
        finishAfterNavigationStopped("Navigation service is no longer active");
        return true;
    }

    private void finishAfterNavigationStopped(@NonNull String reason) {
        AppLogger.i(TAG, reason + ", finishing navigation UI");
        if (!host.isFinishing()) {
            host.finish();
        }
    }
}
