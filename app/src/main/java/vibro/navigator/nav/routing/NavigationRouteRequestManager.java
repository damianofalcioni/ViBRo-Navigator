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
    @NonNull
    private final NavigationRouteRequestSpeculation speculation = new NavigationRouteRequestSpeculation();
    @NonNull
    private final NavigationPendingRouteRecalculations pendingRecalculations =
            new NavigationPendingRouteRecalculations();
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
        speculation.reset();
        pendingRecalculations.reset();
        activeRequestStartLocation = null;
        activeRequestReason = NavigationRouteRecalculationReason.EXPLICIT;
        lastRouteFailure = null;
        inProgressNotice = null;
    }

    public void stop() {
        routeRequestToken++;
        routeCalculationInProgress = false;
        speculation.reset();
        pendingRecalculations.reset();
        activeRequestStartLocation = null;
        activeRequestReason = NavigationRouteRecalculationReason.EXPLICIT;
        inProgressNotice = null;
    }

    public boolean isRouteCalculationInProgress() {
        return routeCalculationInProgress;
    }

    public boolean cancelActiveSpeculativeRequest() {
        NavigationRouteRequestSpeculation.CancelResult cancellation =
                speculation.cancel(routeCalculationInProgress, lastRerouteMs);
        if (!cancellation.canceled) {
            return false;
        }
        routeRequestToken += cancellation.requestTokenIncrement;
        lastRerouteMs = cancellation.lastRerouteMs;
        routeCalculationInProgress = false;
        activeRequestStartLocation = null;
        activeRequestReason = NavigationRouteRecalculationReason.EXPLICIT;
        inProgressNotice = null;
        AppLogger.i(TAG, "Canceled speculative route recalculation");
        return true;
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
        return pendingRecalculations.consume();
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
        return prepare(force, nowMs, request, request.stops, lastFiltered, blocked, inProgressNotice, reason, false);
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
        return prepare(force, nowMs, request, intermediates, lastFiltered, blocked, inProgressNotice, reason, false);
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
            @NonNull NavigationRouteRecalculationReason reason,
            boolean speculative
    ) {
        if (shouldSkipBecauseRequestInProgress(force, lastFiltered, reason, inProgressNotice, speculative)) {
            return null;
        }
        if (request.isStraightLine()) {
            AppLogger.d(TAG, "Skipping route recalculation for straight-line navigation mode");
            return null;
        }
        lastRerouteMs = speculation.restoreDeferredThrottleIfNeeded(lastRerouteMs);
        if (!force && isWithinRerouteThrottle(nowMs)) {
            AppLogger.d(TAG, "Skipping route recalculation because of throttle elapsedMs="
                    + elapsedSinceLastRerouteMs(nowMs));
            return null;
        }
        if (lastFiltered == null) {
            AppLogger.w(TAG, "Cannot recalculate route without a filtered NavigationLocation");
            return null;
        }

        speculation.onRequestStarted(speculative, lastRerouteMs);
        lastRerouteMs = nowMs;
        int requestNumber = ++routeRequestCount;
        int requestToken = ++routeRequestToken;
        NavigationRouteRequestSnapshot snapshot = new NavigationRouteRequestSnapshot(
                requestNumber,
                requestToken,
                request.routingMode,
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                new ArrayList<>(intermediates),
                request.destination,
                request.profile,
                request.customProfile,
                request.profileParameters,
                new ArrayList<>(blocked),
                request.roundTripDistanceMeters,
                request.roundTripDirectionDegrees,
                speculative
        );
        routeCalculationInProgress = true;
        activeRequestStartLocation = new NavigationLocation(lastFiltered);
        activeRequestReason = reason;
        lastRouteFailure = null;
        this.inProgressNotice = RouteRecalculationNotice.sanitize(inProgressNotice);
        AppLogger.i(TAG, "Submitting route recalculation #" + requestNumber
                + " force=" + force
                + " reason=" + reason
                + " start=" + formatLatLon(snapshot.start)
                + " destination=" + formatLatLon(snapshot.destination)
                + " intermediates=" + snapshot.intermediates.size()
                + " blocked=" + snapshot.blocked.size()
                + " roundTripDirectionDegrees=" + snapshot.roundTripDirectionDegrees
                + " speculative=" + speculative);
        return snapshot;
    }

    public boolean onRouteApplied(@NonNull NavigationRouteRequestSnapshot snapshot) {
        if (snapshot.requestToken != routeRequestToken) {
            AppLogger.d(TAG, "Discarded stale route result #" + snapshot.requestNumber);
            return false;
        }
        routeCalculationInProgress = false;
        speculation.onRequestCompleted();
        activeRequestStartLocation = null;
        activeRequestReason = NavigationRouteRecalculationReason.EXPLICIT;
        lastRouteFailure = null;
        inProgressNotice = null;
        return true;
    }

    public boolean onSpeculativeRouteFinished(
            @NonNull NavigationRouteRequestSnapshot snapshot,
            boolean deferred
    ) {
        if (snapshot.requestToken != routeRequestToken) {
            AppLogger.d(TAG, "Ignored stale route result #" + snapshot.requestNumber);
            return false;
        }
        routeCalculationInProgress = false;
        lastRerouteMs = speculation.onSpeculativeRequestFinished(
                snapshot.speculative,
                deferred,
                lastRerouteMs
        );
        activeRequestStartLocation = null;
        activeRequestReason = NavigationRouteRecalculationReason.EXPLICIT;
        inProgressNotice = null;
        return true;
    }

    public void onDeferredSpeculativeRouteApplied() {
        speculation.onDeferredSpeculativeRouteApplied();
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
        speculation.onRequestCompleted();
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

    private boolean isWithinRerouteThrottle(long nowMs) {
        long elapsedMs = elapsedSinceLastRerouteMs(nowMs);
        return elapsedMs >= 0L && elapsedMs < REROUTE_THROTTLE_MS;
    }

    private long elapsedSinceLastRerouteMs(long nowMs) {
        return lastRerouteMs <= 0L ? REROUTE_THROTTLE_MS : nowMs - lastRerouteMs;
    }

    private boolean shouldSkipBecauseRequestInProgress(
            boolean force,
            @Nullable NavigationLocation latestStart,
            @NonNull NavigationRouteRecalculationReason reason,
            @Nullable String pendingNotice,
            boolean speculative
    ) {
        if (!routeCalculationInProgress) {
            return false;
        }
        if (speculative) {
            AppLogger.d(TAG, "Skipping speculative route recalculation while another request is running");
            return true;
        }
        pendingRecalculations.update(
                force,
                latestStart,
                activeRequestReason,
                activeRequestStartLocation,
                reason,
                pendingNotice
        );
        return true;
    }

}
