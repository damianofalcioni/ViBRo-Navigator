package vibro.navigator.nav.service;

import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationFormatter;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionContextResources;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.logging.NavigationEvaluationTrace;
import vibro.navigator.android.power.AndroidNavigationBatteryLogger;

public final class NavigationServiceLocationHandler implements NavigationLocationListener {

    public interface CurrentLocationSeedPolicy {
        boolean isCurrentLocationSeedAllowed();
    }

    public interface RouteRecalculator extends NavigationServiceLocationRouting.Actions { }

    public interface AcceptedLocationSink {
        void onAcceptedLocation(@NonNull NavigationLocation raw, @Nullable NavigationLocation filtered,
                boolean stationarySleepEligible);
    }

    private static final String TAG = "NavigationService";
    private static final long DEFAULT_LOCATION_UPDATE_INTERVAL_MS =
            NavigationLocationController.DEFAULT_UPDATE_INTERVAL_MS;

    private final Context context;
    private final NavigationSession navigationSession;
    private final NavigationServiceTurnEvents turnEvents;
    private final NavigationServiceLocationRouting routing;
    private final CurrentLocationSeedPolicy currentLocationSeedPolicy;
    private final AcceptedLocationSink acceptedLocationSink;
    private final Runnable stateEmitter;
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    private final AndroidNavigationBatteryLogger batteryLogger;
    @Nullable
    private NavigationServiceLocationControllers controllers;
    private final NavigationServiceSimulation simulation;

    public NavigationServiceLocationHandler(
            @NonNull Context context,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationServiceTurnEvents turnEvents,
            @NonNull RouteRecalculator routeRecalculator,
            @NonNull CurrentLocationSeedPolicy currentLocationSeedPolicy,
            @NonNull AcceptedLocationSink acceptedLocationSink,
            @NonNull Runnable stateEmitter,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.context = context;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        this.navigationSession = navigationSession;
        this.turnEvents = turnEvents;
        routing = new NavigationServiceLocationRouting(context, routeRecalculator);
        this.currentLocationSeedPolicy = currentLocationSeedPolicy;
        this.acceptedLocationSink = acceptedLocationSink;
        this.stateEmitter = stateEmitter;
        batteryLogger = new AndroidNavigationBatteryLogger(context);
        simulation = new NavigationServiceSimulation(context, navigationSession, elapsedRealtimeClock,
                location -> handleLocation(location, false));
    }

    NavigationServiceSimulation simulation() { return simulation; }

    void attachControllers(NavigationServiceLocationControllers controllers) {
        this.controllers = controllers;
    }

    AndroidNavigationBatteryLogger batteryLogger() {
        return batteryLogger;
    }

    public void onScreenInteractiveChanged(boolean interactive) {
        NavigationServiceLocationControllers controllers = this.controllers;
        if (!interactive && !currentLocationSeedPolicy.isCurrentLocationSeedAllowed() && controllers != null) {
            controllers.locationController.cancelCurrentLocationSeeds();
        }
    }

    public void seedStartupLocation(long nowMs) {
        NavigationServiceLocationControllers controllers = this.controllers;
        if (controllers == null) {
            return;
        }
        NavigationLocationController controller = controllers.locationController;
        NavigationLocation seed = controller.getBestStartupLastKnownLocation(nowMs);
        if (seed != null) {
            AppLogger.i(TAG, "Using last known location as seed " + NavigationLocationFormatter.format(seed));
            handleLocation(seed, true);
        } else {
            AppLogger.w(TAG, "No usable cached location available at navigation start "
                    + controller.describeAvailability());
        }
    }

    @Override
    public void onLocationChanged(@NonNull NavigationLocation location) {
        if (simulation.ignorePhysicalProviders() && !currentLocationSeedPolicy.isCurrentLocationSeedAllowed()) {
            return;
        }
        NavigationServiceLocationControllers controllers = this.controllers;
        if (simulation.acceptPhysical(location, accepted -> handleLocation(accepted, false)) && controllers != null) {
            controllers.locationController.finishSimulationOrigin();
        }
    }

    private void handleLocation(@NonNull NavigationLocation location, boolean startupSeed) {
        NavigationEvaluationTrace.run(location.getProvider(), location.getElapsedRealtimeOrTimeMs(),
                () -> evaluateLocation(location, startupSeed));
    }

    private void evaluateLocation(@NonNull NavigationLocation location, boolean startupSeed) {
        AppLogger.d(TAG, "Location evaluation received elapsedRealtimeMs=" + elapsedRealtimeClock.elapsedRealtimeMs()
                + " uptimeMs=" + elapsedRealtimeClock.uptimeMs());
        NavigationServiceLocationControllers controllers = this.controllers;
        if (controllers == null) {
            return;
        }
        if (controllers.locationController.isStationarySuspended()) {
            AppLogger.d(TAG, "Ignoring late location update during stationary sleep");
            return;
        }
        if (navigationSession.isPaused()) {
            AppLogger.d(TAG, "Ignoring location update while navigation is paused");
            return;
        }
        long expectedUpdateIntervalMs = controllers.locationController
                .getLastRequestedLocationMinTimeMsOrDefault(DEFAULT_LOCATION_UPDATE_INTERVAL_MS);
        long nowMs = elapsedRealtimeClock.elapsedRealtimeMs();
        controllers.locationController.acquisition().recordRawLocation(location);
        navigationSession.setActiveLocationProviders(controllers.locationController.activeProviderSummary());
        batteryLogger.record(nowMs, controllers.locationController.activeProviderSummary(), expectedUpdateIntervalMs);
        NavigationLocationUpdateResult result =
                startupSeed ? NavigationSessionContextResources.onStartupLocationSeed(
                        navigationSession, context, location, nowMs, expectedUpdateIntervalMs)
                : navigationSession.onRawLocationChanged(
                        context,
                        location,
                        nowMs,
                        expectedUpdateIntervalMs
                );
        if (result.isDropped()) {
            return;
        }
        controllers.locationController.recordAcceptedLocationUpdate();
        routing.apply(result, controllers.locationController, controllers.foregroundController);
        acceptedLocationSink.onAcceptedLocation(location, navigationSession.getLastFilteredLocation(),
                NavigationLocationUpdateRequestPolicy.canSleep(context, navigationSession));
        if (result.getWrongDirectionNotice() != null) {
            controllers.foregroundController.sendWrongDirectionNotification(result.getWrongDirectionNotice());
        }
        turnEvents.dispatch(result.turnEvents);
        controllers.orientationController.maybeSendStationaryOrientationNotification(
                navigationSession,
                controllers.foregroundController
        );
        stateEmitter.run();
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        if (simulation.ignorePhysicalProviders()) { return; }
        NavigationServiceLocationControllers controllers = this.controllers;
        if (controllers == null) {
            return;
        }
        NavigationLocationController controller = controllers.locationController;
        if (navigationSession.isPaused()) {
            AppLogger.d(TAG, "Ignoring provider enabled while navigation is paused provider=" + provider);
            return;
        }
        AppLogger.i(TAG, "Location provider enabled provider=" + provider);
        NavigationLocationUpdateRequestPolicy.requestProviderEnabledUpdates(
                context,
                controller,
                provider,
                currentLocationSeedPolicy.isCurrentLocationSeedAllowed()
        );
        stateEmitter.run();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        if (simulation.ignorePhysicalProviders()) { return; }
        NavigationServiceLocationControllers controllers = this.controllers;
        if (controllers == null) {
            return;
        }
        NavigationLocationController controller = controllers.locationController;
        if (navigationSession.isPaused()) {
            AppLogger.d(TAG, "Ignoring provider disabled while navigation is paused provider=" + provider);
            return;
        }
        AppLogger.w(TAG, "Location provider disabled provider=" + provider);
        navigationSession.onProviderDisabled(provider);
        NavigationLocationUpdateRequestPolicy.requestLocationUpdates(
                context,
                controller,
                controller.getLastRequestedLocationMinTimeMsOrDefault(DEFAULT_LOCATION_UPDATE_INTERVAL_MS)
        );
        stateEmitter.run();
    }

    @Override
    public void onProviderStatusChanged(@Nullable String provider, int status) {
        // The Android adapter emits transitions only; repeated statuses do not reach logging.
        AppLogger.d(TAG, "Location provider status changed provider=" + provider + " status=" + status);
    }

}
