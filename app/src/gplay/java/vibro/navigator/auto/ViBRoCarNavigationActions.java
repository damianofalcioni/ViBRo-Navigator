package vibro.navigator.auto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.service.NavigationServiceBinder;

final class ViBRoCarNavigationActions {
    interface BinderProvider {
        @Nullable
        NavigationServiceBinder currentBinder();
    }

    @NonNull
    private final CarContext carContext;
    @NonNull
    private final ViBRoCarNavigationController.Host host;
    @NonNull
    private final BinderProvider binderProvider;

    ViBRoCarNavigationActions(
            @NonNull CarContext carContext,
            @NonNull ViBRoCarNavigationController.Host host,
            @NonNull BinderProvider binderProvider
    ) {
        this.carContext = carContext;
        this.host = host;
        this.binderProvider = binderProvider;
    }

    void setCompassStreetViewport(@Nullable NavCompassState compassState) {
        NavigationServiceBinder binder = binderProvider.currentBinder();
        if (binder != null) {
            binder.setCompassStreetViewport(compassState);
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
                    "Blocked-road requested while blocked-road rerouting is unavailable"
            );
            return;
        }
        binder.addBlockedWaypoint();
        NavState state = host.currentState();
        if (state != null) {
            host.updateCurrentState(NavStateComposer.withBlockedRoadActionAvailable(state, false));
        }
    }

    void togglePaused() {
        NavigationServiceBinder binder = binderForAction("Pause/resume");
        if (binder == null) {
            return;
        }
        if (binder.isPaused()) {
            binder.resume();
            updatePauseState(false);
        } else {
            binder.pause();
            updatePauseState(true);
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

    void openPhoneNavigationIfActive() {
        NavigationServiceBinder binder = binderProvider.currentBinder();
        if (binder != null && binder.isNavigationStarted()) {
            ViBRoAutoPhoneLauncher.openNavigation(carContext);
        }
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

    private void updatePauseState(boolean paused) {
        NavState state = host.currentState();
        if (state != null) {
            host.updateCurrentState(NavStateComposer.withPauseState(carContext, state, paused));
        }
    }
}
