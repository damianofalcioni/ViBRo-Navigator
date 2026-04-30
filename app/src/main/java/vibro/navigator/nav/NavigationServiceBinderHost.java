package vibro.navigator.nav;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

final class NavigationServiceBinderHost implements NavigationServiceBinder.Host {

    interface BlockedRoadRouteRecalculator {
        void request(@NonNull String inProgressNotice);
    }

    private final Context context;
    private final NavigationStateBroadcaster stateBroadcaster;
    private final NavigationForegroundCoordinator foregroundCoordinator;
    private final NavigationSession navigationSession;
    private final NavigationServiceUiVisibility uiVisibility;
    private final Runnable stateEmitter;
    private final BlockedRoadRouteRecalculator blockedRoadRouteRecalculator;
    private final Runnable stopNavigationAndService;
    private final Runnable pauseNavigation;
    private final Runnable resumeNavigation;

    NavigationServiceBinderHost(
            @NonNull Context context,
            @NonNull NavigationStateBroadcaster stateBroadcaster,
            @NonNull NavigationForegroundCoordinator foregroundCoordinator,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationServiceUiVisibility uiVisibility,
            @NonNull Runnable stateEmitter,
            @NonNull BlockedRoadRouteRecalculator blockedRoadRouteRecalculator,
            @NonNull Runnable stopNavigationAndService,
            @NonNull Runnable pauseNavigation,
            @NonNull Runnable resumeNavigation
    ) {
        this.context = context;
        this.stateBroadcaster = stateBroadcaster;
        this.foregroundCoordinator = foregroundCoordinator;
        this.navigationSession = navigationSession;
        this.uiVisibility = uiVisibility;
        this.stateEmitter = stateEmitter;
        this.blockedRoadRouteRecalculator = blockedRoadRouteRecalculator;
        this.stopNavigationAndService = stopNavigationAndService;
        this.pauseNavigation = pauseNavigation;
        this.resumeNavigation = resumeNavigation;
    }

    @Override
    public void registerListener(@NonNull NavigationService.Listener listener) {
        stateBroadcaster.register(listener);
    }

    @Override
    public void unregisterListener(@NonNull NavigationService.Listener listener) {
        stateBroadcaster.unregister(listener);
    }

    @Override
    public int listenerCount() {
        return stateBroadcaster.size();
    }

    @Override
    public void emitState() {
        stateEmitter.run();
    }

    @Override
    public void ensureForegroundNotification() {
        foregroundCoordinator.onNavigationUiConnected();
    }

    @Override
    public void setNavigationUiVisible(boolean visible) {
        uiVisibility.setNavigationUiVisible(visible);
    }

    @Override
    public boolean isNavigationPaused() {
        return navigationSession.isPaused();
    }

    @Override
    @Nullable
    public Location getLastFilteredLocation() {
        return navigationSession.getLastFilteredLocation();
    }

    @Override
    @NonNull
    public List<?> addBlockedPointsAhead() {
        return navigationSession.addBlockedPointsAhead();
    }

    @Override
    @NonNull
    public String getString(int resId) {
        return context.getString(resId);
    }

    @Override
    public void requestBlockedRoadRouteRecalculation(@NonNull String inProgressNotice) {
        blockedRoadRouteRecalculator.request(inProgressNotice);
    }

    @Override
    public void stopNavigationAndService() {
        stopNavigationAndService.run();
    }

    @Override
    public void pauseNavigation() {
        pauseNavigation.run();
    }

    @Override
    public void resumeNavigation() {
        resumeNavigation.run();
    }
}
