package vibro.navigator.nav.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;

/** Applies route-request decisions after a fix, independently of its physical or simulated source. */
final class NavigationServiceLocationRouting {
    interface Actions {
        void request(
                boolean force,
                @Nullable NavigationRerouteNotice rerouteNotice,
                @NonNull NavigationRouteRecalculationReason reason
        );

        void requestSpeculative(@NonNull NavigationRouteRecalculationReason reason);

        boolean confirmSpeculative(@Nullable NavigationRerouteNotice rerouteNotice);

        void cancelSpeculative();
    }


    private final Context context;
    private final Actions routeRecalculator;

    NavigationServiceLocationRouting(Context context, Actions routeRecalculator) {
        this.context = context;
        this.routeRecalculator = routeRecalculator;
    }

    void apply(
            @NonNull NavigationLocationUpdateResult result,
            @NonNull NavigationLocationController locationController,
            @NonNull NavigationForegroundController foregroundController
    ) {
        NavigationRouteRecalculationReason reason = recalculationReason(result);
        if (result.shouldRecalculateRoute()) {
            if (tryConfirmSpeculativeRoute(result, reason)) {
                return;
            }
            routeRecalculator.request(false, result.getRerouteNotice(), reason);
            return;
        }
        applySpeculativeRouteSignal(result, reason);
        if (result.getRerouteNotice() != null) {
            foregroundController.sendOffRouteNotification(result.getRerouteNotice());
        }
        if (result.getSuggestedUpdateIntervalMs() > 0L) {
            requestSuggestedLocationUpdates(result.getSuggestedUpdateIntervalMs(), locationController);
        }
    }

    private boolean tryConfirmSpeculativeRoute(
            @NonNull NavigationLocationUpdateResult result,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        return reason == NavigationRouteRecalculationReason.ROUTE_DEVIATION
                && routeRecalculator.confirmSpeculative(result.getRerouteNotice());
    }

    private void applySpeculativeRouteSignal(
            @NonNull NavigationLocationUpdateResult result,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        if (result.shouldSpeculativelyRecalculateRoute()) {
            routeRecalculator.requestSpeculative(reason);
        } else if (result.shouldCancelSpeculativeRouteRecalculation()) {
            routeRecalculator.cancelSpeculative();
        }
    }

    private void requestSuggestedLocationUpdates(
            long suggestedUpdateIntervalMs,
            @NonNull NavigationLocationController locationController
    ) {
        NavigationLocationUpdateRequestPolicy.requestLocationUpdates(
                context,
                locationController,
                suggestedUpdateIntervalMs
        );
    }

    @NonNull
    private static NavigationRouteRecalculationReason recalculationReason(
            @NonNull NavigationLocationUpdateResult result
    ) {
        NavigationRouteRecalculationReason reason = result.getRecalculationReason();
        return reason == null ? NavigationRouteRecalculationReason.EXPLICIT : reason;
    }

}
