package vibro.navigator.nav.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.power.AndroidBatterySnapshotReader;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.foreground.NavigationScreenInteractivityMonitor;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationStallMonitor;
import vibro.navigator.android.location.AndroidNavigationLocationRecoveryAlarm;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

final class NavigationServiceRuntime {
    @NonNull
    private final Context context;
    @NonNull
    private final NavigationForegroundController foregroundController;
    @NonNull
    private final NavigationScreenInteractivityMonitor screenInteractivityMonitor;
    @NonNull
    private final NavigationTrackingRuntime tracking;
    @NonNull
    private final NavigationRoutingRuntime routing;
    @NonNull
    private final NavigationServiceStreetOverlay streetOverlay;
    @NonNull
    final NavigationServiceLocationRecovery locationRecovery;
    @NonNull
    final AndroidBatterySnapshotReader batterySnapshotReader;

    NavigationServiceRuntime(
            @NonNull Context context,
            @NonNull NavigationServiceDependencies dependencies,
            @NonNull NavigationServiceStreetOverlay streetOverlay
    ) {
        this.context = context;
        foregroundController = dependencies.foreground.controller;
        screenInteractivityMonitor = dependencies.foreground.screenInteractivityMonitor;
        tracking = dependencies.tracking;
        routing = dependencies.routing;
        this.streetOverlay = streetOverlay;
        locationRecovery = new NavigationServiceLocationRecovery(
                tracking.locationController,
                new NavigationLocationStallMonitor(),
                new AndroidNavigationLocationRecoveryAlarm(
                        context,
                        NavigationService.class,
                        NavigationService.ACTION_RECOVER_LOCATION
                ),
                AndroidElapsedRealtimeClock.INSTANCE
        );
        batterySnapshotReader = new AndroidBatterySnapshotReader(context);
    }

    @NonNull
    NavigationForegroundController foregroundController() {
        return foregroundController;
    }

    void promoteToForeground(@NonNull NavigationRequest request, boolean paused) {
        foregroundController.promoteToForeground(request, paused);
    }

    void resetTrackingState() {
        tracking.locationController.resetTrackingState();
    }

    void resetStreetOverlay() {
        streetOverlay.reset();
    }

    void onAcceptedLocation(@NonNull NavigationLocation location) {
        streetOverlay.onAcceptedLocation(location);
        locationRecovery.onAcceptedFix();
    }

    void onCompassStreetViewport(@Nullable NavCompassState compassState) {
        if (compassState == null) {
            streetOverlay.clearCompassViewport();
            return;
        }
        streetOverlay.onCompassViewport(compassState);
    }

    @NonNull
    NavState attachStreetOverlay(@NonNull NavState state) {
        return streetOverlay.attach(state);
    }

    void requestLocationUpdates(long intervalMs) {
        NavigationLocationUpdateRequestPolicy.requestLocationUpdates(
                context,
                tracking.locationController,
                intervalMs
        );
    }

    void requestCurrentLocationSeeds() {
        tracking.locationController.requestCurrentLocationSeeds();
    }

    void setGnssStatusTrackingAllowed(boolean allowed) {
        tracking.locationController.setGnssStatusTrackingAllowed(allowed);
    }

    void startOrientation() {
        tracking.orientationController.start();
    }

    void stopTrackingAndOrientation() {
        locationRecovery.stop();
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

    long elapsedRealtimeMs() {
        return AndroidElapsedRealtimeClock.INSTANCE.elapsedRealtimeMs();
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

    void shutdown() {
        routing.executor.shutdown();
        streetOverlay.shutdown();
        routing.maneuverSpeaker.shutdown();
        batterySnapshotReader.stop();
    }

}
