package vibro.navigator.nav.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.session.NavigationSession;

final class NavigationServiceRouteRecalculator {

    interface RuntimeProvider {
        @NonNull
        NavigationServiceRuntime runtime();
    }

    @NonNull
    private final NavigationService service;
    @NonNull
    private final NavigationSession navigationSession;
    @NonNull
    private final RuntimeProvider runtimeProvider;
    @NonNull
    private final Runnable stateEmitter;

    NavigationServiceRouteRecalculator(
            @NonNull NavigationService service,
            @NonNull NavigationSession navigationSession,
            @NonNull RuntimeProvider runtimeProvider,
            @NonNull Runnable stateEmitter
    ) {
        this.service = service;
        this.navigationSession = navigationSession;
        this.runtimeProvider = runtimeProvider;
        this.stateEmitter = stateEmitter;
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

    private void request(
            boolean force,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @Nullable String inProgressNotice,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        NavigationRouteRequestSnapshot snapshot =
                navigationSession.prepareRouteRequest(
                        force,
                        System.currentTimeMillis(),
                        inProgressNotice,
                        reason
                );
        if (snapshot == null) {
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
