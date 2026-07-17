package vibro.navigator.nav.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationLocation;

final class NavigationPendingRouteRecalculations {
    private static final String TAG = "NavRouteRequests";

    @Nullable
    private PendingRouteRecalculation pendingRecalculation;

    void reset() {
        pendingRecalculation = null;
    }

    @Nullable
    PendingRouteRecalculation consume() {
        PendingRouteRecalculation queued = pendingRecalculation;
        pendingRecalculation = null;
        return queued;
    }

    void update(
            boolean force,
            @Nullable NavigationLocation latestStart,
            @NonNull NavigationRouteRecalculationReason activeRequestReason,
            @Nullable NavigationLocation activeRequestStartLocation,
            @NonNull NavigationRouteRecalculationReason reason,
            @Nullable String inProgressNotice
    ) {
        PendingRouteRecalculation next = new PendingRouteRecalculation(
                true,
                reason,
                RouteRecalculationNotice.sanitize(inProgressNotice)
        );
        if (pendingRecalculation != null) {
            pendingRecalculation = pendingRecalculation.merge(next);
            AppLogger.d(TAG, "Route recalculation already queued while previous request is still running");
            return;
        }
        if (!shouldQueue(force, activeRequestReason, reason, activeRequestStartLocation, latestStart)) {
            AppLogger.i(TAG, "Skipped queued startup route recalculation because latest fix does not "
                    + "materially improve route start distance="
                    + StartupRouteRefreshPolicy.distanceMeters(activeRequestStartLocation, latestStart));
            return;
        }
        pendingRecalculation = next;
        AppLogger.d(TAG, "Queued route recalculation while previous request is still running");
    }

    private static boolean shouldQueue(
            boolean force,
            @NonNull NavigationRouteRecalculationReason activeRequestReason,
            @NonNull NavigationRouteRecalculationReason reason,
            @Nullable NavigationLocation activeRequestStartLocation,
            @Nullable NavigationLocation latestStart
    ) {
        return StartupRouteRefreshPolicy.shouldQueuePending(
                force,
                activeRequestReason,
                reason,
                activeRequestStartLocation,
                latestStart
        );
    }
}
