package com.vibenavigator.nav;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.vibenavigator.R;
import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.util.AppLogger;

import java.util.List;

public class NavigationService extends Service implements LocationListener {

    private static final String TAG = "NavigationService";
    private static final long FOREGROUND_NOTIFICATION_CHECK_INTERVAL_MS = 5_000L;
    private static final long DEFAULT_LOCATION_UPDATE_INTERVAL_MS = 1_000L;
    private static final long MAX_COMPASS_HEADING_SAMPLE_AGE_MS = 5_000L;
    private static final long MIN_COMPASS_UI_UPDATE_INTERVAL_MS = 100L;

    public interface Listener {
        void onState(@NonNull NavState state);
    }

    public static final String ACTION_START = "com.vibenavigator.action.START";
    public static final String ACTION_STOP = "com.vibenavigator.action.STOP";

    public static final int NOTIFICATION_ID_ONGOING = 1;
    public static final int NOTIFICATION_ID_TURN = 2;
    public static final String CHANNEL_ID_NAV = "vibenavigator.navigation";
    public static final String CHANNEL_ID_ALERT = "vibenavigator.alerts.v1";
    public static final String CHANNEL_ID_TURN_LEFT = "vibenavigator.turn.left.v2";
    public static final String CHANNEL_ID_TURN_RIGHT = "vibenavigator.turn.right.v2";

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
    private final StationaryOrientationAdvisor stationaryOrientationAdvisor = new StationaryOrientationAdvisor();
    private NavigationForegroundController foregroundController;
    private NavigationLocationController locationController;
    private NavigationTurnEventDispatcher turnEventDispatcher;
    private GeomagneticOrientationMonitor geomagneticOrientationMonitor;
    private long stationarySinceElapsedRealtimeMs;
    private boolean stationaryOrientationHandledForCurrentStop;
    private long lastCompassUiUpdateElapsedRealtimeMs;
    private boolean navigationUiVisible;
    private boolean screenInteractive = true;
    private boolean orientationMonitoringActive;
    private final BroadcastReceiver screenInteractiveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                onScreenInteractiveChanged(false);
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                onScreenInteractiveChanged(true);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        foregroundController = new NavigationForegroundController(this);
        locationController = new NavigationLocationController(this, this);
        routeExecutor = NavigationRouteExecutor.createDefault(this, notificationMonitorHandler);
        turnEventDispatcher = new NavigationTurnEventDispatcher(new ForegroundNotificationSink());
        geomagneticOrientationMonitor = new GeomagneticOrientationMonitor(this, this::onGeomagneticSampleUpdated);
        screenInteractive = isScreenInteractive();
        IntentFilter screenInteractiveFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        screenInteractiveFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenInteractiveReceiver, screenInteractiveFilter);
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
        ensureOrientationMonitoring();
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
        stopOrientationMonitoring();
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
        ensureOrientationMonitoring();
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
        stopOrientationMonitoring();
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
        maybeSendStationaryOrientationNotification();
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

    private void ensureOrientationMonitoring() {
        resetStationaryOrientationEpisode();
        if (orientationMonitoringActive || geomagneticOrientationMonitor == null) {
            return;
        }
        if (!geomagneticOrientationMonitor.start()) {
            AppLogger.w(TAG, "Stationary orientation monitor unavailable, skipping stationary orientation notifications");
            return;
        }
        orientationMonitoringActive = true;
    }

    private void stopOrientationMonitoring() {
        stationarySinceElapsedRealtimeMs = 0L;
        stationaryOrientationHandledForCurrentStop = false;
        lastCompassUiUpdateElapsedRealtimeMs = 0L;
        if (!orientationMonitoringActive) {
            return;
        }
        orientationMonitoringActive = false;
        if (geomagneticOrientationMonitor != null) {
            geomagneticOrientationMonitor.stop();
        }
    }

    private void maybeSendStationaryOrientationNotification() {
        if (foregroundController == null || geomagneticOrientationMonitor == null) {
            return;
        }
        if (!shouldEvaluateStationaryOrientation(
                navigationSession.hasActiveRoute(),
                navigationSession.isRouteCalculationInProgress()
        )) {
            resetStationaryOrientationEpisode();
            return;
        }

        float speedMps = navigationSession.lastFilteredSpeedMps();
        if (!navigationSession.isLikelyStationaryForOrientation()) {
            resetStationaryOrientationEpisode();
            return;
        }

        long nowElapsedRealtimeMs = android.os.SystemClock.elapsedRealtime();
        if (stationarySinceElapsedRealtimeMs <= 0L) {
            stationarySinceElapsedRealtimeMs = nowElapsedRealtimeMs;
            stationaryOrientationHandledForCurrentStop = false;
        }
        if (stationaryOrientationHandledForCurrentStop) {
            return;
        }

        StationaryOrientationAdvisor.Evaluation evaluation = stationaryOrientationAdvisor.evaluate(
                speedMps,
                stationarySinceElapsedRealtimeMs,
                navigationSession.currentRouteBearingDegrees(),
                geomagneticOrientationMonitor.getLatestSample(),
                nowElapsedRealtimeMs
        );
        switch (evaluation.outcome) {
            case ALIGNED:
                stationaryOrientationHandledForCurrentStop = true;
                AppLogger.i(TAG, "Stationary orientation notification skipped because the user is already aligned");
                return;
            case NOTIFY:
                if (evaluation.decision != null) {
                    foregroundController.sendStationaryOrientationNotification(evaluation.decision);
                    stationaryOrientationHandledForCurrentStop = true;
                }
                return;
            case MOVING:
                resetStationaryOrientationEpisode();
                return;
            case WAITING_FOR_DWELL:
            case WAITING_FOR_ROUTE:
            case WAITING_FOR_SENSOR:
            case WAITING_FOR_CALIBRATION:
            default:
                return;
        }
    }

    private void resetStationaryOrientationEpisode() {
        stationarySinceElapsedRealtimeMs = 0L;
        stationaryOrientationHandledForCurrentStop = false;
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
        return shouldDispatchCompassUi(
                navigationSession.hasActiveRoute(),
                navigationUiVisible,
                screenInteractive
        );
    }

    static boolean shouldDispatchCompassUi(
            boolean hasActiveRoute,
            boolean navigationUiVisible,
            boolean screenInteractive
    ) {
        return hasActiveRoute && navigationUiVisible && screenInteractive;
    }

    static boolean shouldEvaluateStationaryOrientation(
            boolean hasActiveRoute,
            boolean routeCalculationInProgress
    ) {
        return hasActiveRoute && !routeCalculationInProgress;
    }

    private boolean isScreenInteractive() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        return powerManager == null || powerManager.isInteractive();
    }

    private void onGeomagneticSampleUpdated(@NonNull GeomagneticOrientationMonitor.Sample sample) {
        if (!shouldDispatchCompassUi() || stateBroadcaster.size() == 0) {
            return;
        }
        long nowElapsedRealtimeMs = android.os.SystemClock.elapsedRealtime();
        if (nowElapsedRealtimeMs - lastCompassUiUpdateElapsedRealtimeMs < MIN_COMPASS_UI_UPDATE_INTERVAL_MS) {
            return;
        }
        lastCompassUiUpdateElapsedRealtimeMs = nowElapsedRealtimeMs;
        notificationMonitorHandler.post(this::emitState);
    }

    private void emitState() {
        NavState s = navigationSession.buildState(
                this,
                locationController.getNextEvaluationDeadlineElapsedMs(),
                System.currentTimeMillis(),
                locationController.getFixedSatelliteCount(),
                currentDisplayHeadingDegrees(),
                currentDisplayHeadingAccuracyDegrees()
        );
        stateBroadcaster.dispatch(s);
    }

    @Nullable
    private Double currentDisplayHeadingDegrees() {
        GeomagneticOrientationMonitor.Sample sample = currentDisplayHeadingSample();
        if (sample == null) {
            return null;
        }
        return remapHeadingDegreesForDisplayRotation(sample.headingDegrees, currentDisplayRotation());
    }

    @Nullable
    private Float currentDisplayHeadingAccuracyDegrees() {
        GeomagneticOrientationMonitor.Sample sample = currentDisplayHeadingSample();
        if (sample == null) {
            return null;
        }
        if (sample.headingAccuracyDegrees != null) {
            return sample.headingAccuracyDegrees.floatValue();
        }
        switch (sample.accuracy) {
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return 10f;
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return 20f;
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return 35f;
            case android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE:
            default:
                return null;
        }
    }

    @Nullable
    private GeomagneticOrientationMonitor.Sample currentDisplayHeadingSample() {
        if (!orientationMonitoringActive || geomagneticOrientationMonitor == null) {
            return null;
        }
        GeomagneticOrientationMonitor.Sample sample = geomagneticOrientationMonitor.getLatestSample();
        if (sample == null) {
            return null;
        }
        long ageMs = android.os.SystemClock.elapsedRealtime() - sample.elapsedRealtimeMs;
        if (ageMs > MAX_COMPASS_HEADING_SAMPLE_AGE_MS) {
            return null;
        }
        return sample;
    }

    static double remapHeadingDegreesForDisplayRotation(double headingDegrees, int displayRotation) {
        double rotationOffsetDegrees;
        switch (displayRotation) {
            case Surface.ROTATION_90:
                rotationOffsetDegrees = 90.0;
                break;
            case Surface.ROTATION_180:
                rotationOffsetDegrees = 180.0;
                break;
            case Surface.ROTATION_270:
                rotationOffsetDegrees = 270.0;
                break;
            case Surface.ROTATION_0:
            default:
                rotationOffsetDegrees = 0.0;
                break;
        }
        double normalized = (headingDegrees + rotationOffsetDegrees) % 360.0;
        return normalized < 0.0 ? normalized + 360.0 : normalized;
    }

    private int currentDisplayRotation() {
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager == null) {
            return Surface.ROTATION_0;
        }
        Display defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (defaultDisplay == null) {
            return Surface.ROTATION_0;
        }
        return defaultDisplay.getRotation();
    }

    @Override
    public void onDestroy() {
        AppLogger.i(TAG, "Service destroyed");
        stopNavigation();
        unregisterReceiver(screenInteractiveReceiver);
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
            maybeSendStationaryOrientationNotification();
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
