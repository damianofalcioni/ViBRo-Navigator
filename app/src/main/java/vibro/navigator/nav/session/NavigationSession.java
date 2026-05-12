package vibro.navigator.nav.session;


import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.routing.NavigationRouteRequestManager;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.logging.AppLogger;

import java.util.Collections;
import java.util.List;

// Session coordinator: explicit state collaborators make navigation handoffs auditable without a generic facade.
@SuppressWarnings("PMD.CouplingBetweenObjects")
public final class NavigationSession {

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
    private int acquiredFixCount;

    @NonNull
    private NavigationRequest currentRequest = new NavigationRequest(null, null, null, Collections.emptyList());

    public void loadRequest(@NonNull NavigationRequest request) {
        currentRequest = request;
        AppLogger.i(TAG, "Navigation request loaded " + request.describe());
    }

    public boolean start(@NonNull Context context, long nowMs) {
        started = false;
        paused = false;
        acquiredFixCount = 0;
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

    public void stop() {
        started = false;
        paused = false;
        routeRequestManager.stop();
    }

    public boolean pause() {
        if (!started || paused) {
            return false;
        }
        paused = true;
        return true;
    }

    public boolean resume() {
        if (!started || !paused) {
            return false;
        }
        paused = false;
        return true;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean hasActiveRoute() {
        return routeState.hasActiveRoute();
    }

    public void onProviderDisabled(@NonNull String provider) {
        locationState.onProviderDisabled(provider);
    }

    @Nullable
    public Location getLastFilteredLocation() {
        return locationState.getLastFilteredLocation();
    }

    public float lastFilteredSpeedMps() {
        Location lastFiltered = locationState.getLastFilteredLocation();
        return lastFiltered == null ? 0f : locationState.speedMps(lastFiltered);
    }

    public boolean isLikelyStationaryForOrientation() {
        return locationState.isLikelyStationary();
    }

    public boolean isRouteCalculationInProgress() {
        return routeRequestManager.isRouteCalculationInProgress();
    }

    @Nullable
    public Double currentRouteBearingDegrees() {
        return routeState.currentSegmentBearingDegrees(locationState.getLastFilteredLocation());
    }

    @NonNull
    public NavigationRequest currentNavigationRequest() {
        return currentRequest;
    }

    @NonNull
    public NavigationLocationUpdateResult onRawLocationChanged(@NonNull Context context, @NonNull Location location, long nowMs) {
        NavigationSessionLocationState.Update update = locationState.onRawLocationChanged(location);
        if (update.isDropped()) {
            return NavigationLocationUpdateResult.dropped();
        }
        acquiredFixCount++;

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
    public List<?> addBlockedPointsAhead() {
        return routeState.addBlockedPointsAhead(locationState.getLastFilteredLocation(), System.currentTimeMillis());
    }

    @Nullable
    public NavigationRouteRequestSnapshot prepareRouteRequest(boolean force, long nowMs) {
        return prepareRouteRequest(force, nowMs, null);
    }

    @Nullable
    public NavigationRouteRequestSnapshot prepareRouteRequest(boolean force, long nowMs, @Nullable String inProgressNotice) {
        return routeRequestManager.prepare(
                force,
                nowMs,
                currentRequest,
                routeState.remainingIntermediateStops(currentRequest.stops),
                locationState.getLastFilteredLocation(),
                routeState.copyBlockedPoints(),
                inProgressNotice
        );
    }

    @NonNull
    public List<NavigationTurnEvent> applyRouteResult(
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
                snapshot,
                newRoute,
                lastFiltered,
                speedMps,
                locationState.isLikelyStationary(),
                beganAt
        );
    }

    public void applyRouteFailure(
            @NonNull Context context,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        routeRequestManager.onRouteFailure(context, snapshot, error);
    }

    public boolean consumePendingRouteRecalculation() {
        return routeRequestManager.consumePendingRecalculation();
    }

    @NonNull
    public NavState buildState(
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees
    ) {
        return buildState(
                context,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                fixedSatelliteCount,
                displayHeadingDegrees,
                displayHeadingAccuracyDegrees,
                null
        );
    }

    @NonNull
    public NavState buildState(
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees,
            @Nullable CompassOrientationCue orientationCue
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
        NavigationDisplaySnapshot snapshot = NavigationDisplaySnapshot.builder(context)
                .location(lastFiltered, speedMps, likelyStationary, accuracyMeters)
                .gps(fixedSatelliteCount, acquiredFixCount)
                .heading(heading.headingDegrees, heading.headingAccuracyDegrees)
                .orientationCue(orientationCue)
                .timing(nextEvaluationDeadlineElapsedMs, nowMs)
                .routeCalculation(
                        routeRequestManager.isRouteCalculationInProgress(),
                        routeRequestManager.getInProgressNotice(),
                        routeRequestManager.getLastRouteFailure()
                )
                .build();
        baseState = routeState.advanceDisplayState(snapshot);
        return NavStateComposer.withPauseState(context, baseState, paused);
    }

}
