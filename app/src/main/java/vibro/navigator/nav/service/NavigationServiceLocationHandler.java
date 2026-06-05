package vibro.navigator.nav.service;


import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationFormatter;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.orientation.NavigationOrientationController;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.nav.session.NavigationSession;
import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

public final class NavigationServiceLocationHandler implements NavigationLocationListener {

    public interface RouteRecalculator {
        void request(
                boolean force,
                @Nullable NavigationRerouteNotice rerouteNotice,
                @NonNull NavigationRouteRecalculationReason reason
        );
    }

    private static final String TAG = "NavigationService";
    private static final long DEFAULT_LOCATION_UPDATE_INTERVAL_MS = 1_000L;

    private final Context context;
    private final NavigationSession navigationSession;
    private final NavigationServiceTurnEvents turnEvents;
    private final RouteRecalculator routeRecalculator;
    private final Runnable stateEmitter;
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
            @NonNull Runnable stateEmitter
    ) {
        this.context = context;
        this.navigationSession = navigationSession;
        this.turnEvents = turnEvents;
        this.routeRecalculator = routeRecalculator;
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
        NavigationLocationUpdateResult result =
                navigationSession.onRawLocationChanged(context, location, System.currentTimeMillis());
        if (result.isDropped()) {
            return;
        }
        controllers.locationController.recordAcceptedLocationUpdate();
        applyRouteUpdateRequest(result, controllers.locationController);
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
            @NonNull NavigationLocationController controller
    ) {
        if (result.shouldRecalculateRoute()) {
            routeRecalculator.request(false, result.getRerouteNotice(), recalculationReason(result));
            return;
        }
        if (result.getSuggestedUpdateIntervalMs() > 0L) {
            controller.requestLocationUpdates(result.getSuggestedUpdateIntervalMs());
        }
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
        controller.onProviderEnabled(provider, DEFAULT_LOCATION_UPDATE_INTERVAL_MS);
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
