package vibro.navigator.nav.service;


import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationFormatter;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.orientation.NavigationOrientationController;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

public final class NavigationServiceLocationHandler implements NavigationLocationListener {

    public interface CurrentLocationSeedPolicy {
        boolean isCurrentLocationSeedAllowed();
    }

    public interface RouteRecalculator {
        void request(
                boolean force,
                @Nullable NavigationRerouteNotice rerouteNotice,
                @NonNull NavigationRouteRecalculationReason reason
        );
    }

    public interface SurroundingStreetLocationSink {
        void onAcceptedLocation(@NonNull NavigationLocation location);
    }

    private static final String TAG = "NavigationService";
    private static final long DEFAULT_LOCATION_UPDATE_INTERVAL_MS =
            NavigationLocationController.DEFAULT_UPDATE_INTERVAL_MS;

    private final Context context;
    private final NavigationSession navigationSession;
    private final NavigationServiceTurnEvents turnEvents;
    private final RouteRecalculator routeRecalculator;
    private final CurrentLocationSeedPolicy currentLocationSeedPolicy;
    private final SurroundingStreetLocationSink surroundingStreetLocationSink;
    private final Runnable stateEmitter;
    private final ElapsedRealtimeClock elapsedRealtimeClock = AndroidElapsedRealtimeClock.INSTANCE;
    @Nullable
    private NavigationLocationController locationController;
    @Nullable
    private NavigationOrientationController orientationController;
    @Nullable
    private NavigationForegroundController foregroundController;

    public NavigationServiceLocationHandler(
            @NonNull Context context,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationServiceTurnEvents turnEvents,
            @NonNull RouteRecalculator routeRecalculator,
            @NonNull CurrentLocationSeedPolicy currentLocationSeedPolicy,
            @NonNull SurroundingStreetLocationSink surroundingStreetLocationSink,
            @NonNull Runnable stateEmitter
    ) {
        this.context = context;
        this.navigationSession = navigationSession;
        this.turnEvents = turnEvents;
        this.routeRecalculator = routeRecalculator;
        this.currentLocationSeedPolicy = currentLocationSeedPolicy;
        this.surroundingStreetLocationSink = surroundingStreetLocationSink;
        this.stateEmitter = stateEmitter;
    }

    public void attachControllers(
            @NonNull NavigationLocationController locationController,
            @NonNull NavigationOrientationController orientationController,
            @NonNull NavigationForegroundController foregroundController
    ) {
        this.locationController = locationController;
        this.orientationController = orientationController;
        this.foregroundController = foregroundController;
    }

    public void onScreenInteractiveChanged(boolean interactive) {
        NavigationLocationController controller = locationController;
        if (!interactive && controller != null) {
            controller.cancelCurrentLocationSeeds();
        }
    }

    public void seedStartupLocation(long nowMs) {
        NavigationLocationController controller = locationController;
        if (controller == null) {
            return;
        }
        NavigationLocation seed = controller.getBestStartupLastKnownLocation(nowMs);
        if (seed != null) {
            AppLogger.i(TAG, "Using last known location as seed " + NavigationLocationFormatter.format(seed));
            onLocationChanged(seed);
        } else {
            AppLogger.w(TAG, "No usable cached location available at navigation start "
                    + controller.describeAvailability());
        }
    }

    @Override
    public void onLocationChanged(@NonNull NavigationLocation location) {
        Controllers controllers = controllers();
        if (controllers == null) {
            return;
        }
        if (navigationSession.isPaused()) {
            AppLogger.d(TAG, "Ignoring location update while navigation is paused");
            return;
        }
        long expectedUpdateIntervalMs = controllers.locationController
                .getLastRequestedLocationMinTimeMsOrDefault(DEFAULT_LOCATION_UPDATE_INTERVAL_MS);
        NavigationLocationUpdateResult result =
                navigationSession.onRawLocationChanged(
                        context,
                        location,
                        elapsedRealtimeClock.elapsedRealtimeMs(),
                        expectedUpdateIntervalMs
                );
        if (result.isDropped()) {
            return;
        }
        controllers.locationController.recordAcceptedLocationUpdate();
        surroundingStreetLocationSink.onAcceptedLocation(location);
        applyRouteUpdateRequest(result, controllers);
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

    @Nullable
    private Controllers controllers() {
        NavigationLocationController location = locationController;
        NavigationOrientationController orientation = orientationController;
        NavigationForegroundController foreground = foregroundController;
        return location == null || orientation == null || foreground == null
                ? null
                : new Controllers(location, orientation, foreground);
    }

    private void applyRouteUpdateRequest(
            @NonNull NavigationLocationUpdateResult result,
            @NonNull Controllers controllers
    ) {
        if (result.shouldRecalculateRoute()) {
            routeRecalculator.request(false, result.getRerouteNotice(), recalculationReason(result));
            return;
        }
        if (result.getRerouteNotice() != null) {
            controllers.foregroundController.sendOffRouteNotification(result.getRerouteNotice());
        }
        if (result.getSuggestedUpdateIntervalMs() > 0L) {
            requestSuggestedLocationUpdates(result.getSuggestedUpdateIntervalMs(), controllers.locationController);
        }
    }

    private static void requestSuggestedLocationUpdates(
            long suggestedUpdateIntervalMs,
            @NonNull NavigationLocationController locationController
    ) {
        if (suggestedUpdateIntervalMs == NavigationLocationController.STARTUP_UPDATE_INTERVAL_MS) {
            locationController.requestStartupLocationUpdates();
            return;
        }
        locationController.requestLocationUpdates(suggestedUpdateIntervalMs);
    }

    @NonNull
    private static NavigationRouteRecalculationReason recalculationReason(
            @NonNull NavigationLocationUpdateResult result
    ) {
        NavigationRouteRecalculationReason reason = result.getRecalculationReason();
        return reason == null ? NavigationRouteRecalculationReason.EXPLICIT : reason;
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        NavigationLocationController controller = locationController;
        if (controller == null) {
            return;
        }
        if (navigationSession.isPaused()) {
            AppLogger.d(TAG, "Ignoring provider enabled while navigation is paused provider=" + provider);
            return;
        }
        AppLogger.i(TAG, "Location provider enabled provider=" + provider);
        controller.onProviderEnabled(
                provider,
                DEFAULT_LOCATION_UPDATE_INTERVAL_MS,
                currentLocationSeedPolicy.isCurrentLocationSeedAllowed()
        );
        stateEmitter.run();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        NavigationLocationController controller = locationController;
        if (controller == null) {
            return;
        }
        if (navigationSession.isPaused()) {
            AppLogger.d(TAG, "Ignoring provider disabled while navigation is paused provider=" + provider);
            return;
        }
        AppLogger.w(TAG, "Location provider disabled provider=" + provider);
        navigationSession.onProviderDisabled(provider);
        controller.requestLocationUpdates(
                controller.getLastRequestedLocationMinTimeMsOrDefault(DEFAULT_LOCATION_UPDATE_INTERVAL_MS)
        );
        stateEmitter.run();
    }

    @Override
    public void onProviderStatusChanged(@Nullable String provider, int status) {
        AppLogger.d(TAG, "Location provider status changed provider=" + provider + " status=" + status);
    }

    private static final class Controllers {
        @NonNull
        public final NavigationLocationController locationController;
        @NonNull
        public final NavigationOrientationController orientationController;
        @NonNull
        public final NavigationForegroundController foregroundController;

        Controllers(
                @NonNull NavigationLocationController locationController,
                @NonNull NavigationOrientationController orientationController,
                @NonNull NavigationForegroundController foregroundController
        ) {
            this.locationController = locationController;
            this.orientationController = orientationController;
            this.foregroundController = foregroundController;
        }
    }
}
