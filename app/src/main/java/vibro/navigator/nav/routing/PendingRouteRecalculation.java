package vibro.navigator.nav.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class PendingRouteRecalculation {
    public final boolean force;
    @NonNull
    public final NavigationRouteRecalculationReason reason;
    @Nullable
    public final String inProgressNotice;

    PendingRouteRecalculation(
            boolean force,
            @NonNull NavigationRouteRecalculationReason reason,
            @Nullable String inProgressNotice
    ) {
        this.force = force;
        this.reason = reason;
        this.inProgressNotice = inProgressNotice;
    }

    @NonNull
    PendingRouteRecalculation merge(@NonNull PendingRouteRecalculation next) {
        String mergedNotice = next.inProgressNotice != null ? next.inProgressNotice : inProgressNotice;
        NavigationRouteRecalculationReason mergedReason = chooseReason(reason, next.reason, mergedNotice);
        return new PendingRouteRecalculation(force || next.force, mergedReason, mergedNotice);
    }

    @NonNull
    private static NavigationRouteRecalculationReason chooseReason(
            @NonNull NavigationRouteRecalculationReason current,
            @NonNull NavigationRouteRecalculationReason next,
            @Nullable String mergedNotice
    ) {
        if (mergedNotice != null && !isStartupReason(next)) {
            return next;
        }
        if (isStartupReason(current) && !isStartupReason(next)) {
            return next;
        }
        if (current == NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
                && next == NavigationRouteRecalculationReason.STARTUP_ROUTE_REFRESH) {
            return next;
        }
        return current;
    }

    private static boolean isStartupReason(@NonNull NavigationRouteRecalculationReason reason) {
        return reason == NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
                || reason == NavigationRouteRecalculationReason.STARTUP_ROUTE_REFRESH;
    }
}
