package vibro.navigator.nav.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.foreground.NavigationScreenInteractivityMonitor;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.session.NavigationSession;

final class NavigationServiceRuntime {
    @NonNull
    private final NavigationForegroundController foregroundController;
    @NonNull
    private final NavigationScreenInteractivityMonitor screenInteractivityMonitor;
    private final boolean screenInteractive;
    @NonNull
    private final NavigationTrackingRuntime tracking;
    @NonNull
    private final NavigationRoutingRuntime routing;

    private NavigationServiceRuntime(
            @NonNull NavigationServiceDependencies dependencies
    ) {
        foregroundController = dependencies.foreground.controller;
        screenInteractivityMonitor = dependencies.foreground.screenInteractivityMonitor;
        screenInteractive = dependencies.foreground.screenInteractive;
        tracking = dependencies.tracking;
        routing = dependencies.routing;
    }

    @NonNull
    static NavigationServiceRuntime create(
            @NonNull NavigationService service,
            @NonNull TaskScheduler uiScheduler,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationServiceTurnEvents turnEvents,
            @NonNull NavigationServiceLocationHandler locationHandler,
            @NonNull NavigationServiceUiVisibility uiVisibility,
            @NonNull Runnable stateEmitter,
            @NonNull NavigationServiceRouteCallback.RouteRecalculator routeRecalculator
    ) {
        NavigationServiceDependencies dependencies = NavigationServiceDependencies.create(
                service,
                uiScheduler,
                navigationSession,
                turnEvents,
                locationHandler,
                uiVisibility,
                stateEmitter,
                routeRecalculator
        );
        uiVisibility.setScreenInteractive(dependencies.foreground.screenInteractive);
        return new NavigationServiceRuntime(dependencies);
    }

    @NonNull
    NavigationForegroundController foregroundController() {
        return foregroundController;
    }

    void promoteToForeground(@Nullable NavigationRequest request, boolean paused) {
        foregroundController.promoteToForeground(request, paused);
    }

    void resetTrackingState() {
        tracking.locationController.resetTrackingState();
    }

    void requestLocationUpdates(long intervalMs) {
        tracking.locationController.requestLocationUpdates(intervalMs);
    }

    void requestCurrentLocationSeeds() {
        tracking.locationController.requestCurrentLocationSeeds();
    }

    void startOrientation() {
        tracking.orientationController.start();
    }

    void stopTrackingAndOrientation() {
        tracking.locationController.stopTracking();
        tracking.orientationController.stop();
    }

    void stopForegroundService() {
        foregroundController.stopForegroundService();
    }

    void stopManeuverSpeech() {
        routing.maneuverSpeaker.stop();
    }

    void requestRoute(@NonNull NavigationRouteRequestSnapshot snapshot) {
        routing.executor.requestRoute(snapshot, routing.callback);
    }

    long nextEvaluationDeadlineElapsedMs() {
        return tracking.locationController.getNextEvaluationDeadlineElapsedMs();
    }

    @Nullable
    Integer fixedSatelliteCount() {
        return tracking.locationController.getFixedSatelliteCount();
    }

    @Nullable
    Double displayHeadingDegrees() {
        return tracking.orientationController.currentDisplayHeadingDegrees();
    }

    @Nullable
    Float displayHeadingAccuracyDegrees() {
        return tracking.orientationController.currentDisplayHeadingAccuracyDegrees();
    }

    @Nullable
    CompassOrientationCue activeOrientationCue() {
        return tracking.orientationController.activeOrientationCue();
    }

    long lastRequestedLocationMinTimeMsOrDefault(long defaultIntervalMs) {
        return tracking.locationController.getLastRequestedLocationMinTimeMsOrDefault(defaultIntervalMs);
    }

    void stopScreenInteractivityMonitor() {
        screenInteractivityMonitor.stop();
    }

    void shutdownRouteExecutor() {
        routing.executor.shutdown();
    }

    void shutdownManeuverSpeaker() {
        routing.maneuverSpeaker.shutdown();
    }

    boolean isScreenInteractive() {
        return screenInteractive;
    }
}
