package vibro.navigator.nav;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.util.AppLogger;

import java.util.List;

final class NavigationServiceRouteCallback implements NavigationRouteExecutor.Callback {

    interface TurnEventDispatcher {
        void dispatch(@NonNull List<NavigationTurnEvent> turnEvents);
    }

    interface RouteRecalculator {
        void request(boolean force, @Nullable NavigationRerouteNotice rerouteNotice);
    }

    private static final String TAG = "NavigationService";

    private final Context context;
    private final NavigationSession navigationSession;
    private final NavigationOrientationController orientationController;
    private final NavigationForegroundController foregroundController;
    private final TurnEventDispatcher turnEventDispatcher;
    private final Runnable stateEmitter;
    private final RouteRecalculator routeRecalculator;

    NavigationServiceRouteCallback(
            @NonNull Context context,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationOrientationController orientationController,
            @NonNull NavigationForegroundController foregroundController,
            @NonNull TurnEventDispatcher turnEventDispatcher,
            @NonNull Runnable stateEmitter,
            @NonNull RouteRecalculator routeRecalculator
    ) {
        this.context = context;
        this.navigationSession = navigationSession;
        this.orientationController = orientationController;
        this.foregroundController = foregroundController;
        this.turnEventDispatcher = turnEventDispatcher;
        this.stateEmitter = stateEmitter;
        this.routeRecalculator = routeRecalculator;
    }

    @Override
    public void onRouteApplied(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt
    ) {
        turnEventDispatcher.dispatch(navigationSession.applyRouteResult(context, snapshot, newRoute, beganAt));
        orientationController.maybeSendStationaryOrientationNotification(navigationSession, foregroundController);
        stateEmitter.run();
        if (navigationSession.consumePendingRouteRecalculation()) {
            AppLogger.i(TAG, "Re-running queued route recalculation after previous request finished");
            routeRecalculator.request(true, null);
        }
    }

    @Override
    public void onRouteFailure(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        navigationSession.applyRouteFailure(context, snapshot, error);
        stateEmitter.run();
        if (navigationSession.consumePendingRouteRecalculation()) {
            AppLogger.i(TAG, "Retrying queued route recalculation after previous request failed");
            routeRecalculator.request(true, null);
        }
    }
}
