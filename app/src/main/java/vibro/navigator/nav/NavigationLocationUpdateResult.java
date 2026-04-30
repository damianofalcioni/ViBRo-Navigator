package vibro.navigator.nav;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

final class NavigationLocationUpdateResult {
    private static final long NO_SUGGESTED_INTERVAL = -1L;

    private final boolean dropped;
    private final boolean shouldRecalculateRoute;
    private final long suggestedUpdateIntervalMs;
    @Nullable
    private final NavigationRerouteNotice rerouteNotice;
    @Nullable
    final Location filteredLocation;
    @NonNull
    final List<NavigationTurnEvent> turnEvents;

    private NavigationLocationUpdateResult(
            boolean dropped,
            boolean shouldRecalculateRoute,
            long suggestedUpdateIntervalMs,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @Nullable Location filteredLocation,
            @NonNull List<NavigationTurnEvent> turnEvents
    ) {
        this.dropped = dropped;
        this.shouldRecalculateRoute = shouldRecalculateRoute;
        this.suggestedUpdateIntervalMs = suggestedUpdateIntervalMs;
        this.rerouteNotice = rerouteNotice;
        this.filteredLocation = filteredLocation;
        this.turnEvents = turnEvents;
    }

    @NonNull
    static NavigationLocationUpdateResult dropped() {
        return new NavigationLocationUpdateResult(
                true,
                false,
                NO_SUGGESTED_INTERVAL,
                null,
                null,
                Collections.emptyList()
        );
    }

    @NonNull
    static NavigationLocationUpdateResult accepted(
            @Nullable Location filteredLocation,
            boolean shouldRecalculateRoute,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @NonNull List<NavigationTurnEvent> turnEvents,
            long suggestedUpdateIntervalMs
    ) {
        return new NavigationLocationUpdateResult(
                false,
                shouldRecalculateRoute,
                suggestedUpdateIntervalMs,
                rerouteNotice,
                filteredLocation,
                turnEvents
        );
    }

    boolean isDropped() {
        return dropped;
    }

    boolean shouldRecalculateRoute() {
        return shouldRecalculateRoute;
    }

    long getSuggestedUpdateIntervalMs() {
        return suggestedUpdateIntervalMs;
    }

    @Nullable
    NavigationRerouteNotice getRerouteNotice() {
        return rerouteNotice;
    }
}
