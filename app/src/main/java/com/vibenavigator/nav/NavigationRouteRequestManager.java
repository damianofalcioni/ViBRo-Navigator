package com.vibenavigator.nav;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.R;
import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.geo.LatLon;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;

final class NavigationRouteRequestManager {

    private static final String TAG = "NavRouteRequests";
    private static final long FAST_CHECK_WARMUP_MS = 30_000L;
    private static final long REROUTE_THROTTLE_MS = 8_000L;

    private long fastChecksUntilMs;
    private long lastRerouteMs;
    private int routeRequestCount;
    private int routeRequestToken;
    private boolean routeCalculationInProgress;
    @Nullable
    private Exception lastRouteFailure;

    void reset(long nowMs) {
        routeRequestToken++;
        fastChecksUntilMs = nowMs + FAST_CHECK_WARMUP_MS;
        lastRerouteMs = 0L;
        routeRequestCount = 0;
        routeCalculationInProgress = false;
        lastRouteFailure = null;
    }

    void stop() {
        routeRequestToken++;
        routeCalculationInProgress = false;
    }

    long getFastChecksUntilMs() {
        return fastChecksUntilMs;
    }

    boolean isRouteCalculationInProgress() {
        return routeCalculationInProgress;
    }

    @Nullable
    Exception getLastRouteFailure() {
        return lastRouteFailure;
    }

    void clearRouteFailure() {
        lastRouteFailure = null;
    }

    void markInvalidRequest(@NonNull Context context) {
        lastRouteFailure = new IllegalStateException(context.getString(R.string.nav_start_invalid_request));
    }

    @Nullable
    NavigationSession.RouteRequestSnapshot prepare(
            boolean force,
            long nowMs,
            @NonNull NavigationRequest request,
            @Nullable Location lastFiltered,
            @NonNull List<NogoPoint> blocked
    ) {
        if (!force && nowMs - lastRerouteMs < REROUTE_THROTTLE_MS) {
            AppLogger.d(TAG, "Skipping route recalculation because of throttle elapsedMs=" + (nowMs - lastRerouteMs));
            return null;
        }
        if (lastFiltered == null) {
            AppLogger.w(TAG, "Cannot recalculate route without a filtered location");
            return null;
        }

        lastRerouteMs = nowMs;
        int requestNumber = ++routeRequestCount;
        int requestToken = ++routeRequestToken;
        NavigationSession.RouteRequestSnapshot snapshot = new NavigationSession.RouteRequestSnapshot(
                requestNumber,
                requestToken,
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                new ArrayList<>(request.stops),
                request.destination,
                request.profile,
                new ArrayList<>(blocked)
        );
        routeCalculationInProgress = true;
        lastRouteFailure = null;
        AppLogger.i(TAG, "Submitting route recalculation #" + requestNumber
                + " force=" + force
                + " start=" + formatLatLon(snapshot.start)
                + " destination=" + formatLatLon(snapshot.destination)
                + " intermediates=" + snapshot.intermediates.size()
                + " blocked=" + snapshot.blocked.size());
        return snapshot;
    }

    boolean onRouteApplied(@NonNull NavigationSession.RouteRequestSnapshot snapshot) {
        if (snapshot.requestToken != routeRequestToken) {
            AppLogger.d(TAG, "Discarded stale route result #" + snapshot.requestNumber);
            return false;
        }
        routeCalculationInProgress = false;
        lastRouteFailure = null;
        return true;
    }

    void onRouteFailure(
            @NonNull Context context,
            @NonNull NavigationSession.RouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        if (snapshot.requestToken != routeRequestToken) {
            AppLogger.d(TAG, "Discarded stale route failure #" + snapshot.requestNumber);
            return;
        }
        routeCalculationInProgress = false;
        lastRouteFailure = error;
        AppLogger.e(TAG, "Route recalculation #" + snapshot.requestNumber + " failed", error);
        AppLogger.w(TAG, "Route recalculation #" + snapshot.requestNumber + " failure summary="
                + NavigationRouteFailureFormatter.format(context, error, false));
    }

    @NonNull
    private static String formatLatLon(@Nullable LatLon value) {
        if (value == null) {
            return "null";
        }
        return value.lat + "," + value.lon;
    }
}
