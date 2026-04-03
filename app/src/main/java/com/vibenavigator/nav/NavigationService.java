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
import com.vibenavigator.brouter.BRouterRouter;
import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.VoiceHint;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final List<Listener> listeners = new ArrayList<>();

    private final ExecutorService routeExecutor = Executors.newSingleThreadExecutor();
    private final BRouterRouter router = new BRouterRouter();
    private final NavigationLifecyclePolicy lifecyclePolicy = new NavigationLifecyclePolicy();
    private final NavigationSession navigationSession = new NavigationSession();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler notificationMonitorHandler = new Handler(Looper.getMainLooper());
    private NavigationForegroundController foregroundController;
    private NavigationLocationController locationController;
    private NavigationWakeLockController wakeLockController;
    private final Runnable notificationMonitor = new Runnable() {
        @Override
        public void run() {
            NavigationLifecyclePolicy.ForegroundAction action =
                    lifecyclePolicy.onForegroundNotificationCheck(foregroundController.isOngoingNotificationVisible());
            if (action == NavigationLifecyclePolicy.ForegroundAction.STOP_NAVIGATION) {
                AppLogger.w(TAG, "Foreground notification is missing, stopping navigation");
                stopNavigation();
                stopSelf();
                return;
            }
            notificationMonitorHandler.postDelayed(this, FOREGROUND_NOTIFICATION_CHECK_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        foregroundController = new NavigationForegroundController(this);
        locationController = new NavigationLocationController(this, this);
        wakeLockController = new NavigationWakeLockController(this);
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
            if (!listeners.contains(l)) {
                listeners.add(l);
                AppLogger.d(TAG, "Listener registered totalListeners=" + listeners.size());
            }
            emitState();
        }

        public void ensureForegroundNotification() {
            NavigationLifecyclePolicy.ForegroundAction action =
                    lifecyclePolicy.onNavigationUiConnected(foregroundController.isOngoingNotificationVisible());
            if (action == NavigationLifecyclePolicy.ForegroundAction.PROMOTE_TO_FOREGROUND) {
                AppLogger.i(TAG, "Foreground notification refresh requested through binder");
                promoteToForeground();
            }
        }

        public void unregisterListener(@NonNull Listener l) {
            listeners.remove(l);
            AppLogger.d(TAG, "Listener unregistered totalListeners=" + listeners.size());
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
            requestRouteRecalc(true);
        }

        public void stop() {
            AppLogger.i(TAG, "Stop requested through binder");
            stopNavigation();
            stopSelf();
        }
    }

    private void promoteToForeground() {
        foregroundController.promoteToForeground(navigationSession.currentNavigationRequest());
        startNotificationMonitor();
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
        AppLogger.i(TAG, "Stopping navigation listeners=" + listeners.size()
                + " routeLoaded=" + navigationSession.hasActiveRoute());
        navigationSession.stop();
        stopNotificationMonitor();
        locationController.stopTracking();
        wakeLockController.release();
        listeners.clear();
        foregroundController.stopForegroundService();
    }

    private void startNotificationMonitor() {
        stopNotificationMonitor();
        notificationMonitorHandler.postDelayed(notificationMonitor, FOREGROUND_NOTIFICATION_CHECK_INTERVAL_MS);
    }

    private void stopNotificationMonitor() {
        notificationMonitorHandler.removeCallbacks(notificationMonitor);
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        NavigationSession.LocationUpdateResult result =
                navigationSession.onRawLocationChanged(this, location, System.currentTimeMillis());
        if (result.isDropped()) {
            return;
        }
        if (result.shouldRecalculateRoute()) {
            requestRouteRecalc(false);
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

    private void requestRouteRecalc(boolean force) {
        NavigationSession.RouteRequestSnapshot snapshot =
                navigationSession.prepareRouteRequest(force, System.currentTimeMillis());
        if (snapshot == null) {
            return;
        }
        emitState();

        routeExecutor.submit(() -> {
            long beganAt = System.currentTimeMillis();
            try {
                GeoJsonRoute newRoute = router.routeGeoJson(
                        getApplicationContext(),
                        snapshot.start,
                        snapshot.intermediates,
                        snapshot.destination,
                        snapshot.profile,
                        snapshot.blocked
                );
                if (newRoute.track.isEmpty()) {
                    throw new IllegalStateException("BRouter returned an empty route");
                }
                mainHandler.post(() -> {
                    dispatchTurnEvents(navigationSession.applyRouteResult(this, snapshot, newRoute, beganAt));
                    emitState();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    navigationSession.applyRouteFailure(this, snapshot, e);
                    emitState();
                });
            }
        });
    }

    private void dispatchTurnEvents(@NonNull List<NavigationSession.TurnEvent> turnEvents) {
        for (NavigationSession.TurnEvent event : turnEvents) {
            switch (event.type) {
                case PASSED:
                    AppLogger.i(TAG, "Passed voice hint hintTrackIndex=" + event.hint.indexInTrack);
                    foregroundController.sendPassedTurnNotification(event.hint);
                    break;
                case INITIAL:
                    AppLogger.i(TAG, "Sent initial turn notification distanceMeters=" + event.distanceMeters
                            + " timeSeconds=" + event.timeSeconds);
                    notifyImminent(event.hint, event.distanceMeters, event.timeSeconds);
                    break;
                case IMMINENT:
                    notifyImminent(event.hint, event.distanceMeters, event.timeSeconds);
                    break;
            }
        }
    }

    private void notifyImminent(@NonNull VoiceHint hint, double distMeters, double timeSeconds) {
        com.vibenavigator.nav.directions.DirectionInfo directionInfo =
                com.vibenavigator.nav.directions.VoiceHintMapper.toDirection(hint);
        AppLogger.i(TAG, "Imminent turn kind=" + directionInfo.kind
                + " distanceMeters=" + distMeters
                + " timeSeconds=" + timeSeconds);
        foregroundController.sendImminentTurnNotification(hint, distMeters, timeSeconds);
    }

    private void emitState() {
        NavState s = navigationSession.buildState(
                this,
                locationController.getNextEvaluationDeadlineElapsedMs(),
                System.currentTimeMillis()
        );
        for (Listener l : new ArrayList<>(listeners)) {
            try {
                l.onState(s);
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    @Override
    public void onDestroy() {
        AppLogger.i(TAG, "Service destroyed");
        stopNavigation();
        routeExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (lifecyclePolicy.onTaskRemoved() == NavigationLifecyclePolicy.TaskRemovedAction.STOP_NAVIGATION) {
            AppLogger.i(TAG, "Task removed, stopping navigation service");
            stopNavigation();
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
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
