package vibro.navigator.auto;

import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.model.CarIcon;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.navigation.NavigationManagerCallback;
import androidx.car.app.navigation.model.Trip;

import vibro.navigator.android.automotive.AndroidCarNavigationNotifications;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTravelEstimate;
import vibro.navigator.settings.AppSettings;

/** Maintains navigation ownership even while the car screen is covered by another app. */
final class ViBRoCarHostNavigation implements NavigationManagerCallback {
    interface Host {
        void started();
        void ended();
        void updateTrip(Trip trip);
        void updateNotification(NavState state);
        void clear();
        default boolean usesImperialDistances() { return false; }
        @Nullable default CarIcon uTurnIcon() { return null; }
        default void dispose() { }
    }

    private final Host host;
    private final Runnable stopNavigation;
    private Runnable enableAutoDrive = () -> { };
    private boolean navigating;
    private boolean disposed;
    private long updateRevision;
    @Nullable private Trip lastTrip;
    @Nullable private String lastGuidanceLine;

    ViBRoCarHostNavigation(CarContext context, Runnable stopNavigation) {
        this(new CarHost(context), stopNavigation);
        context.getCarService(NavigationManager.class).setNavigationManagerCallback(this);
    }

    ViBRoCarHostNavigation(Host host, Runnable stopNavigation) {
        this.host = host;
        this.stopNavigation = stopNavigation;
    }

    void setAutoDriveAction(Runnable action) { enableAutoDrive = action; }

    @Override
    public void onAutoDriveEnabled() {
        if (!disposed) { enableAutoDrive.run(); }
    }

    void update(@Nullable NavState state) {
        if (disposed) { return; }
        long activeRevision = ++updateRevision;
        if (!isNavigating(state)) {
            close();
            return;
        }
        if (!navigating) {
            // Set first: host dispatch can cause synchronous callbacks/state refreshes.
            navigating = true;
            host.started();
        }
        if (!ownsUpdate(activeRevision)) {
            return; // Host start may revoke ownership or synchronously replace state in auto-drive.
        }
        publishTrip(state);
        if (ownsUpdate(activeRevision)) {
            publishNotification(state);
        }
    }

    private boolean ownsUpdate(long revision) {
        return navigating && revision == updateRevision;
    }

    private void publishTrip(NavState state) {
        Trip trip = ViBRoCarTrip.from(state, host.usesImperialDistances(), host.uTurnIcon());
        if (!ViBRoCarTrip.sameProgress(trip, lastTrip)) {
            lastTrip = trip;
            host.updateTrip(trip);
        }
    }

    private void publishNotification(NavState state) {
        String guidanceLine = state.routeStatus.guidance.nextLine;
        if (!guidanceLine.equals(lastGuidanceLine)) {
            lastGuidanceLine = guidanceLine;
            host.updateNotification(state);
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
        if (clearActiveGuidance()) {
            host.ended();
        }
    }

    private boolean clearActiveGuidance() {
        if (!navigating) { return false; }
        navigating = false;
        lastTrip = null;
        lastGuidanceLine = null;
        host.clear();
        return true;
    }

    void dispose() {
        disposed = true;
        close();
        host.dispose();
    }

    @Override
    public void onStopNavigation() {
        if (disposed) { return; }
        clearActiveGuidance(); // The host has already withdrawn navigation ownership.
        stopNavigation.run();
    }

    private static final class CarHost implements Host {
        private final NavigationManager manager;
        private final AndroidCarNavigationNotifications notifications;
        private final CarContext context;
        private final CarIcon uTurnIcon;

        CarHost(CarContext context) {
            this.context = context;
            manager = context.getCarService(NavigationManager.class);
            notifications = new AndroidCarNavigationNotifications(context);
            uTurnIcon = ViBRoCarManeuvers.uTurnIcon(context);
        }

        @Override public void started() { manager.navigationStarted(); }
        @Override public void ended() { manager.navigationEnded(); }
        @Override public void updateTrip(Trip trip) { manager.updateTrip(trip); }
        @Override public void updateNotification(NavState state) { notifications.update(state); }
        @Override public void clear() { notifications.clear(); }
        @Override public boolean usesImperialDistances() { return AppSettings.isImperialUnitsEnabled(context); }
        @Override public CarIcon uTurnIcon() { return uTurnIcon; }
        @Override public void dispose() { manager.clearNavigationManagerCallback(); }
    }
}
