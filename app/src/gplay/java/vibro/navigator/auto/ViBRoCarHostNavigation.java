package vibro.navigator.auto;

import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.navigation.NavigationManagerCallback;
import androidx.car.app.navigation.model.Trip;

import vibro.navigator.android.automotive.AndroidCarNavigationNotifications;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTravelEstimate;

/** Maintains navigation ownership even while the car screen is covered by another app. */
final class ViBRoCarHostNavigation implements NavigationManagerCallback {
    interface Host {
        void started();
        void ended();
        void update(Trip trip, NavState state);
        void clear();
        default void dispose() { }
    }

    private final Host host;
    private final Runnable stopNavigation;
    private boolean navigating;
    private boolean disposed;
    @Nullable private Trip lastTrip;
    private String lastGuidanceLine = "";

    ViBRoCarHostNavigation(CarContext context, Runnable stopNavigation) {
        this(new CarHost(context), stopNavigation);
        context.getCarService(NavigationManager.class).setNavigationManagerCallback(this);
    }

    ViBRoCarHostNavigation(Host host, Runnable stopNavigation) {
        this.host = host;
        this.stopNavigation = stopNavigation;
    }

    void update(@Nullable NavState state) {
        if (disposed) { return; }
        if (!isNavigating(state)) {
            close();
            return;
        }
        if (!navigating) {
            // Set first: host dispatch can cause synchronous callbacks/state refreshes.
            navigating = true;
            host.started();
        }
        if (!navigating) {
            return; // The host may revoke ownership synchronously while handling start.
        }
        Trip trip = ViBRoCarTrip.from(state);
        String guidanceLine = state.routeStatus.guidance.nextLine;
        if (!trip.equals(lastTrip) || !guidanceLine.equals(lastGuidanceLine)) {
            lastTrip = trip;
            lastGuidanceLine = guidanceLine;
            host.update(trip, state);
        }
    }

    static boolean isNavigating(@Nullable NavState state) {
        if (state == null || state.pauseStatus.paused) {
            return false;
        }
        NavTravelEstimate estimate = state.routeStatus.progress.travelEstimate;
        return estimate == null || !estimate.destinationReached;
    }

    void close() {
        boolean wasNavigating = navigating;
        navigating = false;
        lastTrip = null;
        host.clear();
        if (wasNavigating) {
            host.ended();
        }
    }

    void dispose() {
        disposed = true;
        close();
        host.dispose();
    }

    @Override
    public void onStopNavigation() {
        if (disposed) { return; }
        navigating = false; // The host has already withdrawn navigation ownership.
        lastTrip = null;
        host.clear();
        stopNavigation.run();
    }

    private static final class CarHost implements Host {
        private final NavigationManager manager;
        private final AndroidCarNavigationNotifications notifications;

        CarHost(CarContext context) {
            manager = context.getCarService(NavigationManager.class);
            notifications = new AndroidCarNavigationNotifications(context);
        }

        @Override public void started() { manager.navigationStarted(); }
        @Override public void ended() { manager.navigationEnded(); }
        @Override public void update(Trip trip, NavState state) {
            notifications.update(state);
            manager.updateTrip(trip);
        }
        @Override public void clear() { notifications.clear(); }
        @Override public void dispose() { manager.clearNavigationManagerCallback(); }
    }
}
