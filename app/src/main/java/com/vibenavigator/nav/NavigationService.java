package com.vibenavigator.nav;

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
import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.util.AppLogger;

import java.util.List;

public class NavigationService extends Service implements LocationListener {

    private static final String TAG = "NavigationService";
    private static final long FOREGROUND_NOTIFICATION_CHECK_INTERVAL_MS = 5_000L;

    public interface Listener {
        void onState(@NonNull NavState state);
    }

    public static final String ACTION_START = "com.vibenavigator.action.START";
    public static final String ACTION_STOP = "com.vibenavigator.action.STOP";

    public static final int NOTIFICATION_ID_ONGOING = 1;
    public static final int NOTIFICATION_ID_TURN = 2;
    public static final String CHANNEL_ID_NAV = "vibenavigator.navigation";
    public static final String CHANNEL_ID_TURN_LEFT = "vibenavigator.turn.left";
    public static final String CHANNEL_ID_TURN_RIGHT = "vibenavigator.turn.right";

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
    private final NavigationRouteExecutor routeExecutor =
            NavigationRouteExecutor.createDefault(new Handler(Looper.getMainLooper()));
    private final NavigationRouteExecutor.Callback routeCallback = new NavigationRouteCallback();
    private NavigationForegroundController foregroundController;
    private NavigationLocationController locationController;
    private NavigationWakeLockController wakeLockController;
    private NavigationTurnEventDispatcher turnEventDispatcher;

    @Override
    public void onCreate() {
        super.onCreate();
        foregroundController = new NavigationForegroundController(this);
        locationController = new NavigationLocationController(this, this);
        wakeLockController = new NavigationWakeLockController(this);
        turnEventDispatcher = new NavigationTurnEventDispatcher(new ForegroundNotificationSink());
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

        public void unregisterListener(@NonNull Listener l) {
            stateBroadcaster.unregister(l);
            AppLogger.d(TAG, "Listener unregistered totalListeners=" + stateBroadcaster.size());
        }

        public void addBlockedWaypoint() {
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
            requestRouteRecalc(true, null);
        }

        public void stop() {
            AppLogger.i(TAG, "Stop requested through binder");
            stopNavigation();
            stopSelf();
        }
    }

    private void promoteToForeground() {
        foregroundController.promoteToForeground(navigationSession.currentNavigationRequest());
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

        wakeLockController.acquire();
        locationController.requestLocationUpdates(2000L);
        locationController.requestCurrentLocationSeeds();
        emitState();
        NavigationRequest request = navigationSession.currentNavigationRequest();
        AppLogger.i(TAG, "Navigation started " + request.describe() + " blockedReset=true");

        Location seed = locationController.getBestLastKnownLocation();
        if (seed != null) {
            AppLogger.i(TAG, "Using last known location as seed " + formatLocation(seed));
            onLocationChanged(seed);
        } else {
            AppLogger.w(TAG, "No last known location available at navigation start "
                    + locationController.describeAvailability());
        }
    }

    private void stopNavigation() {
        AppLogger.i(TAG, "Stopping navigation listeners=" + stateBroadcaster.size()
                + " routeLoaded=" + navigationSession.hasActiveRoute());
        navigationSession.stop();
        foregroundCoordinator.stopMonitoring();
        locationController.stopTracking();
        wakeLockController.release();
        stateBroadcaster.clear();
        foregroundController.stopForegroundService();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
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
        emitState();
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        AppLogger.i(TAG, "Location provider enabled provider=" + provider);
        locationController.onProviderEnabled(provider, 2000L);
        emitState();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        AppLogger.w(TAG, "Location provider disabled provider=" + provider);
        navigationSession.onProviderDisabled(provider);
        locationController.requestLocationUpdates(locationController.getLastRequestedLocationMinTimeMsOrDefault(2000L));
        emitState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStatusChanged(@Nullable String provider, int status, @Nullable Bundle extras) {
        AppLogger.d(TAG, "Location provider status changed provider=" + provider + " status=" + status);
    }

    private void requestRouteRecalc(boolean force, @Nullable NavigationRerouteNotice rerouteNotice) {
        NavigationSession.RouteRequestSnapshot snapshot =
                navigationSession.prepareRouteRequest(force, System.currentTimeMillis());
        if (snapshot == null) {
            return;
        }
        emitState();
        if (rerouteNotice != null) {
            foregroundController.sendOffRouteNotification(rerouteNotice);
        }
        routeExecutor.requestRoute(this, snapshot, routeCallback);
    }

    private void dispatchTurnEvents(@NonNull List<NavigationSession.TurnEvent> turnEvents) {
        if (turnEventDispatcher != null) {
            turnEventDispatcher.dispatch(turnEvents);
        }
    }

    private void emitState() {
        NavState s = navigationSession.buildState(
                this,
                locationController.getNextEvaluationDeadlineElapsedMs(),
                System.currentTimeMillis()
        );
        stateBroadcaster.dispatch(s);
    }

    @Override
    public void onDestroy() {
        AppLogger.i(TAG, "Service destroyed");
        stopNavigation();
        routeExecutor.shutdown();
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
        public void sendPassedTurnNotification(@NonNull com.vibenavigator.nav.route.VoiceHint hint) {
            foregroundController.sendPassedTurnNotification(hint);
        }

        @Override
        public void sendImminentTurnNotification(
                @NonNull com.vibenavigator.nav.route.VoiceHint hint,
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
                @NonNull com.vibenavigator.nav.route.GeoJsonRoute newRoute,
                long beganAt
        ) {
            dispatchTurnEvents(navigationSession.applyRouteResult(NavigationService.this, snapshot, newRoute, beganAt));
            emitState();
        }

        @Override
        public void onRouteFailure(
                @NonNull NavigationSession.RouteRequestSnapshot snapshot,
                @NonNull Exception error
        ) {
            navigationSession.applyRouteFailure(NavigationService.this, snapshot, error);
            emitState();
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
