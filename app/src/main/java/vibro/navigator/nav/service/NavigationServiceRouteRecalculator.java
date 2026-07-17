package vibro.navigator.nav.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.routing.PendingRouteRecalculation;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionSpeculativeRoutes;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class NavigationServiceRouteRecalculator {

    interface RuntimeProvider {
        @NonNull
        NavigationServiceRuntime runtime();
    }

    interface SpeculativeRouteConfirmationSink {
        void onSpeculativeRouteConfirmed(
                @Nullable NavigationRerouteNotice rerouteNotice,
                @NonNull NavigationSessionSpeculativeRoutes.Confirmation confirmation
        );
    }

    @NonNull
    private final NavigationSession navigationSession;
    @NonNull
    private final RuntimeProvider runtimeProvider;
    @NonNull
    private final Runnable stateEmitter;
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock = AndroidElapsedRealtimeClock.INSTANCE;
    @NonNull
    private SpeculativeRouteConfirmationSink speculativeRouteConfirmationSink = (rerouteNotice, confirmation) -> {
    };

    NavigationServiceRouteRecalculator(
            @NonNull NavigationSession navigationSession,
            @NonNull RuntimeProvider runtimeProvider,
            @NonNull Runnable stateEmitter
    ) {
        this.navigationSession = navigationSession;
        this.runtimeProvider = runtimeProvider;
        this.stateEmitter = stateEmitter;
    }

    void attachSpeculativeRouteConfirmationSink(
            @NonNull SpeculativeRouteConfirmationSink speculativeRouteConfirmationSink
    ) {
        this.speculativeRouteConfirmationSink = speculativeRouteConfirmationSink;
    }

    void request(boolean force, @Nullable NavigationRerouteNotice rerouteNotice) {
        NavigationRouteRecalculationReason reason = rerouteNotice == null
                ? NavigationRouteRecalculationReason.EXPLICIT
                : NavigationRouteRecalculationReason.ROUTE_DEVIATION;
        request(force, rerouteNotice, null, reason);
    }

    void request(boolean force, @Nullable NavigationRerouteNotice rerouteNotice, @Nullable String inProgressNotice) {
        request(
                force,
                rerouteNotice,
                inProgressNotice,
                NavigationRouteRecalculationReason.EXPLICIT
        );
    }

    void requestForLocation(
            boolean force,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        request(force, rerouteNotice, null, reason);
    }

    void requestSpeculative(@NonNull NavigationRouteRecalculationReason reason) {
        NavigationRouteRequestSnapshot snapshot =
                navigationSession.speculativeRoutes().prepareRequest(
                        false,
                        elapsedRealtimeClock.elapsedRealtimeMs(),
                        reason
                );
        if (snapshot == null) {
            return;
        }
        stateEmitter.run();
        runtimeProvider.runtime().requestRoute(snapshot);
    }

    boolean confirmSpeculative(@Nullable NavigationRerouteNotice rerouteNotice) {
        NavigationSessionSpeculativeRoutes.Confirmation confirmation =
                navigationSession.speculativeRoutes().confirmRecalculation();
        if (confirmation == NavigationSessionSpeculativeRoutes.Confirmation.NONE) {
            return false;
        }
        speculativeRouteConfirmationSink.onSpeculativeRouteConfirmed(rerouteNotice, confirmation);
        return true;
    }

    void cancelSpeculative() {
        if (!navigationSession.speculativeRoutes().cancelRecalculation()) {
            return;
        }
        stateEmitter.run();
        PendingRouteRecalculation pending = navigationSession.consumePendingRouteRecalculation();
        if (pending != null) {
            request(pending);
        }
    }

    void request(@NonNull PendingRouteRecalculation pendingRecalculation) {
        request(
                pendingRecalculation.force,
                null,
                pendingRecalculation.inProgressNotice,
                pendingRecalculation.reason
        );
    }

    private void request(
            boolean force,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @Nullable String inProgressNotice,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        NavigationRouteRequestSnapshot snapshot =
                navigationSession.prepareRouteRequest(
                        force,
                        elapsedRealtimeClock.elapsedRealtimeMs(),
                        inProgressNotice,
                        reason
                );
        if (snapshot == null) {
            if (rerouteNotice != null && navigationSession.isRouteCalculationInProgress()) {
                runtimeProvider.runtime().foregroundController().sendOffRouteNotification(rerouteNotice);
            }
            return;
        }
        stateEmitter.run();
        NavigationServiceRuntime runtime = runtimeProvider.runtime();
        if (rerouteNotice != null) {
            runtime.foregroundController().sendOffRouteNotification(rerouteNotice);
        }
        runtime.requestRoute(snapshot);
    }
}
