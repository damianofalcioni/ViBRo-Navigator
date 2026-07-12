package vibro.navigator.nav.service;


import vibro.navigator.nav.foreground.NavigationForegroundCoordinator;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionResourceAdapter;
import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.brouter.NogoPoint;

public final class NavigationServiceBinderHost implements NavigationServiceBinder.Host {

    public interface BlockedRoadRouteRecalculator {
        void request(@NonNull String inProgressNotice);
    }

    public interface CompassStreetViewportSink {
        void set(@Nullable NavCompassState compassState);
    }

    private final Context context;
    private final NavigationStateBroadcaster stateBroadcaster;
    private final NavigationForegroundCoordinator foregroundCoordinator;
    private final NavigationSession navigationSession;
    private final NavigationServiceUiVisibility uiVisibility;
    private final Runnable stateEmitter;
    private final CompassStreetViewportSink compassStreetViewportSink;
    private final Runnable locationUpdateSettingsRefresher;
    private final BlockedRoadRouteRecalculator blockedRoadRouteRecalculator;
    private final Runnable stopNavigationAndService;
    private final Runnable pauseNavigation;
    private final Runnable resumeNavigation;
    private final ElapsedRealtimeClock elapsedRealtimeClock = AndroidElapsedRealtimeClock.INSTANCE;

    public NavigationServiceBinderHost(
            @NonNull Context context,
            @NonNull NavigationStateBroadcaster stateBroadcaster,
            @NonNull NavigationForegroundCoordinator foregroundCoordinator,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationServiceUiVisibility uiVisibility,
            @NonNull Runnable stateEmitter,
            @NonNull CompassStreetViewportSink compassStreetViewportSink,
            @NonNull Runnable locationUpdateSettingsRefresher,
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
        this.compassStreetViewportSink = compassStreetViewportSink;
        this.locationUpdateSettingsRefresher = locationUpdateSettingsRefresher;
        this.blockedRoadRouteRecalculator = blockedRoadRouteRecalculator;
        this.stopNavigationAndService = stopNavigationAndService;
        this.pauseNavigation = pauseNavigation;
        this.resumeNavigation = resumeNavigation;
    }

    @Override
    public void registerListener(@NonNull NavigationService.Listener listener) {
        stateBroadcaster.register(listener);
        uiVisibility.onStateListenersChanged();
    }

    @Override
    public void unregisterListener(@NonNull NavigationService.Listener listener) {
        stateBroadcaster.unregister(listener);
        uiVisibility.onStateListenersChanged();
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
    public void setCompassStreetViewport(@Nullable NavCompassState compassState) {
        compassStreetViewportSink.set(uiVisibility.canUseCompassStreetViewport() ? compassState : null);
    }

    @Override
    public void refreshLocationUpdateSettings() {
        if (navigationSession.isStarted() && !navigationSession.isPaused()) {
            locationUpdateSettingsRefresher.run();
        }
    }

    @Override
    public boolean isNavigationPaused() {
        return navigationSession.isPaused();
    }

    @Override
    @Nullable
    public NavigationLocation getLastFilteredLocation() {
        return navigationSession.getLastFilteredLocation();
    }

    @Override
    @NonNull
    public List<NogoPoint> addBlockedPointsAhead() {
        return navigationSession.addBlockedPointsAhead(elapsedRealtimeClock.elapsedRealtimeMs());
    }

    @Override
    public boolean canAddBlockedWaypoint() {
        return navigationSession.canAddBlockedWaypoint();
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

    @Override
    @Nullable
    public String buildCurrentRouteGpx() {
        return navigationSession.buildCurrentRouteGpx(context);
    }

    @Override
    @NonNull
    public List<String> buildCurrentDirectionDetails() {
        return NavigationSessionResourceAdapter.buildCurrentDirectionDetails(
                navigationSession,
                new AndroidNavigationTextResources(context)
        );
    }
}
