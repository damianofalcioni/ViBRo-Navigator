package vibro.navigator.nav.session;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

public final class NavigationSessionSpeculativeRoutes {
    private static final String TAG = "NavigationSession";

    public enum Confirmation {
        NONE,
        IN_PROGRESS,
        RESULT_READY
    }

    @NonNull
    private final NavigationSession session;

    NavigationSessionSpeculativeRoutes(@NonNull NavigationSession session) {
        this.session = session;
    }

    @Nullable
    public NavigationRouteRequestSnapshot prepareRequest(
            boolean force,
            long nowMs,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        NavigationRouteRequestSnapshot snapshot = session.components.routeRequestManager.prepare(
                force,
                nowMs,
                session.currentRequest,
                session.components.routeState.remainingIntermediateStops(session.currentRequest.stops),
                session.components.locationState.getLastFilteredLocation(),
                session.components.routeState.copyBlockedPoints(),
                null,
                reason,
                true
        );
        session.components.speculativeRouteState.onRouteRequestPrepared(snapshot);
        return snapshot;
    }

    public boolean handleUnconfirmedRouteResult(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt
    ) {
        return NavigationSessionResourceAdapter.handleUnconfirmedSpeculativeRouteResult(
                session,
                snapshot,
                newRoute,
                beganAt
        );
    }

    public boolean ignoreUnconfirmedRouteFailure(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        return NavigationSessionResourceAdapter.ignoreUnconfirmedSpeculativeRouteFailure(
                session,
                snapshot,
                error
        );
    }

    @NonNull
    public Confirmation confirmRecalculation() {
        return session.components.speculativeRouteState.confirm();
    }

    public boolean cancelRecalculation() {
        boolean canceled = session.components.speculativeRouteState.cancelUnconfirmed();
        boolean requestCanceled = session.components.routeRequestManager.cancelActiveSpeculativeRequest();
        if (canceled || requestCanceled) {
            AppLogger.i(TAG, "Canceled unconfirmed speculative route recalculation");
        }
        return canceled || requestCanceled;
    }

    @NonNull
    public List<NavigationTurnEvent> applyConfirmedRouteResult(
            @NonNull Context context,
            long routeAppliedAtElapsedMs
    ) {
        return NavigationSessionResourceAdapter.applyConfirmedSpeculativeRouteResult(
                session,
                new AndroidNavigationTextResources(context),
                routeAppliedAtElapsedMs,
                NavigationSession.isSingleInstructionModeEnabled(context)
        );
    }
}
