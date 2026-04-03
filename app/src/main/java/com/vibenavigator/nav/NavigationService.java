package com.vibenavigator.nav;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.vibenavigator.MainActivity;
import com.vibenavigator.NavigationActivity;
import com.vibenavigator.R;
import com.vibenavigator.brouter.BRouterRouter;
import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.nav.directions.DirectionInfo;
import com.vibenavigator.nav.directions.DirectionKind;
import com.vibenavigator.nav.directions.VoiceHintMapper;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.VoiceHint;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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
    private final Runnable notificationMonitor = new Runnable() {
        @Override
        public void run() {
            NavigationLifecyclePolicy.ForegroundAction action =
                    lifecyclePolicy.onForegroundNotificationCheck(isOngoingNotificationVisible());
            if (action == NavigationLifecyclePolicy.ForegroundAction.STOP_NAVIGATION) {
                AppLogger.w(TAG, "Foreground notification is missing, stopping navigation");
                stopNavigation();
                stopSelf();
                return;
            }
            notificationMonitorHandler.postDelayed(this, FOREGROUND_NOTIFICATION_CHECK_INTERVAL_MS);
        }
    };

    private LocationManager locationManager;
    private Executor locationCallbackExecutor;
    private PowerManager.WakeLock wakeLock;

    private long lastRequestedLocationMinTimeMs = -1L;
    @Nullable
    private String lastRequestedProvider;
    @Nullable
    private CancellationSignal gpsCurrentLocationCancellation;
    @Nullable
    private CancellationSignal networkCurrentLocationCancellation;
    private long nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationCallbackExecutor = ContextCompat.getMainExecutor(this);
        ensureChannels();
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
                    lifecyclePolicy.onNavigationUiConnected(isOngoingNotificationVisible());
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

    private void ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) {
            AppLogger.w(TAG, "NotificationManager unavailable while creating channels");
            return;
        }

        NotificationChannel nav = new NotificationChannel(
                CHANNEL_ID_NAV,
                getString(R.string.notification_channel_nav),
                NotificationManager.IMPORTANCE_LOW
        );
        nav.enableVibration(false);
        nm.createNotificationChannel(nav);

        NotificationChannel left = new NotificationChannel(
                CHANNEL_ID_TURN_LEFT,
                getString(R.string.notification_channel_turn_left),
                NotificationManager.IMPORTANCE_HIGH
        );
        left.enableVibration(true);
        left.setVibrationPattern(new long[]{0, 80, 80, 220});
        nm.createNotificationChannel(left);

        NotificationChannel right = new NotificationChannel(
                CHANNEL_ID_TURN_RIGHT,
                getString(R.string.notification_channel_turn_right),
                NotificationManager.IMPORTANCE_HIGH
        );
        right.enableVibration(true);
        right.setVibrationPattern(new long[]{0, 220, 80, 80});
        nm.createNotificationChannel(right);
        AppLogger.i(TAG, "Notification channels ensured");
    }

    private Notification buildOngoingNotification() {
        Intent stopNavigationIntent = new Intent(this, NavigationService.class);
        stopNavigationIntent.setAction(ACTION_STOP);
        PendingIntent stopNavigationPendingIntent = PendingIntent.getService(
                this,
                1,
                stopNavigationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent openNavigationIntent = new Intent(this, MainActivity.class);
        openNavigationIntent.putExtra(MainActivity.EXTRA_OPEN_NAVIGATION, true);
        openNavigationIntent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        openNavigationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        navigationSession.currentNavigationRequest().putInto(openNavigationIntent);
        PendingIntent openNavigationPendingIntent = PendingIntent.getActivity(
                this,
                0,
                openNavigationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID_NAV)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(getString(R.string.notification_nav_running))
                .setContentText(getString(R.string.notification_nav_running_text))
                .setOngoing(true)
                .setDeleteIntent(stopNavigationPendingIntent)
                .setContentIntent(openNavigationPendingIntent)
                .build();
    }

    private void promoteToForeground() {
        startForeground(NOTIFICATION_ID_ONGOING, buildOngoingNotification());
        startNotificationMonitor();
    }

    private void readNavRequest(@NonNull Intent intent) {
        NavigationRequest request = NavigationRequest.fromIntent(intent);
        navigationSession.loadRequest(request);
    }

    private void startNavigation() {
        lastRequestedLocationMinTimeMs = -1L;
        lastRequestedProvider = null;
        nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
        cancelPendingCurrentLocationRequests();

        if (!navigationSession.start(this, System.currentTimeMillis())) {
            emitState();
            return;
        }

        acquireWakeLock();
        requestLocationUpdates(2000L);
        requestCurrentLocationSeeds();
        emitState();
        NavigationRequest request = navigationSession.currentNavigationRequest();
        AppLogger.i(TAG, "Navigation started " + request.describe() + " blockedReset=true");

        Location seed = getBestLastKnownLocation();
        if (seed != null) {
            AppLogger.i(TAG, "Using last known location as seed " + formatLocation(seed));
            onLocationChanged(seed);
        } else {
            AppLogger.w(TAG, "No last known location available at navigation start "
                    + describeLocationAvailability());
        }
    }

    private void stopNavigation() {
        AppLogger.i(TAG, "Stopping navigation listeners=" + listeners.size()
                + " routeLoaded=" + navigationSession.hasActiveRoute());
        navigationSession.stop();
        stopNotificationMonitor();
        cancelPendingCurrentLocationRequests();
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Failed to remove location updates", e);
        }
        releaseWakeLock();
        listeners.clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            //noinspection deprecation
            stopForeground(true);
        }
    }

    private void startNotificationMonitor() {
        stopNotificationMonitor();
        notificationMonitorHandler.postDelayed(notificationMonitor, FOREGROUND_NOTIFICATION_CHECK_INTERVAL_MS);
    }

    private void stopNotificationMonitor() {
        notificationMonitorHandler.removeCallbacks(notificationMonitor);
    }

    private boolean isOngoingNotificationVisible() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) {
            AppLogger.w(TAG, "NotificationManager unavailable while checking foreground notification");
            return true;
        }
        try {
            StatusBarNotification[] notifications = nm.getActiveNotifications();
            for (StatusBarNotification notification : notifications) {
                if (notification.getId() == NOTIFICATION_ID_ONGOING) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to query active notifications", e);
            return true;
        }
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm == null) {
                AppLogger.w(TAG, "PowerManager unavailable, wake lock not acquired");
                return;
            }
            if (wakeLock != null && wakeLock.isHeld()) {
                AppLogger.d(TAG, "Wake lock already held");
                return;
            }
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VibeNavigator:Nav");
            wakeLock.acquire();
            AppLogger.i(TAG, "Wake lock acquired");
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to acquire wake lock", e);
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                AppLogger.i(TAG, "Wake lock released");
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to release wake lock", e);
        } finally {
            wakeLock = null;
        }
    }

    private void requestLocationUpdates(long minTimeMs) {
        try {
            if (locationManager == null) {
                AppLogger.w(TAG, "LocationManager unavailable, cannot request updates");
                return;
            }
            locationManager.removeUpdates(this);
            List<String> providers = new ArrayList<>(2);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                requestProviderUpdates(LocationManager.GPS_PROVIDER, minTimeMs);
                providers.add(LocationManager.GPS_PROVIDER);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                requestProviderUpdates(LocationManager.NETWORK_PROVIDER, minTimeMs);
                providers.add(LocationManager.NETWORK_PROVIDER);
            }
            if (providers.isEmpty()) {
                nextEvaluationDeadlineElapsedMs = NavState.NO_DEADLINE;
                AppLogger.w(TAG, "No enabled location provider available for updates "
                        + describeLocationAvailability());
            } else {
                nextEvaluationDeadlineElapsedMs = SystemClock.elapsedRealtime() + minTimeMs;
            }
            String provider = joinProviders(providers);
            if (provider != null && (minTimeMs != lastRequestedLocationMinTimeMs || !provider.equals(lastRequestedProvider))) {
                lastRequestedLocationMinTimeMs = minTimeMs;
                lastRequestedProvider = provider;
                AppLogger.i(TAG, "Requested location updates provider=" + provider + " minTimeMs=" + minTimeMs);
            }
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while requesting location updates", e);
        }
    }

    private void requestProviderUpdates(@NonNull String provider, long minTimeMs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationManager.requestLocationUpdates(provider, minTimeMs, 0f, locationCallbackExecutor, this);
        } else {
            locationManager.requestLocationUpdates(provider, minTimeMs, 0f, this);
        }
    }

    private void requestCurrentLocationSeeds() {
        if (locationManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsCurrentLocationCancellation = requestCurrentLocationSeed(LocationManager.GPS_PROVIDER);
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            networkCurrentLocationCancellation = requestCurrentLocationSeed(LocationManager.NETWORK_PROVIDER);
        }
    }

    @Nullable
    private CancellationSignal requestCurrentLocationSeed(@NonNull String provider) {
        CancellationSignal cancellationSignal = new CancellationSignal();
        Consumer<Location> consumer = location -> {
            clearCurrentLocationCancellation(provider, cancellationSignal);
            if (location == null) {
                AppLogger.d(TAG, "Current location seed returned null provider=" + provider);
                return;
            }
            AppLogger.i(TAG, "Received current location seed provider=" + provider
                    + " location=" + formatLocation(location));
            onLocationChanged(location);
        };
        try {
            locationManager.getCurrentLocation(provider, cancellationSignal, locationCallbackExecutor, consumer);
            AppLogger.d(TAG, "Requested current location seed provider=" + provider);
            return cancellationSignal;
        } catch (SecurityException e) {
            clearCurrentLocationCancellation(provider, cancellationSignal);
            AppLogger.w(TAG, "Permission denied while requesting current location seed provider=" + provider, e);
            return null;
        } catch (Exception e) {
            clearCurrentLocationCancellation(provider, cancellationSignal);
            AppLogger.w(TAG, "Failed to request current location seed provider=" + provider, e);
            return null;
        }
    }

    private void clearCurrentLocationCancellation(@NonNull String provider,
                                                  @NonNull CancellationSignal cancellationSignal) {
        if (LocationManager.GPS_PROVIDER.equals(provider) && gpsCurrentLocationCancellation == cancellationSignal) {
            gpsCurrentLocationCancellation = null;
        } else if (LocationManager.NETWORK_PROVIDER.equals(provider)
                && networkCurrentLocationCancellation == cancellationSignal) {
            networkCurrentLocationCancellation = null;
        }
    }

    private void cancelPendingCurrentLocationRequests() {
        if (gpsCurrentLocationCancellation != null) {
            gpsCurrentLocationCancellation.cancel();
            gpsCurrentLocationCancellation = null;
        }
        if (networkCurrentLocationCancellation != null) {
            networkCurrentLocationCancellation.cancel();
            networkCurrentLocationCancellation = null;
        }
    }

    @Nullable
    private Location getBestLastKnownLocation() {
        try {
            if (locationManager == null) {
                return null;
            }
            Location best = null;
            Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location net = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (gps != null) {
                best = gps;
            }
            if (net != null && (best == null || net.getTime() > best.getTime())) {
                best = net;
            }
            AppLogger.d(TAG, "Best last known location=" + formatLocation(best));
            return best;
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while reading last known location", e);
            return null;
        }
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
            requestLocationUpdates(result.getSuggestedUpdateIntervalMs());
        }
        dispatchTurnEvents(result.turnEvents);
        emitState();
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        AppLogger.i(TAG, "Location provider enabled provider=" + provider);
        long minTimeMs = lastRequestedLocationMinTimeMs > 0 ? lastRequestedLocationMinTimeMs : 2000L;
        requestLocationUpdates(minTimeMs);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                gpsCurrentLocationCancellation = requestCurrentLocationSeed(provider);
            } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
                networkCurrentLocationCancellation = requestCurrentLocationSeed(provider);
            }
        }
        emitState();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        AppLogger.w(TAG, "Location provider disabled provider=" + provider);
        navigationSession.onProviderDisabled(provider);
        long minTimeMs = lastRequestedLocationMinTimeMs > 0 ? lastRequestedLocationMinTimeMs : 2000L;
        requestLocationUpdates(minTimeMs);
        emitState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStatusChanged(@Nullable String provider, int status, @Nullable Bundle extras) {
        AppLogger.d(TAG, "Location provider status changed provider=" + provider + " status=" + status);
    }

    @Nullable
    private String joinProviders(@NonNull List<String> providers) {
        if (providers.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < providers.size(); i++) {
            if (i > 0) {
                sb.append("+");
            }
            sb.append(providers.get(i));
        }
        return sb.toString();
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
                    sendTurnNotification(event.hint, 0, 0, CHANNEL_ID_NAV, false);
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
        DirectionInfo directionInfo = VoiceHintMapper.toDirection(hint);
        AppLogger.i(TAG, "Imminent turn kind=" + directionInfo.kind
                + " distanceMeters=" + distMeters
                + " timeSeconds=" + timeSeconds);
        String channel = directionInfo.kind == DirectionKind.LEFT
                ? CHANNEL_ID_TURN_LEFT
                : (directionInfo.kind == DirectionKind.RIGHT ? CHANNEL_ID_TURN_RIGHT : CHANNEL_ID_NAV);
        boolean vibrate = directionInfo.kind == DirectionKind.LEFT || directionInfo.kind == DirectionKind.RIGHT;
        sendTurnNotification(hint, distMeters, timeSeconds, channel, vibrate);
    }

    private void sendTurnNotification(@NonNull VoiceHint hint, double distMeters, double timeSeconds, @NonNull String channelId, boolean vibrate) {
        DirectionInfo di = VoiceHintMapper.toDirection(hint);
        String msg = NavigationTextFormatter.formatTurnNotification(this, hint, distMeters, timeSeconds);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(msg)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && vibrate) {
            if (di.kind == DirectionKind.LEFT) {
                b.setVibrate(new long[]{0, 80, 80, 220});
            } else if (di.kind == DirectionKind.RIGHT) {
                b.setVibrate(new long[]{0, 220, 80, 80});
            }
        }

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) {
            AppLogger.w(TAG, "NotificationManager unavailable, cannot send turn notification");
            return;
        }
        nm.notify(NOTIFICATION_ID_TURN, b.build());
        AppLogger.d(TAG, "Sent turn notification channel=" + channelId
                + " vibrate=" + vibrate
                + " notificationId=" + NOTIFICATION_ID_TURN
                + " message=" + msg);
    }

    private void emitState() {
        NavState s = navigationSession.buildState(this, nextEvaluationDeadlineElapsedMs, System.currentTimeMillis());
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

    @NonNull
    private String describeLocationAvailability() {
        if (locationManager == null) {
            return "locationManager=null";
        }
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read GPS provider state", e);
        }
        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read network provider state", e);
        }
        return "gpsEnabled=" + gpsEnabled
                + ", networkEnabled=" + networkEnabled
                + ", lastGps=" + formatLocation(getLastKnownLocationQuietly(LocationManager.GPS_PROVIDER))
                + ", lastNetwork=" + formatLocation(getLastKnownLocationQuietly(LocationManager.NETWORK_PROVIDER));
    }

    @Nullable
    private Location getLastKnownLocationQuietly(@NonNull String provider) {
        if (locationManager == null) {
            return null;
        }
        try {
            return locationManager.getLastKnownLocation(provider);
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while reading last known location provider=" + provider, e);
            return null;
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read last known location provider=" + provider, e);
            return null;
        }
    }

}
