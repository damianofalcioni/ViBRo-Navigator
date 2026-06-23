package vibro.navigator.nav.service;


import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.orientation.NavigationOrientationController;
import vibro.navigator.nav.routing.NavigationRouteExecutor;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.routing.PendingRouteRecalculation;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.session.NavigationSession;
import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.logging.AppLogger;

import java.util.List;

public final class NavigationServiceRouteCallback implements NavigationRouteExecutor.Callback {

    public interface TurnEventDispatcher {
        void dispatch(@NonNull List<NavigationTurnEvent> turnEvents);
    }

    public interface RouteRecalculator {
        void request(@NonNull PendingRouteRecalculation pendingRecalculation);
    }

    public interface RouteAppliedLocationRequester {
        long requestFastLocationUpdates();
    }

    private static final String TAG = "NavigationService";

    private final Context context;
    private final NavigationSession navigationSession;
    private final NavigationOrientationController orientationController;
    private final NavigationForegroundController foregroundController;
    private final TurnEventDispatcher turnEventDispatcher;
    private final RouteAppliedLocationRequester routeAppliedLocationRequester;
    private final Runnable stateEmitter;
    private final RouteRecalculator routeRecalculator;

    public NavigationServiceRouteCallback(
            @NonNull Context context,
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationOrientationController orientationController,
            @NonNull NavigationForegroundController foregroundController,
            @NonNull TurnEventDispatcher turnEventDispatcher,
            @NonNull RouteAppliedLocationRequester routeAppliedLocationRequester,
            @NonNull Runnable stateEmitter,
            @NonNull RouteRecalculator routeRecalculator
    ) {
        this.context = context;
        this.navigationSession = navigationSession;
        this.orientationController = orientationController;
        this.foregroundController = foregroundController;
        this.turnEventDispatcher = turnEventDispatcher;
        this.routeAppliedLocationRequester = routeAppliedLocationRequester;
        this.stateEmitter = stateEmitter;
        this.routeRecalculator = routeRecalculator;
    }

    @Override
    public void onRouteApplied(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt
    ) {
        if (!navigationSession.isCurrentRouteRequest(snapshot)) {
            return;
        }
        long routeAppliedAtElapsedMs = routeAppliedLocationRequester.requestFastLocationUpdates();
        turnEventDispatcher.dispatch(navigationSession.applyRouteResult(
                context,
                snapshot,
                newRoute,
                beganAt,
                routeAppliedAtElapsedMs
        ));
        orientationController.maybeSendStationaryOrientationNotification(navigationSession, foregroundController);
        stateEmitter.run();
        PendingRouteRecalculation pending = navigationSession.consumePendingRouteRecalculation();
        if (pending != null) {
            AppLogger.i(TAG, "Re-running queued route recalculation after previous request finished");
            routeRecalculator.request(pending);
        }
    }

    @Override
    public void onRouteFailure(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        if (!navigationSession.applyRouteFailure(context, snapshot, error)) {
            return;
        }
        stateEmitter.run();
        PendingRouteRecalculation pending = navigationSession.consumePendingRouteRecalculation();
        if (pending != null) {
            AppLogger.i(TAG, "Retrying queued route recalculation after previous request failed");
            routeRecalculator.request(pending);
        }
    }
}
