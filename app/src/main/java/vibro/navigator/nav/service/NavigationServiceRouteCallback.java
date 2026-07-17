package vibro.navigator.nav.service;


import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.orientation.NavigationOrientationController;
import vibro.navigator.nav.routing.NavigationRouteExecutor;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.routing.PendingRouteRecalculation;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionSpeculativeRoutes;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
        if (navigationSession.speculativeRoutes().handleUnconfirmedRouteResult(snapshot, newRoute, beganAt)) {
            stateEmitter.run();
            runQueuedRouteRecalculation("Re-running queued route recalculation after speculative request finished");
            return;
        }
        applyRouteResult(snapshot, newRoute, beganAt);
    }

    @Override
    public void onRouteFailure(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        if (navigationSession.speculativeRoutes().ignoreUnconfirmedRouteFailure(snapshot, error)) {
            stateEmitter.run();
            runQueuedRouteRecalculation("Retrying queued route recalculation after speculative request failed");
            return;
        }
        if (!navigationSession.applyRouteFailure(context, snapshot, error)) {
            return;
        }
        stateEmitter.run();
        runQueuedRouteRecalculation("Retrying queued route recalculation after previous request failed");
    }

    void onSpeculativeRouteConfirmed(
            @Nullable NavigationRerouteNotice rerouteNotice,
            @NonNull NavigationSessionSpeculativeRoutes.Confirmation confirmation
    ) {
        if (rerouteNotice != null) {
            foregroundController.sendOffRouteNotification(rerouteNotice);
        }
        if (confirmation == NavigationSessionSpeculativeRoutes.Confirmation.RESULT_READY) {
            applyConfirmedSpeculativeRouteResult();
        }
    }

    private void applyRouteResult(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt
    ) {
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
        runQueuedRouteRecalculation("Re-running queued route recalculation after previous request finished");
    }

    private void applyConfirmedSpeculativeRouteResult() {
        long routeAppliedAtElapsedMs = routeAppliedLocationRequester.requestFastLocationUpdates();
        turnEventDispatcher.dispatch(navigationSession.speculativeRoutes().applyConfirmedRouteResult(
                context,
                routeAppliedAtElapsedMs
        ));
        orientationController.maybeSendStationaryOrientationNotification(navigationSession, foregroundController);
        stateEmitter.run();
        runQueuedRouteRecalculation("Re-running queued route recalculation after speculative route was confirmed");
    }

    private void runQueuedRouteRecalculation(@NonNull String logMessage) {
        PendingRouteRecalculation pending = navigationSession.consumePendingRouteRecalculation();
        if (pending != null) {
            AppLogger.i(TAG, logMessage);
            routeRecalculator.request(pending);
        }
    }
}
