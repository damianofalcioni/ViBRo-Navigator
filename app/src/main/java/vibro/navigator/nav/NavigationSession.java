package vibro.navigator.nav;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.util.AppLogger;

import java.util.Collections;
import java.util.List;

// Session coordinator: explicit state collaborators make navigation handoffs auditable without a generic facade.
@SuppressWarnings("PMD.CouplingBetweenObjects")
final class NavigationSession {

    private static final String TAG = "NavigationSession";
    private static final long NO_SUGGESTED_INTERVAL = -1L;

    private final NavigationSessionLocationState locationState = new NavigationSessionLocationState();
    private final NavigationSessionHeadingResolver headingResolver =
            new NavigationSessionHeadingResolver(locationState);
    private final NavigationSessionRouteState routeState = new NavigationSessionRouteState();
    private final NavigationWarmupController warmupController = new NavigationWarmupController();
    private final NavigationRouteRequestManager routeRequestManager = new NavigationRouteRequestManager();
    private boolean started;
    private boolean paused;

    @NonNull
    private NavigationRequest currentRequest = new NavigationRequest(null, null, null, Collections.emptyList());

    void loadRequest(@NonNull NavigationRequest request) {
        currentRequest = request;
        AppLogger.i(TAG, "Navigation request loaded " + request.describe());
    }

    boolean start(@NonNull Context context, long nowMs) {
        started = false;
        paused = false;
        locationState.reset();
        routeState.reset();
        warmupController.reset(nowMs);
        routeRequestManager.reset(nowMs);

        if (!currentRequest.isComplete()) {
            routeRequestManager.markInvalidRequest(context);
            AppLogger.e(TAG, "Navigation start aborted because the request is incomplete "
                    + currentRequest.describe(), null);
            return false;
        }
        started = true;
        return true;
    }

    void stop() {
        started = false;
        paused = false;
        routeRequestManager.stop();
    }

    boolean pause() {
        if (!started || paused) {
            return false;
        }
        paused = true;
        return true;
    }

    boolean resume() {
        if (!started || !paused) {
            return false;
        }
        paused = false;
        return true;
    }

    boolean isPaused() {
        return paused;
    }

    boolean hasActiveRoute() {
        return routeState.hasActiveRoute();
    }

    void onProviderDisabled(@NonNull String provider) {
        locationState.onProviderDisabled(provider);
    }

    @Nullable
    Location getLastFilteredLocation() {
        return locationState.getLastFilteredLocation();
    }

    float lastFilteredSpeedMps() {
        Location lastFiltered = locationState.getLastFilteredLocation();
        return lastFiltered == null ? 0f : locationState.speedMps(lastFiltered);
    }

    boolean isLikelyStationaryForOrientation() {
        return locationState.isLikelyStationary();
    }

    boolean isRouteCalculationInProgress() {
        return routeRequestManager.isRouteCalculationInProgress();
    }

    @Nullable
    Double currentRouteBearingDegrees() {
        return routeState.currentSegmentBearingDegrees(locationState.getLastFilteredLocation());
    }

    @NonNull
    NavigationRequest currentNavigationRequest() {
        return currentRequest;
    }

    @NonNull
    NavigationLocationUpdateResult onRawLocationChanged(@NonNull Context context, @NonNull Location location, long nowMs) {
        NavigationSessionLocationState.Update update = locationState.onRawLocationChanged(location);
        if (update.isDropped()) {
            return NavigationLocationUpdateResult.dropped();
        }

        Location filtered = update.getFilteredLocation();
        routeRequestManager.clearRouteFailure();
        if (!currentRequest.isComplete()) {
            routeRequestManager.markInvalidRequest(context);
            AppLogger.e(TAG, "Skipping route evaluation because the request is incomplete "
                    + currentRequest.describe(), null);
            return NavigationLocationUpdateResult.accepted(
                    filtered,
                    true,
                    null,
                    Collections.emptyList(),
                    NO_SUGGESTED_INTERVAL
            );
        }

        NavigationSessionRouteState.Evaluation evaluation = routeState.evaluateLocation(
                filtered,
                locationState.speedMps(filtered),
                locationState.isLikelyStationary(),
                locationState.accuracyMeters(filtered),
                locationState.trustedActualBearingDegreesForReroute(filtered),
                nowMs,
                warmupController.getFastChecksUntilMs()
        );
        warmupController.recordEvaluation(evaluation.isStableOnRouteSample(), locationState.accuracyMeters(filtered), nowMs);
        return NavigationLocationUpdateResult.accepted(
                filtered,
                evaluation.shouldRecalculateRoute(),
                evaluation.rerouteNotice,
                evaluation.turnEvents,
                evaluation.getSuggestedUpdateIntervalMs()
        );
    }

    @NonNull
    List<?> addBlockedPointsAhead() {
        return routeState.addBlockedPointsAhead(locationState.getLastFilteredLocation(), System.currentTimeMillis());
    }

    @Nullable
    NavigationRouteRequestSnapshot prepareRouteRequest(boolean force, long nowMs) {
        return prepareRouteRequest(force, nowMs, null);
    }

    @Nullable
    NavigationRouteRequestSnapshot prepareRouteRequest(boolean force, long nowMs, @Nullable String inProgressNotice) {
        return routeRequestManager.prepare(
                force,
                nowMs,
                currentRequest,
                locationState.getLastFilteredLocation(),
                routeState.copyBlockedPoints(),
                inProgressNotice
        );
    }

    @NonNull
    List<NavigationTurnEvent> applyRouteResult(
            @NonNull Context context,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt
    ) {
        if (!routeRequestManager.onRouteApplied(snapshot)) {
            return Collections.emptyList();
        }
        warmupController.onRouteApplied();
        Location lastFiltered = locationState.getLastFilteredLocation();
        float speedMps = lastFiltered != null ? locationState.speedMps(lastFiltered) : 0f;
        return routeState.applyRouteResult(
                context,
                currentRequest,
                snapshot,
                newRoute,
                lastFiltered,
                speedMps,
                locationState.isLikelyStationary(),
                beganAt
        );
    }

    void applyRouteFailure(
            @NonNull Context context,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        routeRequestManager.onRouteFailure(context, snapshot, error);
    }

    boolean consumePendingRouteRecalculation() {
        return routeRequestManager.consumePendingRecalculation();
    }

    @NonNull
    NavState buildState(
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees
    ) {
        NavState baseState;
        Location lastFiltered = locationState.getLastFilteredLocation();
        float speedMps = lastFiltered != null ? locationState.speedMps(lastFiltered) : 0f;
        boolean likelyStationary = locationState.isLikelyStationary();
        float accuracyMeters = lastFiltered != null
                ? locationState.accuracyMeters(lastFiltered)
                : Float.MAX_VALUE;
        NavigationSessionHeadingResolver.Selection heading = headingResolver.selectHeading(
                lastFiltered,
                likelyStationary,
                displayHeadingDegrees,
                displayHeadingAccuracyDegrees
        );
        baseState = routeState.buildState(
                context,
                lastFiltered,
                speedMps,
                likelyStationary,
                accuracyMeters,
                fixedSatelliteCount,
                heading.headingDegrees,
                heading.headingAccuracyDegrees,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                routeRequestManager.isRouteCalculationInProgress(),
                routeRequestManager.getInProgressNotice(),
                routeRequestManager.getLastRouteFailure()
        );
        return NavState.withPauseState(context, baseState, paused);
    }

}
