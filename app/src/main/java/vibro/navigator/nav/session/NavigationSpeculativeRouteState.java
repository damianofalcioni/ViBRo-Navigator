package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

final class NavigationSpeculativeRouteState {
    private static final int NO_REQUEST_TOKEN = -1;

    private int requestToken = NO_REQUEST_TOKEN;
    private boolean confirmed;
    @Nullable
    private PendingRouteResult pendingResult;

    void reset() {
        requestToken = NO_REQUEST_TOKEN;
        confirmed = false;
        pendingResult = null;
    }

    void onRouteRequestPrepared(@Nullable NavigationRouteRequestSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (!snapshot.speculative) {
            reset();
            return;
        }
        requestToken = snapshot.requestToken;
        confirmed = false;
        pendingResult = null;
    }

    @NonNull
    NavigationSessionSpeculativeRoutes.Confirmation confirm() {
        if (!isActive()) {
            return NavigationSessionSpeculativeRoutes.Confirmation.NONE;
        }
        confirmed = true;
        return pendingResult == null
                ? NavigationSessionSpeculativeRoutes.Confirmation.IN_PROGRESS
                : NavigationSessionSpeculativeRoutes.Confirmation.RESULT_READY;
    }

    boolean isConfirmed(@NonNull NavigationRouteRequestSnapshot snapshot) {
        return isMatching(snapshot) && confirmed;
    }

    boolean deferResult(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute route,
            long beganAt
    ) {
        if (!snapshot.speculative || !isMatching(snapshot) || confirmed) {
            return false;
        }
        pendingResult = new PendingRouteResult(snapshot, route, beganAt);
        return true;
    }

    boolean cancelUnconfirmed() {
        if (!isActive() || confirmed) {
            return false;
        }
        reset();
        return true;
    }

    @Nullable
    PendingRouteResult consumeConfirmedResult() {
        if (!confirmed || pendingResult == null) {
            return null;
        }
        PendingRouteResult result = pendingResult;
        reset();
        return result;
    }

    void onRouteApplied(@NonNull NavigationRouteRequestSnapshot snapshot) {
        if (snapshot.speculative && isMatching(snapshot)) {
            reset();
        }
    }

    void onRouteFailed(@NonNull NavigationRouteRequestSnapshot snapshot) {
        if (snapshot.speculative && isMatching(snapshot)) {
            reset();
        }
    }

    private boolean isActive() {
        return requestToken != NO_REQUEST_TOKEN;
    }

    private boolean isMatching(@NonNull NavigationRouteRequestSnapshot snapshot) {
        return requestToken == snapshot.requestToken;
    }

    static final class PendingRouteResult {
        @NonNull
        final NavigationRouteRequestSnapshot snapshot;
        @NonNull
        final GeoJsonRoute route;
        final long beganAt;

        PendingRouteResult(
                @NonNull NavigationRouteRequestSnapshot snapshot,
                @NonNull GeoJsonRoute route,
                long beganAt
        ) {
            this.snapshot = snapshot;
            this.route = route;
            this.beganAt = beganAt;
        }
    }
}
