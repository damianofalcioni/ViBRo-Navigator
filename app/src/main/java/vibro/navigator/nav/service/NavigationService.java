package vibro.navigator.nav.service;


import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.foreground.NavigationForegroundCoordinator;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionResourceAdapter;
import vibro.navigator.nav.policy.NavigationLifecyclePolicy;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vibro.navigator.logging.AppLogger;

// Android service shell: explicit collaborators keep lifecycle ownership visible and behavior isolated in helpers.
public class NavigationService extends Service {

    private static final String TAG = "NavigationService";
    private static final long FOREGROUND_NOTIFICATION_CHECK_INTERVAL_MS = 5_000L;
    private static final long DEFAULT_LOCATION_UPDATE_INTERVAL_MS =
            NavigationLocationController.DEFAULT_UPDATE_INTERVAL_MS;
    private static final long STARTUP_LOCATION_UPDATE_INTERVAL_MS =
            NavigationLocationController.STARTUP_UPDATE_INTERVAL_MS;

    public interface Listener {
        void onState(@NonNull NavState state);

        default void onNavigationStopped() {
        }
    }

    public static final String ACTION_START = "vibro.navigator.action.START";
    public static final String ACTION_STOP = "vibro.navigator.action.STOP";
    public static final String ACTION_RECOVER_LOCATION = "vibro.navigator.action.RECOVER_LOCATION";

    public static final int NOTIFICATION_ID_ONGOING = 1;
    public static final int NOTIFICATION_ID_TURN = 2;
    public static final String CHANNEL_ID_NAV = "navigator.navigation";
    public static final String CHANNEL_ID_ALERT = "navigator.alerts.v2";
    public static final String CHANNEL_ID_TURN_LEFT = "navigator.turn.left.v3";
    public static final String CHANNEL_ID_TURN_RIGHT = "navigator.turn.right.v3";

    private final NavigationSession navigationSession = new NavigationSession();
    private final NavigationStateBroadcaster stateBroadcaster = new NavigationStateBroadcaster();
    private final TaskScheduler uiScheduler = AndroidTaskScheduler.main();
    private final NavigationServiceTurnEvents turnEvents = new NavigationServiceTurnEvents(navigationSession);
    private final NavigationServiceUiVisibility uiVisibility =
            new NavigationServiceUiVisibility(
                    navigationSession,
                    stateBroadcaster,
                    this::emitState,
                    this::emitCompassState,
                    this::clearCompassStreetViewport,
                    this::setGnssStatusDisplayActive
            );
    private final NavigationServiceRouteRecalculator routeRecalculator =
            new NavigationServiceRouteRecalculator(
                    navigationSession,
                    this::runtime,
                    this::emitState
            );
    @Nullable
    private NavigationServiceRuntime runtime;
    private final NavigationServiceLocationHandler locationHandler = new NavigationServiceLocationHandler(
            this,
            navigationSession,
            turnEvents,
            new NavigationServiceLocationRouteRecalculator(routeRecalculator),
            this::hasRunningNavigationSession,
            location -> {
                if (runtime != null) {
                    runtime.onAcceptedLocation(location);
                }
            },
            this::emitState
    );
    private final NavigationForegroundCoordinator foregroundCoordinator =
            new NavigationForegroundCoordinator(
                    uiScheduler,
                    new NavigationLifecyclePolicy(),
                    FOREGROUND_NOTIFICATION_CHECK_INTERVAL_MS,
                    new NavigationServiceForegroundHost(
                            () -> runtime().foregroundController(),
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
            compassState -> {
                if (runtime != null) {
                    runtime.onCompassStreetViewport(compassState);
                }
            },
            this::refreshLocationUpdateSettings,
            notice -> routeRecalculator.request(true, null, notice),
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

    @Override
    public void onCreate() {
        super.onCreate();
        runtime = NavigationServiceRuntimeFactory.create(
                this,
                uiScheduler,
                navigationSession,
                turnEvents,
                locationHandler,
                uiVisibility,
                this::emitState,
                routeRecalculator
        );
        setGnssStatusDisplayActive(uiVisibility.canDispatchStateToUi());
        AppLogger.i(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_RECOVER_LOCATION.equals(intent.getAction())) {
            return recoverStalledLocation();
        }
        return commandHandler.handle(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        AppLogger.i(TAG, "Client bound to service");
        return binder;
    }

    private void promoteToForeground() {
        runtime().promoteToForeground(
                navigationSession.currentNavigationRequest(),
                navigationSession.isPaused()
        );
        foregroundCoordinator.startMonitoring();
    }

    private void readNavRequest(@NonNull Intent intent) {
        NavigationRequest request = AndroidNavigationRequestIntentContract.fromIntent(intent);
        navigationSession.loadRequest(request);
    }

    private void startNavigation() {
        runtime().resetTrackingState();
        runtime().resetStreetOverlay();

        long nowElapsedMs = runtime().elapsedRealtimeMs();
        if (!NavigationSessionResourceAdapter.start(
                navigationSession,
                new AndroidNavigationTextResources(this),
                nowElapsedMs,
                uiVisibility.isScreenInteractive(),
                runtime().batterySnapshotReader.read()
        )) {
            emitState();
            return;
        }

        setGnssStatusDisplayActive(uiVisibility.canDispatchStateToUi());
        runtime().requestLocationUpdates(STARTUP_LOCATION_UPDATE_INTERVAL_MS);
        runtime().locationRecovery.start();
        requestCurrentLocationSeedsForActiveNavigation();
        runtime().startOrientation();
        emitState();
        NavigationRequest request = navigationSession.currentNavigationRequest();
        AppLogger.i(TAG, "Navigation started " + request.describe() + " blockedReset=true");
        locationHandler.seedStartupLocation(nowElapsedMs);
    }

    private void pauseNavigation() {
        if (!navigationSession.pause()) {
            return;
        }
        runtime().stopTrackingAndOrientation();
        promoteToForeground();
        emitState();
        AppLogger.i(TAG, "Navigation paused");
    }

    private void resumeNavigation() {
        if (!navigationSession.resume()) {
            return;
        }
        setGnssStatusDisplayActive(uiVisibility.canDispatchStateToUi());
        runtime().requestLocationUpdates(
                runtime().lastRequestedLocationMinTimeMsOrDefault(DEFAULT_LOCATION_UPDATE_INTERVAL_MS)
        );
        runtime().locationRecovery.start();
        requestCurrentLocationSeedsForActiveNavigation();
        runtime().startOrientation();
        promoteToForeground();
        emitState();
        AppLogger.i(TAG, "Navigation resumed");
    }

    private void requestCurrentLocationSeedsForActiveNavigation() {
        if (hasRunningNavigationSession()) {
            runtime().requestCurrentLocationSeeds();
        }
    }

    private boolean hasRunningNavigationSession() {
        return navigationSession.isStarted() && !navigationSession.isPaused();
    }

    private void stopNavigation() {
        AppLogger.i(TAG, "Stopping navigation listeners=" + stateBroadcaster.size()
                + " routeLoaded=" + navigationSession.hasActiveRoute());
        navigationSession.stop();
        runtime().resetStreetOverlay();
        foregroundCoordinator.stopMonitoring();
        runtime().stopTrackingAndOrientation();
        runtime().stopManeuverSpeech();
        stateBroadcaster.dispatchStopped();
        stateBroadcaster.clear();
        uiVisibility.onStateListenersChanged();
        runtime().stopForegroundService();
    }

    private int recoverStalledLocation() {
        AppLogger.i(TAG, "onStartCommand action=" + ACTION_RECOVER_LOCATION);
        if (!navigationSession.isStarted() || navigationSession.isPaused()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        runtime().locationRecovery.recoverIfStalled();
        return START_STICKY;
    }

    private void setGnssStatusDisplayActive(boolean active) {
        if (runtime != null) {
            runtime.setGnssStatusTrackingAllowed(active);
        }
    }

    private void refreshLocationUpdateSettings() {
        if (runtime != null && navigationSession.isStarted() && !navigationSession.isPaused()) {
            runtime.requestLocationUpdates(DEFAULT_LOCATION_UPDATE_INTERVAL_MS);
        }
    }

    private void clearCompassStreetViewport() {
        if (runtime != null) {
            runtime.onCompassStreetViewport(null);
        }
    }

    private void emitState() {
        if (runtime != null) {
            runtime.locationRecovery.onUpdateIntervalChanged();
            NavigationSessionResourceAdapter.recordBatterySnapshot(
                    navigationSession,
                    runtime.batterySnapshotReader.read()
            );
        }
        if (!uiVisibility.canDispatchStateToUi()) {
            return;
        }
        var orientationCue = runtime().activeOrientationCue();
        NavState s = navigationSession.buildState(
                this,
                runtime().nextEvaluationDeadlineElapsedMs(),
                System.currentTimeMillis(),
                runtime().fixedSatelliteCount(),
                runtime().displayHeadingDegrees(),
                runtime().displayHeadingAccuracyDegrees(),
                orientationCue
        );
        s = runtime().attachStreetOverlay(s);
        stateBroadcaster.dispatchStructural(s, orientationCue);
    }

    private void emitCompassState() {
        stateBroadcaster.dispatchHeadingOrRefreshStructural(
                navigationSession,
                runtime,
                uiVisibility.canDispatchStateToUi(),
                this::emitState
        );
    }

    @Override
    public void onDestroy() {
        AppLogger.i(TAG, "Service destroyed");
        stopNavigation();
        if (runtime != null) {
            runtime.stopScreenInteractivityMonitor();
            runtime.shutdown();
            runtime = null;
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

    @NonNull
    private NavigationServiceRuntime runtime() {
        if (runtime == null) {
            throw new IllegalStateException("Navigation service runtime is not initialized");
        }
        return runtime;
    }

}
