package vibro.navigator.nav.service;


import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.foreground.NavigationForegroundCoordinator;
import vibro.navigator.nav.foreground.NavigationScreenInteractivityMonitor;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.orientation.NavigationOrientationController;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.routing.NavigationRouteExecutor;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.policy.NavigationLifecyclePolicy;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vibro.navigator.util.AppLogger;

// Android service shell: explicit collaborators keep lifecycle ownership visible and behavior isolated in helpers.
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class NavigationService extends Service {

    private static final String TAG = "NavigationService";
    private static final long FOREGROUND_NOTIFICATION_CHECK_INTERVAL_MS = 5_000L;
    private static final long DEFAULT_LOCATION_UPDATE_INTERVAL_MS = 1_000L;

    public interface Listener {
        void onState(@NonNull NavState state);
    }

    public static final String ACTION_START = "vibro.navigator.action.START";
    public static final String ACTION_STOP = "vibro.navigator.action.STOP";

    public static final int NOTIFICATION_ID_ONGOING = 1;
    public static final int NOTIFICATION_ID_TURN = 2;
    public static final String CHANNEL_ID_NAV = "navigator.navigation";
    public static final String CHANNEL_ID_ALERT = "navigator.alerts.v1";
    public static final String CHANNEL_ID_TURN_LEFT = "navigator.turn.left.v2";
    public static final String CHANNEL_ID_TURN_RIGHT = "navigator.turn.right.v2";

    private final NavigationSession navigationSession = new NavigationSession();
    private final NavigationStateBroadcaster stateBroadcaster = new NavigationStateBroadcaster();
    private final Handler notificationMonitorHandler = new Handler(Looper.getMainLooper());
    private final NavigationServiceTurnEvents turnEvents = new NavigationServiceTurnEvents(navigationSession);
    private final NavigationServiceLocationHandler locationHandler = new NavigationServiceLocationHandler(
            this,
            navigationSession,
            turnEvents,
            this::requestRouteRecalc,
            this::emitState
    );
    private NavigationForegroundController foregroundController;
    private NavigationLocationController locationController;
    private NavigationOrientationController orientationController;
    private NavigationScreenInteractivityMonitor screenInteractivityMonitor;
    private final NavigationServiceUiVisibility uiVisibility =
            new NavigationServiceUiVisibility(navigationSession, stateBroadcaster, this::emitState);
    private final NavigationForegroundCoordinator foregroundCoordinator =
            new NavigationForegroundCoordinator(
                    notificationMonitorHandler,
                    new NavigationLifecyclePolicy(),
                    FOREGROUND_NOTIFICATION_CHECK_INTERVAL_MS,
                    new NavigationServiceForegroundHost(
                            () -> foregroundController,
                            this::promoteToForeground,
                            this::stopNavigation,
                            this::stopSelf
                    )
            );
    private final NavigationServiceBinder binder = new NavigationServiceBinder(new NavigationServiceBinderHost(
            this,
            stateBroadcaster,
            foregroundCoordinator,
            navigationSession,
            uiVisibility,
            this::emitState,
            notice -> requestRouteRecalc(true, null, notice),
            () -> {
                stopNavigation();
                stopSelf();
            },
            this::pauseNavigation,
            this::resumeNavigation
    ));
    private final NavigationServiceCommandHandler commandHandler = new NavigationServiceCommandHandler(
            this::readNavRequest,
            this::startNavigation,
            this::stopNavigation,
            this::stopSelf,
            this::promoteToForeground
    );
    @Nullable
    private NavigationRouteExecutor routeExecutor;
    @Nullable
    private NavigationRouteExecutor.Callback routeCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        NavigationServiceDependencies dependencies = NavigationServiceDependencies.create(
                this,
                notificationMonitorHandler,
                navigationSession,
                turnEvents,
                locationHandler,
                uiVisibility,
                this::emitState,
                this::requestRouteRecalc
        );
        foregroundController = dependencies.foregroundController;
        locationController = dependencies.locationController;
        routeExecutor = dependencies.routeExecutor;
        routeCallback = dependencies.routeCallback;
        orientationController = dependencies.orientationController;
        screenInteractivityMonitor = dependencies.screenInteractivityMonitor;
        uiVisibility.setScreenInteractive(dependencies.screenInteractive);
        AppLogger.i(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return commandHandler.handle(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        AppLogger.i(TAG, "Client bound to service");
        return binder;
    }

    private void promoteToForeground() {
        foregroundController.promoteToForeground(
                navigationSession.currentNavigationRequest(),
                navigationSession.isPaused()
        );
        foregroundCoordinator.startMonitoring();
    }

    private void readNavRequest(@NonNull Intent intent) {
        NavigationRequest request = NavigationRequest.fromIntent(intent);
        navigationSession.loadRequest(request);
    }

    private void startNavigation() {
        locationController.resetTrackingState();

        if (!navigationSession.start(this, System.currentTimeMillis())) {
            emitState();
            return;
        }

        locationController.requestLocationUpdates(DEFAULT_LOCATION_UPDATE_INTERVAL_MS);
        locationController.requestCurrentLocationSeeds();
        orientationController.start();
        emitState();
        NavigationRequest request = navigationSession.currentNavigationRequest();
        AppLogger.i(TAG, "Navigation started " + request.describe() + " blockedReset=true");
        locationHandler.seedStartupLocation();
    }

    private void pauseNavigation() {
        if (!navigationSession.pause()) {
            return;
        }
        locationController.stopTracking();
        orientationController.stop();
        promoteToForeground();
        emitState();
        AppLogger.i(TAG, "Navigation paused");
    }

    private void resumeNavigation() {
        if (!navigationSession.resume()) {
            return;
        }
        locationController.requestLocationUpdates(
                locationController.getLastRequestedLocationMinTimeMsOrDefault(DEFAULT_LOCATION_UPDATE_INTERVAL_MS)
        );
        locationController.requestCurrentLocationSeeds();
        orientationController.start();
        promoteToForeground();
        emitState();
        AppLogger.i(TAG, "Navigation resumed");
    }

    private void stopNavigation() {
        AppLogger.i(TAG, "Stopping navigation listeners=" + stateBroadcaster.size()
                + " routeLoaded=" + navigationSession.hasActiveRoute());
        navigationSession.stop();
        foregroundCoordinator.stopMonitoring();
        locationController.stopTracking();
        orientationController.stop();
        stateBroadcaster.clear();
        foregroundController.stopForegroundService();
    }

    private void requestRouteRecalc(boolean force, @Nullable NavigationRerouteNotice rerouteNotice) {
        requestRouteRecalc(force, rerouteNotice, null);
    }

    private void requestRouteRecalc(
            boolean force,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @Nullable String inProgressNotice
    ) {
        NavigationRouteRequestSnapshot snapshot =
                navigationSession.prepareRouteRequest(force, System.currentTimeMillis(), inProgressNotice);
        if (snapshot == null) {
            return;
        }
        emitState();
        if (rerouteNotice != null) {
            foregroundController.sendOffRouteNotification(rerouteNotice);
        }
        if (routeExecutor == null || routeCallback == null) {
            AppLogger.w(TAG, "Route executor unavailable, cannot calculate route");
            return;
        }
        routeExecutor.requestRoute(this, snapshot, routeCallback);
    }

    private void emitState() {
        NavState s = navigationSession.buildState(
                this,
                locationController.getNextEvaluationDeadlineElapsedMs(),
                System.currentTimeMillis(),
                locationController.getFixedSatelliteCount(),
                orientationController.currentDisplayHeadingDegrees(),
                orientationController.currentDisplayHeadingAccuracyDegrees()
        );
        stateBroadcaster.dispatch(s);
    }

    @Override
    public void onDestroy() {
        AppLogger.i(TAG, "Service destroyed");
        stopNavigation();
        if (screenInteractivityMonitor != null) {
            screenInteractivityMonitor.stop();
            screenInteractivityMonitor = null;
        }
        if (routeExecutor != null) {
            routeExecutor.shutdown();
            routeExecutor = null;
        }
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (foregroundCoordinator.shouldStopOnTaskRemoved()) {
            AppLogger.i(TAG, "Task removed, stopping navigation service");
            stopNavigation();
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }

}
