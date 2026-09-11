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
    private final NavigationBeelineRecoveryRequests recoveryRequests;

    NavigationSessionSpeculativeRoutes(@NonNull NavigationSession session) {
        this.session = session;
        recoveryRequests = new NavigationBeelineRecoveryRequests(session);
    }

    @Nullable
    public NavigationRouteRequestSnapshot prepareRequest(
            boolean force,
            long nowMs,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        if (reason == NavigationRouteRecalculationReason.BEELINE_RECOVERY && !recoveryRequests.canPrepare(nowMs)) {
            return null;
        }
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
        if (snapshot != null) {
            recoveryRequests.clear();
            if (reason == NavigationRouteRecalculationReason.BEELINE_RECOVERY) {
                recoveryRequests.remember(snapshot, nowMs);
            }
        }
        return snapshot;
    }

    public boolean handleUnconfirmedRouteResult(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt,
            long nowMs
    ) {
        if (recoveryRequests.isFor(snapshot)) {
            return handleRecoveryResult(snapshot, newRoute, nowMs);
        }
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
        if (recoveryRequests.isFor(snapshot)) {
            recoveryRequests.clear();
        }
        return NavigationSessionResourceAdapter.ignoreUnconfirmedSpeculativeRouteFailure(
                session,
                snapshot,
                error
        );
    }

    @NonNull
    public Confirmation confirmRecalculation() {
        if (recoveryRequests.isActive()) {
            cancelRecalculation();
            return Confirmation.NONE;
        }
        return session.components.speculativeRouteState.confirm();
    }

    public boolean cancelRecalculation() {
        recoveryRequests.clear();
        boolean canceled = session.components.speculativeRouteState.cancelUnconfirmed();
        boolean requestCanceled = session.components.routeRequestManager.cancelActiveSpeculativeRequest();
        if (canceled || requestCanceled) {
            AppLogger.i(TAG, "Canceled unconfirmed speculative route recalculation");
        }
        return canceled || requestCanceled;
    }

    /** Recovery requests are hidden background work until a road-backed result is accepted. */
    public boolean isBackgroundRecoveryRequest(@NonNull NavigationRouteRequestSnapshot snapshot) {
        return recoveryRequests.isFor(snapshot);
    }

    private boolean handleRecoveryResult(NavigationRouteRequestSnapshot snapshot, GeoJsonRoute route, long nowMs) {
        boolean usable = recoveryRequests.accepts(snapshot, route, nowMs);
        recoveryRequests.clear();
        if (usable) {
            AppLogger.i(TAG, "Applying usable speculative beeline recovery route");
            return false;
        }
        session.components.routeRequestManager.onSpeculativeRouteFinished(snapshot, false);
        session.components.speculativeRouteState.onRouteFailed(snapshot);
        AppLogger.i(TAG, "Keeping current beeline after unsuitable or obsolete recovery result");
        return true;
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
