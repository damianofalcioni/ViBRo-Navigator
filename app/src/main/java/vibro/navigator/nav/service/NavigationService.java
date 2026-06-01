package vibro.navigator.nav.service;


import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.foreground.NavigationForegroundCoordinator;
import vibro.navigator.nav.intent.NavigationRequestIntentContract;
import vibro.navigator.nav.session.NavigationSession;
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
    private static final long DEFAULT_LOCATION_UPDATE_INTERVAL_MS = 1_000L;

    public interface Listener {
        void onState(@NonNull NavState state);
    }

    public static final String ACTION_START = "vibro.navigator.action.START";
    public static final String ACTION_STOP = "vibro.navigator.action.STOP";

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
    private final NavigationServiceRouteRecalculator routeRecalculator =
            new NavigationServiceRouteRecalculator(this, navigationSession, this::runtime, this::emitState);
    private final NavigationServiceLocationHandler locationHandler = new NavigationServiceLocationHandler(
            this,
            navigationSession,
            turnEvents,
            routeRecalculator::requestForLocation,
            this::emitState
    );
    @Nullable
    private NavigationServiceRuntime runtime;
    private final NavigationServiceUiVisibility uiVisibility =
            new NavigationServiceUiVisibility(navigationSession, stateBroadcaster, this::emitState);
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
        runtime = NavigationServiceRuntime.create(
                this,
                uiScheduler,
                navigationSession,
                turnEvents,
                locationHandler,
                uiVisibility,
                this::emitState,
                routeRecalculator::request
        );
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
        runtime().promoteToForeground(
                navigationSession.currentNavigationRequest(),
                navigationSession.isPaused()
        );
        foregroundCoordinator.startMonitoring();
    }

    private void readNavRequest(@NonNull Intent intent) {
        NavigationRequest request = NavigationRequestIntentContract.fromIntent(intent);
        navigationSession.loadRequest(request);
    }

    private void startNavigation() {
        runtime().resetTrackingState();

        if (!navigationSession.start(this, System.currentTimeMillis())) {
            emitState();
            return;
        }

        runtime().requestLocationUpdates(DEFAULT_LOCATION_UPDATE_INTERVAL_MS);
        runtime().requestCurrentLocationSeeds();
        runtime().startOrientation();
        emitState();
        NavigationRequest request = navigationSession.currentNavigationRequest();
        AppLogger.i(TAG, "Navigation started " + request.describe() + " blockedReset=true");
        locationHandler.seedStartupLocation();
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
        runtime().requestLocationUpdates(
                runtime().lastRequestedLocationMinTimeMsOrDefault(DEFAULT_LOCATION_UPDATE_INTERVAL_MS)
        );
        runtime().requestCurrentLocationSeeds();
        runtime().startOrientation();
        promoteToForeground();
        emitState();
        AppLogger.i(TAG, "Navigation resumed");
    }

    private void stopNavigation() {
        AppLogger.i(TAG, "Stopping navigation listeners=" + stateBroadcaster.size()
                + " routeLoaded=" + navigationSession.hasActiveRoute());
        navigationSession.stop();
        foregroundCoordinator.stopMonitoring();
        runtime().stopTrackingAndOrientation();
        runtime().stopManeuverSpeech();
        stateBroadcaster.clear();
        runtime().stopForegroundService();
    }

    private void emitState() {
        NavState s = navigationSession.buildState(
                this,
                runtime().nextEvaluationDeadlineElapsedMs(),
                System.currentTimeMillis(),
                runtime().fixedSatelliteCount(),
                runtime().displayHeadingDegrees(),
                runtime().displayHeadingAccuracyDegrees(),
                runtime().activeOrientationCue()
        );
        stateBroadcaster.dispatch(s);
    }

    @Override
    public void onDestroy() {
        AppLogger.i(TAG, "Service destroyed");
        stopNavigation();
        if (runtime != null) {
            runtime.stopScreenInteractivityMonitor();
            runtime.shutdownManeuverSpeaker();
            runtime.shutdownRouteExecutor();
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
