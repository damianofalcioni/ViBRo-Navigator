package vibro.navigator.nav;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vibro.navigator.R;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.util.AppLogger;

import java.util.List;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;

public class NavigationService extends Service implements LocationListener {

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

    private final IBinder binder = new LocalBinder();
    private final NavigationSession navigationSession = new NavigationSession();
    private final NavigationStateBroadcaster stateBroadcaster = new NavigationStateBroadcaster();
    private final Handler notificationMonitorHandler = new Handler(Looper.getMainLooper());
    private final NavigationForegroundCoordinator foregroundCoordinator =
            new NavigationForegroundCoordinator(
                    notificationMonitorHandler,
                    new NavigationLifecyclePolicy(),
                    FOREGROUND_NOTIFICATION_CHECK_INTERVAL_MS,
                    new ForegroundHost()
            );
    @Nullable
    private NavigationRouteExecutor routeExecutor;
    private final NavigationRouteExecutor.Callback routeCallback = new NavigationRouteCallback();
    private NavigationForegroundController foregroundController;
    private NavigationLocationController locationController;
    private NavigationTurnEventDispatcher turnEventDispatcher;
    private NavigationOrientationController orientationController;
    private NavigationScreenInteractivityMonitor screenInteractivityMonitor;
    private boolean navigationUiVisible;
    private boolean screenInteractive = true;

    @Override
    public void onCreate() {
        super.onCreate();
        foregroundController = new NavigationForegroundController(this);
        locationController = new NavigationLocationController(this, this);
        routeExecutor = NavigationRouteExecutor.createDefault(this, notificationMonitorHandler);
        turnEventDispatcher = new NavigationTurnEventDispatcher(new ForegroundNotificationSink());
        orientationController = new NavigationOrientationController(
                this,
                notificationMonitorHandler,
                new OrientationCompassUiState()
        );
        screenInteractivityMonitor =
                new NavigationScreenInteractivityMonitor(this, this::onScreenInteractiveChanged);
        screenInteractive = screenInteractivityMonitor.start();
        foregroundController.ensureChannels();
        AppLogger.i(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppLogger.i(TAG, "onStartCommand action=" + (intent == null ? "null" : intent.getAction())
                + " flags=" + flags
                + " startId=" + startId);
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopNavigation();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null && ACTION_START.equals(intent.getAction())) {
            readNavRequest(intent);
            startNavigation();
        }

        promoteToForeground();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        AppLogger.i(TAG, "Client bound to service");
        return binder;
    }

    public final class LocalBinder extends Binder {
        public void registerListener(@NonNull Listener l) {
            stateBroadcaster.register(l);
            AppLogger.d(TAG, "Listener registered totalListeners=" + stateBroadcaster.size());
            emitState();
        }

        public void ensureForegroundNotification() {
            foregroundCoordinator.onNavigationUiConnected();
        }

        public void setNavigationUiVisible(boolean visible) {
            NavigationService.this.setNavigationUiVisible(visible);
        }

        public void unregisterListener(@NonNull Listener l) {
            stateBroadcaster.unregister(l);
            AppLogger.d(TAG, "Listener unregistered totalListeners=" + stateBroadcaster.size());
        }

        public void addBlockedWaypoint() {
            if (navigationSession.isPaused()) {
                AppLogger.w(TAG, "Blocked waypoint requested while navigation is paused");
                return;
            }
            Location loc = navigationSession.getLastFilteredLocation();
            if (loc == null) {
                AppLogger.w(TAG, "Blocked waypoint requested without a current filtered location");
                return;
            }
            List<NogoPoint> added = navigationSession.addBlockedPointsAhead();
            if (added.isEmpty()) {
                AppLogger.w(TAG, "Blocked-road reroute ignored because no route point ahead could be matched");
                return;
            }
            AppLogger.i(TAG, "Blocked-road points added added=" + formatNogoPoints(added)
                    + " location=" + formatLocation(loc));
            requestRouteRecalc(true, null, getString(R.string.nav_route_notice_blocked_road_recalculating));
        }

        public void stop() {
            AppLogger.i(TAG, "Stop requested through binder");
            stopNavigation();
            stopSelf();
        }

        public void pause() {
            NavigationService.this.pauseNavigation();
        }

        public void resume() {
            NavigationService.this.resumeNavigation();
        }

        public boolean isPaused() {
            return navigationSession.isPaused();
        }
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

        Location seed = locationController.getBestStartupLastKnownLocation();
        if (seed != null) {
            AppLogger.i(TAG, "Using last known location as seed " + formatLocation(seed));
            onLocationChanged(seed);
        } else {
            AppLogger.w(TAG, "No usable cached location available at navigation start "
                    + locationController.describeAvailability());
        }
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

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (navigationSession.isPaused()) {
            AppLogger.d(TAG, "Ignoring location update while navigation is paused");
            return;
        }
        NavigationSession.LocationUpdateResult result =
                navigationSession.onRawLocationChanged(this, location, System.currentTimeMillis());
        if (result.isDropped()) {
            return;
        }
        if (result.shouldRecalculateRoute()) {
            requestRouteRecalc(false, result.getRerouteNotice());
        } else if (result.getSuggestedUpdateIntervalMs() > 0L) {
            locationController.requestLocationUpdates(result.getSuggestedUpdateIntervalMs());
        }
        dispatchTurnEvents(result.turnEvents);
        orientationController.maybeSendStationaryOrientationNotification(navigationSession, foregroundController);
        emitState();
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        if (navigationSession.isPaused()) {
            AppLogger.d(TAG, "Ignoring provider enabled while navigation is paused provider=" + provider);
            return;
        }
        AppLogger.i(TAG, "Location provider enabled provider=" + provider);
        locationController.onProviderEnabled(provider, DEFAULT_LOCATION_UPDATE_INTERVAL_MS);
        emitState();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        if (navigationSession.isPaused()) {
            AppLogger.d(TAG, "Ignoring provider disabled while navigation is paused provider=" + provider);
            return;
        }
        AppLogger.w(TAG, "Location provider disabled provider=" + provider);
        navigationSession.onProviderDisabled(provider);
        locationController.requestLocationUpdates(
                locationController.getLastRequestedLocationMinTimeMsOrDefault(DEFAULT_LOCATION_UPDATE_INTERVAL_MS)
        );
        emitState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStatusChanged(@Nullable String provider, int status, @Nullable Bundle extras) {
        AppLogger.d(TAG, "Location provider status changed provider=" + provider + " status=" + status);
    }

    private void requestRouteRecalc(boolean force, @Nullable NavigationRerouteNotice rerouteNotice) {
        requestRouteRecalc(force, rerouteNotice, null);
    }

    private void requestRouteRecalc(
            boolean force,
            @Nullable NavigationRerouteNotice rerouteNotice,
            @Nullable String inProgressNotice
    ) {
        NavigationSession.RouteRequestSnapshot snapshot =
                navigationSession.prepareRouteRequest(force, System.currentTimeMillis(), inProgressNotice);
        if (snapshot == null) {
            return;
        }
        emitState();
        if (rerouteNotice != null) {
            foregroundController.sendOffRouteNotification(rerouteNotice);
        }
        if (routeExecutor == null) {
            AppLogger.w(TAG, "Route executor unavailable, cannot calculate route");
            return;
        }
        routeExecutor.requestRoute(this, snapshot, routeCallback);
    }

    private void dispatchTurnEvents(@NonNull List<NavigationSession.TurnEvent> turnEvents) {
        if (!navigationSession.isPaused() && turnEventDispatcher != null) {
            turnEventDispatcher.dispatch(turnEvents);
        }
    }

    private void setNavigationUiVisible(boolean visible) {
        if (navigationUiVisible == visible) {
            return;
        }
        navigationUiVisible = visible;
        if (visible && screenInteractive) {
            emitState();
        }
    }

    private void onScreenInteractiveChanged(boolean interactive) {
        if (screenInteractive == interactive) {
            return;
        }
        screenInteractive = interactive;
        if (interactive && navigationUiVisible && stateBroadcaster.size() > 0) {
            emitState();
        }
    }

    private boolean shouldDispatchCompassUi() {
        return NavigationOrientationController.shouldDispatchCompassUi(
                navigationSession.hasActiveRoute(),
                navigationUiVisible,
                screenInteractive
        );
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

    private final class OrientationCompassUiState implements NavigationOrientationController.CompassUiState {
        @Override
        public boolean shouldDispatchCompassUi() {
            return NavigationService.this.shouldDispatchCompassUi();
        }

        @Override
        public boolean hasStateListeners() {
            return stateBroadcaster.size() > 0;
        }

        @Override
        public void requestStateRefresh() {
            emitState();
        }
    }

    private final class ForegroundHost implements NavigationForegroundCoordinator.Host {
        @Override
        public boolean isOngoingNotificationVisible() {
            return foregroundController != null && foregroundController.isOngoingNotificationVisible();
        }

        @Override
        public void promoteToForeground() {
            NavigationService.this.promoteToForeground();
        }

        @Override
        public void stopNavigation() {
            NavigationService.this.stopNavigation();
        }

        @Override
        public void stopSelf() {
            NavigationService.this.stopSelf();
        }
    }

    private final class ForegroundNotificationSink implements NavigationTurnEventDispatcher.TurnNotificationSink {
        @Override
        public void sendImminentTurnNotification(
                @NonNull VoiceHint hint,
                double distanceMeters,
                double timeSeconds
        ) {
            foregroundController.sendImminentTurnNotification(hint, distanceMeters, timeSeconds);
        }
    }

    private final class NavigationRouteCallback implements NavigationRouteExecutor.Callback {
        @Override
        public void onRouteApplied(
                @NonNull NavigationSession.RouteRequestSnapshot snapshot,
                @NonNull GeoJsonRoute newRoute,
                long beganAt
        ) {
            dispatchTurnEvents(navigationSession.applyRouteResult(NavigationService.this, snapshot, newRoute, beganAt));
            orientationController.maybeSendStationaryOrientationNotification(navigationSession, foregroundController);
            emitState();
            if (navigationSession.consumePendingRouteRecalculation()) {
                AppLogger.i(TAG, "Re-running queued route recalculation after previous request finished");
                requestRouteRecalc(true, null);
            }
        }

        @Override
        public void onRouteFailure(
                @NonNull NavigationSession.RouteRequestSnapshot snapshot,
                @NonNull Exception error
        ) {
            navigationSession.applyRouteFailure(NavigationService.this, snapshot, error);
            emitState();
            if (navigationSession.consumePendingRouteRecalculation()) {
                AppLogger.i(TAG, "Retrying queued route recalculation after previous request failed");
                requestRouteRecalc(true, null);
            }
        }
    }

    @NonNull
    private static String formatNogoPoints(@NonNull List<NogoPoint> values) {
        if (values.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(values.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    @NonNull
    private static String formatLocation(@Nullable Location location) {
        if (location == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(location.getProvider())
                .append("(")
                .append(location.getLatitude())
                .append(",")
                .append(location.getLongitude())
                .append(")");
        if (location.hasAccuracy()) {
            sb.append(" acc=").append(location.getAccuracy());
        }
        if (location.hasSpeed()) {
            sb.append(" speed=").append(location.getSpeed());
        }
        if (location.hasBearing()) {
            sb.append(" bearing=").append(location.getBearing());
        }
        sb.append(" time=").append(location.getTime());
        return sb.toString();
    }

}
