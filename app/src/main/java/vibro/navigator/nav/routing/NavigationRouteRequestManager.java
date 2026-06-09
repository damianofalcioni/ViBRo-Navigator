package vibro.navigator.nav.routing;


import vibro.navigator.nav.model.NavigationRequest;
import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextResources;

import java.util.ArrayList;
import java.util.List;

public final class NavigationRouteRequestManager {

    private static final String TAG = "NavRouteRequests";
    private static final long REROUTE_THROTTLE_MS = 2_000L;

    private long lastRerouteMs;
    private int routeRequestCount;
    private int routeRequestToken;
    private boolean routeCalculationInProgress;
    @Nullable
    private PendingRouteRecalculation pendingRecalculation;
    @Nullable
    private NavigationLocation activeRequestStartLocation;
    @NonNull
    private NavigationRouteRecalculationReason activeRequestReason =
            NavigationRouteRecalculationReason.EXPLICIT;
    @Nullable
    private Exception lastRouteFailure;
    @Nullable
    private String inProgressNotice;

    public void reset() {
        routeRequestToken++;
        lastRerouteMs = 0L;
        routeRequestCount = 0;
        routeCalculationInProgress = false;
        pendingRecalculation = null;
        activeRequestStartLocation = null;
        activeRequestReason = NavigationRouteRecalculationReason.EXPLICIT;
        lastRouteFailure = null;
        inProgressNotice = null;
    }

    public void stop() {
        routeRequestToken++;
        routeCalculationInProgress = false;
        pendingRecalculation = null;
        activeRequestStartLocation = null;
        activeRequestReason = NavigationRouteRecalculationReason.EXPLICIT;
        inProgressNotice = null;
    }

    public boolean isRouteCalculationInProgress() {
        return routeCalculationInProgress;
    }

    @Nullable
    public Exception getLastRouteFailure() {
        return lastRouteFailure;
    }

    @Nullable
    public String getInProgressNotice() {
        return inProgressNotice;
    }

    public void clearRouteFailure() {
        lastRouteFailure = null;
    }

    public boolean isCurrentRequest(@NonNull NavigationRouteRequestSnapshot snapshot) {
        return snapshot.requestToken == routeRequestToken;
    }

    @Nullable
    public PendingRouteRecalculation consumePendingRecalculation() {
        PendingRouteRecalculation queued = pendingRecalculation;
        pendingRecalculation = null;
        return queued;
    }

    public void markInvalidRequest(@NonNull Context context) {
        markInvalidRequest(new AndroidNavigationTextResources(context));
    }

    public void markInvalidRequest(@NonNull NavigationTextResources textResources) {
        lastRouteFailure = new IllegalStateException(textResources.getString(R.string.nav_start_invalid_request));
    }

    @Nullable
    public NavigationRouteRequestSnapshot prepare(
            boolean force,
            long nowMs,
            @NonNull NavigationRequest request,
            @Nullable NavigationLocation lastFiltered,
            @NonNull List<NogoPoint> blocked,
            @Nullable String inProgressNotice
    ) {
        return prepare(
                force,
                nowMs,
                request,
                request.stops,
                lastFiltered,
                blocked,
                inProgressNotice,
                NavigationRouteRecalculationReason.EXPLICIT
        );
    }

    @Nullable
    public NavigationRouteRequestSnapshot prepare(
            boolean force,
            long nowMs,
            @NonNull NavigationRequest request,
            @Nullable NavigationLocation lastFiltered,
            @NonNull List<NogoPoint> blocked,
            @Nullable String inProgressNotice,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        return prepare(force, nowMs, request, request.stops, lastFiltered, blocked, inProgressNotice, reason);
    }

    @Nullable
    public NavigationRouteRequestSnapshot prepare(
            boolean force,
            long nowMs,
            @NonNull NavigationRequest request,
            @NonNull List<LatLon> intermediates,
            @Nullable NavigationLocation lastFiltered,
            @NonNull List<NogoPoint> blocked,
            @Nullable String inProgressNotice
    ) {
        return prepare(
                force,
                nowMs,
                request,
                intermediates,
                lastFiltered,
                blocked,
                inProgressNotice,
                NavigationRouteRecalculationReason.EXPLICIT
        );
    }

    @Nullable
    public NavigationRouteRequestSnapshot prepare(
            boolean force,
            long nowMs,
            @NonNull NavigationRequest request,
            @NonNull List<LatLon> intermediates,
            @Nullable NavigationLocation lastFiltered,
            @NonNull List<NogoPoint> blocked,
            @Nullable String inProgressNotice,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        if (routeCalculationInProgress) {
            updatePendingRecalculation(force, lastFiltered, reason, inProgressNotice);
            return null;
        }
        if (!force && nowMs - lastRerouteMs < REROUTE_THROTTLE_MS) {
            AppLogger.d(TAG, "Skipping route recalculation because of throttle elapsedMs=" + (nowMs - lastRerouteMs));
            return null;
        }
        if (lastFiltered == null) {
            AppLogger.w(TAG, "Cannot recalculate route without a filtered NavigationLocation");
            return null;
        }

        lastRerouteMs = nowMs;
        int requestNumber = ++routeRequestCount;
        int requestToken = ++routeRequestToken;
        NavigationRouteRequestSnapshot snapshot = new NavigationRouteRequestSnapshot(
                requestNumber,
                requestToken,
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                new ArrayList<>(intermediates),
                request.destination,
                request.profile,
                new ArrayList<>(blocked)
        );
        routeCalculationInProgress = true;
        activeRequestStartLocation = new NavigationLocation(lastFiltered);
        activeRequestReason = reason;
        lastRouteFailure = null;
        this.inProgressNotice = sanitizeNotice(inProgressNotice);
        AppLogger.i(TAG, "Submitting route recalculation #" + requestNumber
                + " force=" + force
                + " reason=" + reason
                + " start=" + formatLatLon(snapshot.start)
                + " destination=" + formatLatLon(snapshot.destination)
                + " intermediates=" + snapshot.intermediates.size()
                + " blocked=" + snapshot.blocked.size());
        return snapshot;
    }

    public boolean onRouteApplied(@NonNull NavigationRouteRequestSnapshot snapshot) {
        if (snapshot.requestToken != routeRequestToken) {
            AppLogger.d(TAG, "Discarded stale route result #" + snapshot.requestNumber);
            return false;
        }
        routeCalculationInProgress = false;
        activeRequestStartLocation = null;
        activeRequestReason = NavigationRouteRecalculationReason.EXPLICIT;
        lastRouteFailure = null;
        inProgressNotice = null;
        return true;
    }

    public boolean onRouteFailure(
            @NonNull Context context,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        return onRouteFailure(new AndroidNavigationTextResources(context), snapshot, error);
    }

    public boolean onRouteFailure(
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        if (snapshot.requestToken != routeRequestToken) {
            AppLogger.d(TAG, "Discarded stale route failure #" + snapshot.requestNumber);
            return false;
        }
        routeCalculationInProgress = false;
        activeRequestStartLocation = null;
        activeRequestReason = NavigationRouteRecalculationReason.EXPLICIT;
        lastRouteFailure = error;
        inProgressNotice = null;
        AppLogger.e(TAG, "Route recalculation #" + snapshot.requestNumber + " failed", error);
        AppLogger.w(TAG, "Route recalculation #" + snapshot.requestNumber + " failure summary="
                + NavigationRouteFailureFormatter.format(textResources, error, false));
        return true;
    }

    @NonNull
    private static String formatLatLon(@Nullable LatLon value) {
        if (value == null) {
            return "null";
        }
        return value.lat + "," + value.lon;
    }

    private void updatePendingRecalculation(
            boolean force,
            @Nullable NavigationLocation latestStart,
            @NonNull NavigationRouteRecalculationReason reason,
            @Nullable String inProgressNotice
    ) {
        PendingRouteRecalculation next = new PendingRouteRecalculation(
                true,
                reason,
                sanitizeNotice(inProgressNotice)
        );
        if (pendingRecalculation != null) {
            pendingRecalculation = pendingRecalculation.merge(next);
            AppLogger.d(TAG, "Route recalculation already queued while previous request is still running");
            return;
        }
        if (!shouldQueuePendingRecalculation(force, latestStart, reason)) {
            AppLogger.i(TAG, "Skipped queued startup route recalculation because latest fix does not "
                    + "materially improve route start distance="
                    + StartupRouteRefreshPolicy.distanceMeters(activeRequestStartLocation, latestStart));
            return;
        }
        pendingRecalculation = next;
        AppLogger.d(TAG, "Queued route recalculation while previous request is still running");
    }

    private boolean shouldQueuePendingRecalculation(
            boolean force,
            @Nullable NavigationLocation latestStart,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        if (force) {
            return true;
        }
        if (reason != NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
                || activeRequestReason != NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE) {
            return true;
        }
        return StartupRouteRefreshPolicy.shouldRefresh(activeRequestStartLocation, latestStart);
    }

    @Nullable
    private static String sanitizeNotice(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
