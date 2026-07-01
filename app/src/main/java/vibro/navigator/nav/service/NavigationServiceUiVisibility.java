package vibro.navigator.nav.service;


import vibro.navigator.nav.orientation.NavigationOrientationController;
import vibro.navigator.nav.session.NavigationSession;
import androidx.annotation.NonNull;

public final class NavigationServiceUiVisibility implements NavigationOrientationController.CompassUiState {

    private final NavigationSession navigationSession;
    private final NavigationStateBroadcaster stateBroadcaster;
    private final Runnable stateRefresh;
    private final Runnable compassStreetViewportClearer;
    private boolean navigationUiVisible;
    private boolean screenInteractive = true;

    public NavigationServiceUiVisibility(
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationStateBroadcaster stateBroadcaster,
            @NonNull Runnable stateRefresh,
            @NonNull Runnable compassStreetViewportClearer
    ) {
        this.navigationSession = navigationSession;
        this.stateBroadcaster = stateBroadcaster;
        this.stateRefresh = stateRefresh;
        this.compassStreetViewportClearer = compassStreetViewportClearer;
    }

    public void setScreenInteractive(boolean interactive) {
        screenInteractive = interactive;
        clearCompassStreetViewportIfInactive();
    }

    public boolean isScreenInteractive() {
        return screenInteractive;
    }

    public void setNavigationUiVisible(boolean visible) {
        if (navigationUiVisible == visible) {
            return;
        }
        navigationUiVisible = visible;
        clearCompassStreetViewportIfInactive();
        if (visible && screenInteractive) {
            stateRefresh.run();
        }
    }

    public void onScreenInteractiveChanged(boolean interactive) {
        if (screenInteractive == interactive) {
            return;
        }
        screenInteractive = interactive;
        clearCompassStreetViewportIfInactive();
        if (interactive && navigationUiVisible && stateBroadcaster.size() > 0) {
            stateRefresh.run();
        }
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

    private void clearCompassStreetViewportIfInactive() {
        if (!canUseCompassStreetViewport()) {
            compassStreetViewportClearer.run();
        }
    }
}
