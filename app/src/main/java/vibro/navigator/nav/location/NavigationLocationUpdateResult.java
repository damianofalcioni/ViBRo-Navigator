package vibro.navigator.nav.location;


import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public final class NavigationLocationUpdateResult {
    private static final long NO_SUGGESTED_INTERVAL = -1L;

    private final boolean dropped;
    private final boolean shouldRecalculateRoute;
    private final long suggestedUpdateIntervalMs;
    @Nullable
    private final NavigationRerouteNotice rerouteNotice;
    @Nullable
    private final NavigationRouteRecalculationReason recalculationReason;
    @Nullable
    private final NavigationWrongDirectionNotice wrongDirectionNotice;
    @Nullable
    public final NavigationLocation filteredLocation;
    @NonNull
    public final List<NavigationTurnEvent> turnEvents;

    private NavigationLocationUpdateResult(
            boolean dropped,
            boolean shouldRecalculateRoute,
            long suggestedUpdateIntervalMs,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @Nullable NavigationRouteRecalculationReason recalculationReason,
            @Nullable NavigationWrongDirectionNotice wrongDirectionNotice,
            @Nullable NavigationLocation filteredLocation,
            @NonNull List<NavigationTurnEvent> turnEvents
    ) {
        this.dropped = dropped;
        this.shouldRecalculateRoute = shouldRecalculateRoute;
        this.suggestedUpdateIntervalMs = suggestedUpdateIntervalMs;
        this.rerouteNotice = rerouteNotice;
        this.recalculationReason = recalculationReason;
        this.wrongDirectionNotice = wrongDirectionNotice;
        this.filteredLocation = filteredLocation;
        this.turnEvents = turnEvents;
    }

    @NonNull
    public static NavigationLocationUpdateResult dropped() {
        return new NavigationLocationUpdateResult(
                true,
                false,
                NO_SUGGESTED_INTERVAL,
                null,
                null,
                null,
                null,
                Collections.emptyList()
        );
    }

    @NonNull
    public static NavigationLocationUpdateResult accepted(
            @Nullable NavigationLocation filteredLocation,
            boolean shouldRecalculateRoute,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @NonNull List<NavigationTurnEvent> turnEvents,
            long suggestedUpdateIntervalMs
    ) {
        return accepted(
                filteredLocation,
                shouldRecalculateRoute,
                rerouteNotice,
                null,
                null,
                turnEvents,
                suggestedUpdateIntervalMs
        );
    }

    @NonNull
    public static NavigationLocationUpdateResult accepted(
            @Nullable NavigationLocation filteredLocation,
            boolean shouldRecalculateRoute,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @Nullable NavigationRouteRecalculationReason recalculationReason,
            @NonNull List<NavigationTurnEvent> turnEvents,
            long suggestedUpdateIntervalMs
    ) {
        return accepted(
                filteredLocation,
                shouldRecalculateRoute,
                rerouteNotice,
                recalculationReason,
                null,
                turnEvents,
                suggestedUpdateIntervalMs
        );
    }

    @NonNull
    public static NavigationLocationUpdateResult accepted(
            @Nullable NavigationLocation filteredLocation,
            boolean shouldRecalculateRoute,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @Nullable NavigationRouteRecalculationReason recalculationReason,
            @Nullable NavigationWrongDirectionNotice wrongDirectionNotice,
            @NonNull List<NavigationTurnEvent> turnEvents,
            long suggestedUpdateIntervalMs
    ) {
        return new NavigationLocationUpdateResult(
                false,
                shouldRecalculateRoute,
                suggestedUpdateIntervalMs,
                rerouteNotice,
                recalculationReason,
                wrongDirectionNotice,
                filteredLocation,
                turnEvents
        );
    }

    public boolean isDropped() {
        return dropped;
    }

    public boolean shouldRecalculateRoute() {
        return shouldRecalculateRoute;
    }

    public long getSuggestedUpdateIntervalMs() {
        return suggestedUpdateIntervalMs;
    }

    @Nullable
    public NavigationRerouteNotice getRerouteNotice() {
        return rerouteNotice;
    }

    @Nullable
    public NavigationRouteRecalculationReason getRecalculationReason() {
        return recalculationReason;
    }

    @Nullable
    public NavigationWrongDirectionNotice getWrongDirectionNotice() {
        return wrongDirectionNotice;
    }
}
