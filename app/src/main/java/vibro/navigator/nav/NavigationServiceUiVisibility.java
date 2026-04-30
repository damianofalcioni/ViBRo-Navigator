package vibro.navigator.nav;

import androidx.annotation.NonNull;

final class NavigationServiceUiVisibility implements NavigationOrientationController.CompassUiState {

    private final NavigationSession navigationSession;
    private final NavigationStateBroadcaster stateBroadcaster;
    private final Runnable stateRefresh;
    private boolean navigationUiVisible;
    private boolean screenInteractive = true;

    NavigationServiceUiVisibility(
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationStateBroadcaster stateBroadcaster,
            @NonNull Runnable stateRefresh
    ) {
        this.navigationSession = navigationSession;
        this.stateBroadcaster = stateBroadcaster;
        this.stateRefresh = stateRefresh;
    }

    void setScreenInteractive(boolean interactive) {
        screenInteractive = interactive;
    }

    void setNavigationUiVisible(boolean visible) {
        if (navigationUiVisible == visible) {
            return;
        }
        navigationUiVisible = visible;
        if (visible && screenInteractive) {
            stateRefresh.run();
        }
    }

    void onScreenInteractiveChanged(boolean interactive) {
        if (screenInteractive == interactive) {
            return;
        }
        screenInteractive = interactive;
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

    @Override
    public void requestStateRefresh() {
        stateRefresh.run();
    }
}
