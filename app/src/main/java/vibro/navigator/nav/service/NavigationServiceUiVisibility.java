package vibro.navigator.nav.service;


import vibro.navigator.nav.orientation.NavigationOrientationController;
import vibro.navigator.nav.session.NavigationSession;
import androidx.annotation.NonNull;

public final class NavigationServiceUiVisibility implements NavigationOrientationController.CompassUiState {

    public interface DisplayActivityListener {
        void onDisplayActivityChanged(boolean active);
    }

    private final NavigationSession navigationSession;
    private final NavigationStateBroadcaster stateBroadcaster;
    private final Runnable stateRefresh;
    private final Runnable compassStreetViewportClearer;
    private final DisplayActivityListener displayActivityListener;
    private boolean navigationUiVisible;
    private boolean screenInteractive = true;
    private boolean displayActive;

    public NavigationServiceUiVisibility(
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationStateBroadcaster stateBroadcaster,
            @NonNull Runnable stateRefresh,
            @NonNull Runnable compassStreetViewportClearer,
            @NonNull DisplayActivityListener displayActivityListener
    ) {
        this.navigationSession = navigationSession;
        this.stateBroadcaster = stateBroadcaster;
        this.stateRefresh = stateRefresh;
        this.compassStreetViewportClearer = compassStreetViewportClearer;
        this.displayActivityListener = displayActivityListener;
    }

    public void setScreenInteractive(boolean interactive) {
        screenInteractive = interactive;
        onDisplayInputsChanged();
    }

    public boolean isScreenInteractive() {
        return screenInteractive;
    }

    public void setNavigationUiVisible(boolean visible) {
        if (navigationUiVisible == visible) {
            return;
        }
        navigationUiVisible = visible;
        onDisplayInputsChanged();
        if (visible && screenInteractive) {
            stateRefresh.run();
        }
    }

    public void onScreenInteractiveChanged(boolean interactive) {
        if (screenInteractive == interactive) {
            return;
        }
        screenInteractive = interactive;
        onDisplayInputsChanged();
        if (interactive && navigationUiVisible && stateBroadcaster.size() > 0) {
            stateRefresh.run();
        }
    }

    public void onStateListenersChanged() {
        onDisplayInputsChanged();
    }

    @Override
    public boolean shouldDispatchCompassUi() {
        return NavigationOrientationController.shouldDispatchCompassUi(
                navigationSession.hasActiveRoute(),
                navigationUiVisible,
                screenInteractive
        );
    }

    @Override
    public boolean hasStateListeners() {
        return stateBroadcaster.size() > 0;
    }

    boolean canUseCompassStreetViewport() {
        return canDispatchStateToUi();
    }

    boolean canDispatchStateToUi() {
        return navigationUiVisible
                && screenInteractive
                && stateBroadcaster.size() > 0;
    }

    @Override
    public void requestStateRefresh() {
        stateRefresh.run();
    }

    private void onDisplayInputsChanged() {
        clearCompassStreetViewportIfInactive();
        notifyDisplayActivityIfChanged();
    }

    private void clearCompassStreetViewportIfInactive() {
        if (!canUseCompassStreetViewport()) {
            compassStreetViewportClearer.run();
        }
    }

    private void notifyDisplayActivityIfChanged() {
        boolean active = canDispatchStateToUi();
        if (displayActive == active) {
            return;
        }
        displayActive = active;
        displayActivityListener.onDisplayActivityChanged(active);
    }
}
