package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;

public final class NavigationRouteEvaluation {
    private static final long NO_SUGGESTED_INTERVAL = -1L;
    private static final long DEVIATION_CONFIRMATION_INTERVAL_MS = 1_000L;

    private final boolean shouldRecalculateRoute;
    private final boolean stableOnRouteSample;
    private final long suggestedUpdateIntervalMs;
    @Nullable
    final NavigationRouteRecalculationReason recalculationReason;
    @Nullable
    final NavigationRerouteNotice rerouteNotice;
    @Nullable
    final NavigationWrongDirectionNotice wrongDirectionNotice;
    @NonNull
    final List<NavigationTurnEvent> turnEvents;

    private NavigationRouteEvaluation(
            boolean shouldRecalculateRoute,
            boolean stableOnRouteSample,
            long suggestedUpdateIntervalMs,
            @Nullable NavigationRouteRecalculationReason recalculationReason,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @Nullable NavigationWrongDirectionNotice wrongDirectionNotice,
            @NonNull List<NavigationTurnEvent> turnEvents
    ) {
        this.shouldRecalculateRoute = shouldRecalculateRoute;
        this.stableOnRouteSample = stableOnRouteSample;
        this.suggestedUpdateIntervalMs = suggestedUpdateIntervalMs;
        this.recalculationReason = recalculationReason;
        this.rerouteNotice = rerouteNotice;
        this.wrongDirectionNotice = wrongDirectionNotice;
        this.turnEvents = turnEvents;
    }

    @NonNull
    public static NavigationRouteEvaluation requestRecalculation(@Nullable NavigationRerouteNotice rerouteNotice) {
        NavigationRouteRecalculationReason reason = rerouteNotice == null
                ? NavigationRouteRecalculationReason.EXPLICIT
                : NavigationRouteRecalculationReason.ROUTE_DEVIATION;
        return requestRecalculation(rerouteNotice, reason);
    }

    @NonNull
    public static NavigationRouteEvaluation requestRecalculation(
            @Nullable NavigationRerouteNotice rerouteNotice,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        return new NavigationRouteEvaluation(
                true,
                false,
                NO_SUGGESTED_INTERVAL,
                reason,
                rerouteNotice,
                null,
                Collections.emptyList()
        );
    }

    @NonNull
    public static NavigationRouteEvaluation keepRoute(
            @NonNull List<NavigationTurnEvent> turnEvents,
            long suggestedUpdateIntervalMs,
            boolean stableOnRouteSample
    ) {
        return keepRoute(turnEvents, suggestedUpdateIntervalMs, stableOnRouteSample, null);
    }

    @NonNull
    public static NavigationRouteEvaluation keepRoute(
            @NonNull List<NavigationTurnEvent> turnEvents,
            long suggestedUpdateIntervalMs,
            boolean stableOnRouteSample,
            @Nullable NavigationWrongDirectionNotice wrongDirectionNotice
    ) {
        return new NavigationRouteEvaluation(
                false,
                stableOnRouteSample,
                suggestedUpdateIntervalMs,
                null,
                null,
                wrongDirectionNotice,
                turnEvents
        );
    }

    public boolean shouldRecalculateRoute() {
        return shouldRecalculateRoute;
    }

    @NonNull
    public static NavigationRouteEvaluation waitForDeviationConfirmation() {
        return new NavigationRouteEvaluation(
                false,
                false,
                DEVIATION_CONFIRMATION_INTERVAL_MS,
                NavigationRouteRecalculationReason.ROUTE_DEVIATION,
                null,
                null,
                Collections.emptyList()
        );
    }

    @NonNull
    NavigationRouteEvaluation asNotificationOnly() {
        if (rerouteNotice == null || !shouldRecalculateRoute) {
            return this;
        }
        return new NavigationRouteEvaluation(
                false,
                stableOnRouteSample,
                suggestedUpdateIntervalMs,
                null,
                rerouteNotice.asNotificationOnly(),
                wrongDirectionNotice,
                turnEvents
        );
    }

    public boolean isStableOnRouteSample() {
        return stableOnRouteSample;
    }

    public long getSuggestedUpdateIntervalMs() {
        return suggestedUpdateIntervalMs;
    }

    public boolean isRouteDeviationConfirmationPending() {
        return !shouldRecalculateRoute
                && recalculationReason == NavigationRouteRecalculationReason.ROUTE_DEVIATION;
    }

    @Nullable
    public NavigationWrongDirectionNotice getWrongDirectionNotice() {
        return wrongDirectionNotice;
    }
}
