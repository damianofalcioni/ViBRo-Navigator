package com.vibenavigator.nav;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.vibenavigator.NavigationActivity;
import com.vibenavigator.R;
import com.vibenavigator.brouter.BRouterRouter;
import com.vibenavigator.geo.GeoMath;
import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.directions.DirectionInfo;
import com.vibenavigator.nav.directions.DirectionKind;
import com.vibenavigator.nav.directions.VoiceHintMapper;
import com.vibenavigator.nav.kalman.LatLonKalmanFilter;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.PolylineIndex;
import com.vibenavigator.nav.route.VoiceHint;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NavigationService extends Service implements LocationListener {

    private static final String TAG = "NavigationService";

    public interface Listener {
        void onState(@NonNull NavState state);
    }

    public static final String ACTION_START = "com.vibenavigator.action.START";
    public static final String ACTION_STOP = "com.vibenavigator.action.STOP";

    public static final int NOTIFICATION_ID_ONGOING = 1;
    public static final String CHANNEL_ID_NAV = "vibenavigator.navigation";
    public static final String CHANNEL_ID_TURN_LEFT = "vibenavigator.turn.left";
    public static final String CHANNEL_ID_TURN_RIGHT = "vibenavigator.turn.right";

    private final IBinder binder = new LocalBinder();
    private final List<Listener> listeners = new ArrayList<>();

    private final ExecutorService routeExecutor = Executors.newSingleThreadExecutor();
    private final BRouterRouter router = new BRouterRouter();
    private final LatLonKalmanFilter kalman = new LatLonKalmanFilter();

    private LocationManager locationManager;
    private PowerManager.WakeLock wakeLock;

    private String profile;
    private LatLon destination;
    private List<LatLon> intermediates = new ArrayList<>();
    private final List<LatLon> blocked = new ArrayList<>();

    private Location lastFiltered;
    private Location prevFiltered;

    private GeoJsonRoute route;
    private PolylineIndex polylineIndex;
    private int lastSegmentIndex = -1;
    private int nextHintIdx = 0;
    private boolean notified10;
    private boolean notified5;
    private long fastChecksUntilMs;
    private long lastRerouteMs;
    private List<NavTarget> targets = new ArrayList<>();
    private int locationUpdateCount;
    private int routeRequestCount;
    private long lastRequestedLocationMinTimeMs = -1L;
    @Nullable
    private String lastRequestedProvider;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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

        startForeground(NOTIFICATION_ID_ONGOING, buildOngoingNotification());
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

        public void unregisterListener(@NonNull Listener l) {
            listeners.remove(l);
            AppLogger.d(TAG, "Listener unregistered totalListeners=" + listeners.size());
        }

        public void addBlockedWaypoint() {
            Location loc = lastFiltered;
            if (loc == null) {
                AppLogger.w(TAG, "Blocked waypoint requested without a current filtered location");
                return;
            }
            blocked.add(new LatLon(loc.getLatitude(), loc.getLongitude()));
            AppLogger.i(TAG, "Blocked waypoint added count=" + blocked.size()
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
        return new NotificationCompat.Builder(this, CHANNEL_ID_NAV)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(getString(R.string.notification_nav_running))
                .setContentText(getString(R.string.notification_nav_running_text))
                .setOngoing(true)
                .build();
    }

    private void readNavRequest(@NonNull Intent intent) {
        profile = intent.getStringExtra(NavigationActivity.EXTRA_PROFILE);
        double lat = intent.getDoubleExtra(NavigationActivity.EXTRA_DEST_LAT, Double.NaN);
        double lon = intent.getDoubleExtra(NavigationActivity.EXTRA_DEST_LON, Double.NaN);
        if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
            destination = new LatLon(lat, lon);
        }
        ArrayList<String> stops = intent.getStringArrayListExtra(NavigationActivity.EXTRA_STOPS);
        intermediates = new ArrayList<>();
        if (stops != null) {
            for (String s : stops) {
                LatLon ll = parseLatLon(s);
                if (ll != null) {
                    intermediates.add(ll);
                } else {
                    AppLogger.w(TAG, "Discarded invalid intermediate stop=" + s);
                }
            }
        }
        AppLogger.i(TAG, "Navigation request loaded profile=" + profile
                + " destination=" + formatLatLon(destination)
                + " intermediates=" + intermediates.size());
    }

    private void startNavigation() {
        blocked.clear();
        route = null;
        polylineIndex = null;
        lastSegmentIndex = -1;
        nextHintIdx = 0;
        notified10 = false;
        notified5 = false;
        targets = new ArrayList<>();
        fastChecksUntilMs = System.currentTimeMillis() + 30_000L;
        lastRerouteMs = 0;
        lastRequestedLocationMinTimeMs = -1L;
        lastRequestedProvider = null;
        locationUpdateCount = 0;
        routeRequestCount = 0;

        acquireWakeLock();
        requestLocationUpdates(2000L);
        emitState();
        AppLogger.i(TAG, "Navigation started profile=" + profile
                + " destination=" + formatLatLon(destination)
                + " intermediates=" + intermediates.size()
                + " blockedReset=true");

        Location seed = getBestLastKnownLocation();
        if (seed != null) {
            AppLogger.i(TAG, "Using last known location as seed " + formatLocation(seed));
            onLocationChanged(seed);
        } else {
            AppLogger.w(TAG, "No last known location available at navigation start");
        }
    }

    private void stopNavigation() {
        AppLogger.i(TAG, "Stopping navigation listeners=" + listeners.size()
                + " routeLoaded=" + (route != null));
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
            String provider = null;
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                provider = LocationManager.GPS_PROVIDER;
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, 0, this);
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                provider = LocationManager.NETWORK_PROVIDER;
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeMs, 0, this);
            } else {
                AppLogger.w(TAG, "No enabled location provider available for updates");
            }
            if (provider != null && (minTimeMs != lastRequestedLocationMinTimeMs || !provider.equals(lastRequestedProvider))) {
                lastRequestedLocationMinTimeMs = minTimeMs;
                lastRequestedProvider = provider;
                AppLogger.i(TAG, "Requested location updates provider=" + provider + " minTimeMs=" + minTimeMs);
            }
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while requesting location updates", e);
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
        Location filtered = kalman.update(location);
        if (filtered == null) {
            AppLogger.d(TAG, "Kalman filter dropped location " + formatLocation(location));
            return;
        }
        prevFiltered = lastFiltered;
        lastFiltered = filtered;
        locationUpdateCount++;
        AppLogger.d(TAG, "Location update #" + locationUpdateCount
                + " raw=" + formatLocation(location)
                + " filtered=" + formatLocation(filtered));

        evaluateAndMaybeReroute();
        emitState();
    }

    private void evaluateAndMaybeReroute() {
        if (destination == null || profile == null || profile.trim().isEmpty()) {
            AppLogger.w(TAG, "Skipping route evaluation because destination/profile is incomplete");
            return;
        }
        Location loc = lastFiltered;
        if (loc == null) {
            AppLogger.d(TAG, "Skipping route evaluation because filtered location is unavailable");
            return;
        }

        if (route == null || polylineIndex == null || route.track.isEmpty()) {
            AppLogger.i(TAG, "No active route loaded, requesting route calculation");
            requestRouteRecalc(false);
            return;
        }

        LatLon p = new LatLon(loc.getLatitude(), loc.getLongitude());
        PolylineIndex.Match m = polylineIndex.match(p, lastSegmentIndex);
        if (m == null) {
            AppLogger.w(TAG, "Route match failed, requesting recalculation");
            requestRouteRecalc(false);
            return;
        }
        lastSegmentIndex = m.segmentIndex;

        double accuracy = loc.hasAccuracy() ? loc.getAccuracy() : 20.0;
        double offTrackThreshold = 10.0 + accuracy;
        if (m.distanceToTrackMeters > offTrackThreshold) {
            AppLogger.w(TAG, "Off-track detected distance=" + m.distanceToTrackMeters
                    + " threshold=" + offTrackThreshold);
            requestRouteRecalc(false);
            return;
        }

        double expectedBearing = m.segmentBearingDegrees;
        Double actualBearing = getActualBearingDegrees(loc);
        if (actualBearing != null) {
            double diff = GeoMath.angularDiffDegrees(actualBearing, expectedBearing);
            if (diff > 60.0) {
                AppLogger.w(TAG, "Bearing mismatch detected diff=" + diff
                        + " expected=" + expectedBearing
                        + " actual=" + actualBearing);
                requestRouteRecalc(false);
                return;
            }
        }

        advanceVoiceHints(m.alongTrackMeters, getSpeedMps(loc));
        adjustUpdateInterval(m.alongTrackMeters, getSpeedMps(loc));
    }

    private void requestRouteRecalc(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastRerouteMs < 8000L) {
            AppLogger.d(TAG, "Skipping route recalculation because of throttle elapsedMs=" + (now - lastRerouteMs));
            return;
        }
        lastRerouteMs = now;

        Location loc = lastFiltered;
        if (loc == null) {
            AppLogger.w(TAG, "Cannot recalculate route without a filtered location");
            return;
        }
        LatLon start = new LatLon(loc.getLatitude(), loc.getLongitude());
        int requestNumber = ++routeRequestCount;
        AppLogger.i(TAG, "Submitting route recalculation #" + requestNumber
                + " force=" + force
                + " start=" + formatLatLon(start)
                + " destination=" + formatLatLon(destination)
                + " intermediates=" + intermediates.size()
                + " blocked=" + blocked.size());

        routeExecutor.submit(() -> {
            long beganAt = System.currentTimeMillis();
            try {
                GeoJsonRoute newRoute = router.routeGeoJson(getApplicationContext(), start, intermediates, destination, profile, blocked);
                route = newRoute;
                polylineIndex = new PolylineIndex(newRoute.track);
                lastSegmentIndex = -1;
                nextHintIdx = 0;
                notified10 = false;
                notified5 = false;
                targets = buildTargets(polylineIndex);
                AppLogger.i(TAG, "Route recalculation #" + requestNumber
                        + " succeeded durationMs=" + (System.currentTimeMillis() - beganAt)
                        + " trackPoints=" + newRoute.track.size()
                        + " voiceHints=" + newRoute.voiceHints.size()
                        + " lengthMeters=" + newRoute.trackLengthMeters);
                emitState();
            } catch (Exception e) {
                AppLogger.e(TAG, "Route recalculation #" + requestNumber + " failed", e);
            }
        });
    }

    @NonNull
    private List<NavTarget> buildTargets(@NonNull PolylineIndex idx) {
        List<NavTarget> out = new ArrayList<>();
        for (int i = 0; i < intermediates.size(); i++) {
            LatLon s = intermediates.get(i);
            PolylineIndex.Match m = idx.match(s, -1);
            if (m != null) {
                out.add(new NavTarget(getString(R.string.format_stop_label, i + 1), m.alongTrackMeters));
            }
        }
        out.add(new NavTarget(getString(R.string.label_destination), idx.totalLengthMeters()));
        return out;
    }

    @Nullable
    private static LatLon parseLatLon(@NonNull String s) {
        String[] parts = s.split(",");
        if (parts.length < 2) {
            return null;
        }
        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            return new LatLon(lat, lon);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private float getSpeedMps(@NonNull Location loc) {
        if (loc.hasSpeed()) {
            return Math.max(0f, loc.getSpeed());
        }
        if (prevFiltered != null) {
            double d = GeoMath.distanceMeters(prevFiltered.getLatitude(), prevFiltered.getLongitude(), loc.getLatitude(), loc.getLongitude());
            double dt = Math.max(1.0, (loc.getTime() - prevFiltered.getTime()) / 1000.0);
            return (float) (d / dt);
        }
        return 0f;
    }

    @Nullable
    private Double getActualBearingDegrees(@NonNull Location loc) {
        if (loc.hasBearing() && getSpeedMps(loc) > 1.0f) {
            return (double) loc.getBearing();
        }
        if (prevFiltered != null) {
            double d = GeoMath.distanceMeters(prevFiltered.getLatitude(), prevFiltered.getLongitude(), loc.getLatitude(), loc.getLongitude());
            if (d < 3.0) {
                return null;
            }
            return GeoMath.bearingDegrees(prevFiltered.getLatitude(), prevFiltered.getLongitude(), loc.getLatitude(), loc.getLongitude());
        }
        return null;
    }

    private void advanceVoiceHints(double alongTrackMeters, float speedMps) {
        if (route == null || polylineIndex == null) {
            return;
        }
        List<VoiceHint> hints = route.voiceHints;
        if (hints.isEmpty()) {
            return;
        }

        while (nextHintIdx < hints.size()) {
            VoiceHint next = hints.get(nextHintIdx);
            double hintDist = polylineIndex.distanceAtPointIndex(next.indexInTrack);
            if (alongTrackMeters >= hintDist + 5.0) {
                // just passed
                notifyPassed(next);
                nextHintIdx++;
                notified10 = false;
                notified5 = false;
                continue;
            }
            break;
        }

        if (nextHintIdx >= hints.size()) {
            return;
        }
        VoiceHint next = hints.get(nextHintIdx);
        double hintDist = polylineIndex.distanceAtPointIndex(next.indexInTrack);
        double distToNext = Math.max(0.0, hintDist - alongTrackMeters);
        double timeToNext = distToNext / Math.max(1.0, speedMps);

        if (!notified10 && timeToNext <= 10.0) {
            notified10 = true;
            notifyImminent(next, distToNext, timeToNext);
        }
        if (!notified5 && timeToNext <= 5.0) {
            notified5 = true;
            notifyImminent(next, distToNext, timeToNext);
        }
    }

    private void adjustUpdateInterval(double alongTrackMeters, float speedMps) {
        long now = System.currentTimeMillis();
        long nextMinTime = 2000L;

        if (now > fastChecksUntilMs && route != null && polylineIndex != null && !route.voiceHints.isEmpty() && nextHintIdx < route.voiceHints.size()) {
            VoiceHint next = route.voiceHints.get(nextHintIdx);
            double hintDist = polylineIndex.distanceAtPointIndex(next.indexInTrack);
            double dist = Math.max(0.0, hintDist - alongTrackMeters);
            double sec = dist / Math.max(1.0, speedMps);
            long suggested = (long) Math.max(2000.0, Math.min(60000.0, sec * 250.0));
            nextMinTime = suggested;
        }

        requestLocationUpdates(nextMinTime);
    }

    private void notifyPassed(@NonNull VoiceHint hint) {
        // "just passed" is informational only, no vibration.
        AppLogger.i(TAG, "Passed voice hint index=" + nextHintIdx + " hintTrackIndex=" + hint.indexInTrack);
        sendTurnNotification(hint, 0, 0, CHANNEL_ID_NAV, false);
    }

    private void notifyImminent(@NonNull VoiceHint hint, double distMeters, double timeSeconds) {
        DirectionInfo di = VoiceHintMapper.toDirection(hint);
        AppLogger.i(TAG, "Imminent turn kind=" + di.kind
                + " distanceMeters=" + distMeters
                + " timeSeconds=" + timeSeconds);
        String channel = di.kind == DirectionKind.LEFT ? CHANNEL_ID_TURN_LEFT : (di.kind == DirectionKind.RIGHT ? CHANNEL_ID_TURN_RIGHT : CHANNEL_ID_NAV);
        boolean vibrate = di.kind == DirectionKind.LEFT || di.kind == DirectionKind.RIGHT;
        sendTurnNotification(hint, distMeters, timeSeconds, channel, vibrate);
    }

    private void sendTurnNotification(@NonNull VoiceHint hint, double distMeters, double timeSeconds, @NonNull String channelId, boolean vibrate) {
        DirectionInfo di = VoiceHintMapper.toDirection(hint);
        String dirText = di.exitNumber > 0
                ? getString(di.labelRes, di.exitNumber)
                : getString(di.labelRes);

        String distText = formatDistance(distMeters);
        String timeText = formatTimeSeconds((int) Math.round(timeSeconds));
        String msg = getString(R.string.format_turn_notification, di.emoji, distText, timeText, dirText);

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
        nm.notify(1000 + nextHintIdx, b.build());
        AppLogger.d(TAG, "Sent turn notification channel=" + channelId
                + " vibrate=" + vibrate
                + " nextHintIdx=" + nextHintIdx
                + " message=" + msg);
    }

    private String formatDistance(double meters) {
        if (meters >= 1000.0) {
            return getString(R.string.format_distance_km, meters / 1000.0);
        }
        return getString(R.string.format_distance_m, meters);
    }

    private String formatTimeSeconds(int seconds) {
        if (seconds >= 60) {
            return getString(R.string.format_time_min, (int) Math.round(seconds / 60.0));
        }
        return getString(R.string.format_time_s, Math.max(0, seconds));
    }

    private void emitState() {
        NavState s = buildState();
        for (Listener l : new ArrayList<>(listeners)) {
            try {
                l.onState(s);
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    @NonNull
    private NavState buildState() {
        if (route == null || polylineIndex == null || lastFiltered == null) {
            return NavState.waiting(this);
        }

        LatLon p = new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude());
        PolylineIndex.Match m = polylineIndex.match(p, lastSegmentIndex);
        if (m == null) {
            return NavState.waiting(this);
        }

        float speed = getSpeedMps(lastFiltered);
        return NavState.from(route, polylineIndex, m.alongTrackMeters, nextHintIdx, speed, System.currentTimeMillis(), targets, this);
    }

    @Override
    public void onDestroy() {
        AppLogger.i(TAG, "Service destroyed");
        stopNavigation();
        routeExecutor.shutdownNow();
        super.onDestroy();
    }

    @NonNull
    private static String formatLatLon(@Nullable LatLon value) {
        if (value == null) {
            return "null";
        }
        return value.lat + "," + value.lon;
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
