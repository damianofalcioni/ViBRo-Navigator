package vibro.navigator.auto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.service.NavigationServiceBinder;

final class ViBRoCarNavigationActions {
    interface BinderProvider {
        @Nullable
        NavigationServiceBinder currentBinder();
    }

    @NonNull
    private final ViBRoCarNavigationController.Host host;
    @NonNull
    private final BinderProvider binderProvider;

    ViBRoCarNavigationActions(
            @NonNull ViBRoCarNavigationController.Host host,
            @NonNull BinderProvider binderProvider
    ) {
        this.host = host;
        this.binderProvider = binderProvider;
    }

    void setCompassStreetViewport(@Nullable NavCompassState compassState) {
        NavigationServiceBinder binder = binderProvider.currentBinder();
        if (binder != null) {
            binder.setCompassStreetViewport(compassState);
        }
    }

    void refreshLocationUpdateSettings() {
        NavigationServiceBinder binder = binderProvider.currentBinder();
        if (binder != null) {
            binder.refreshLocationUpdateSettings();
        }
    }

    void addBlockedWaypoint() {
        NavigationServiceBinder binder = binderForAction("Blocked-road");
        if (binder == null) {
            return;
        }
        if (!binder.canAddBlockedWaypoint()) {
            AppLogger.w(
                    ViBRoCarNavigationController.TAG,
                    "Blocked-road requested while its current action is unavailable"
            );
            return;
        }
        binder.addBlockedWaypoint();
    }

    void togglePaused() {
        NavigationServiceBinder binder = binderForAction("Pause/resume");
        if (binder == null) {
            return;
        }
        if (binder.isPaused()) {
            binder.resume();
        } else {
            binder.pause();
        }
    }

    void stopNavigation() {
        NavigationServiceBinder binder = binderForAction("Stop");
        if (binder == null) {
            return;
        }
        binder.stop();
        host.updateCurrentState(null);
    }

    @Nullable
    private NavigationServiceBinder binderForAction(@NonNull String actionName) {
        NavigationServiceBinder binder = binderProvider.currentBinder();
        if (binder != null) {
            return binder;
        }
        AppLogger.w(
                ViBRoCarNavigationController.TAG,
                actionName + " requested before service binding completed"
        );
        return null;
    }


}
