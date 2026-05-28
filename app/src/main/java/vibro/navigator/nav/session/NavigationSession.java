package vibro.navigator.nav.session;


import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.route.GeoJsonRoute;

import java.util.Collections;
import java.util.List;

// Session coordinator: explicit state collaborators make navigation handoffs auditable without a generic facade.
public final class NavigationSession {

    private static final String TAG = "NavigationSession";

    private final NavigationSessionComponents components = new NavigationSessionComponents();
    private boolean started;
    private boolean paused;

    @NonNull
    private NavigationRequest currentRequest = new NavigationRequest(null, null, null, Collections.emptyList());

    public void loadRequest(@NonNull NavigationRequest request) {
        currentRequest = request;
        AppLogger.i(TAG, "Navigation request loaded " + request.describe());
    }

    public boolean start(@NonNull Context context, long nowMs) {
        started = false;
        paused = false;
        components.reset(nowMs);

        if (!currentRequest.isComplete()) {
            components.routeRequestManager.markInvalidRequest(context);
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
        components.routeRequestManager.stop();
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
        return components.routeState.hasActiveRoute();
    }

    @Nullable
    public String buildCurrentRouteGpx(@NonNull Context context) {
        return NavigationSessionRouteExporter.export(context, components.routeState, currentRequest);
    }

    public void onProviderDisabled(@NonNull String provider) {
        components.locationState.onProviderDisabled(provider);
    }

    @Nullable
    public NavigationLocation getLastFilteredLocation() {
        return components.locationState.getLastFilteredLocation();
    }

    public float lastFilteredSpeedMps() {
        NavigationLocation lastFiltered = components.locationState.getLastFilteredLocation();
        return lastFiltered == null ? 0f : components.locationState.speedMps(lastFiltered);
    }

    public boolean isLikelyStationaryForOrientation() {
        return components.locationState.isLikelyStationary();
    }

    public boolean isRouteCalculationInProgress() {
        return components.routeRequestManager.isRouteCalculationInProgress();
    }

    @Nullable
    public Double currentRouteBearingDegrees() {
        return components.routeState.currentSegmentBearingDegrees(components.locationState.getLastFilteredLocation());
    }

    @NonNull
    public NavigationRequest currentNavigationRequest() {
        return currentRequest;
    }

    @NonNull
    public NavigationLocationUpdateResult onRawLocationChanged(@NonNull Context context, @NonNull NavigationLocation NavigationLocation, long nowMs) {
        return components.locationEvaluator.onRawLocationChanged(context, currentRequest, NavigationLocation, nowMs);
    }

    @NonNull
    public List<?> addBlockedPointsAhead() {
        return components.routeState.addBlockedPointsAhead(
                components.locationState.getLastFilteredLocation(),
                System.currentTimeMillis()
        );
    }

    @Nullable
    public NavigationRouteRequestSnapshot prepareRouteRequest(boolean force, long nowMs) {
        return prepareRouteRequest(force, nowMs, null);
    }

    @Nullable
    public NavigationRouteRequestSnapshot prepareRouteRequest(boolean force, long nowMs, @Nullable String inProgressNotice) {
        return prepareRouteRequest(
                force,
                nowMs,
                inProgressNotice,
                NavigationRouteRecalculationReason.EXPLICIT
        );
    }

    @Nullable
    public NavigationRouteRequestSnapshot prepareRouteRequest(
            boolean force,
            long nowMs,
            @Nullable String inProgressNotice,
            @NonNull NavigationRouteRecalculationReason reason
    ) {
        return components.routeRequestManager.prepare(
                force,
                nowMs,
                currentRequest,
                components.routeState.remainingIntermediateStops(currentRequest.stops),
                components.locationState.getLastFilteredLocation(),
                components.routeState.copyBlockedPoints(),
                inProgressNotice,
                reason
        );
    }

    @NonNull
    public List<NavigationTurnEvent> applyRouteResult(
            @NonNull Context context,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt
    ) {
        if (!components.routeRequestManager.onRouteApplied(snapshot)) {
            return Collections.emptyList();
        }
        components.warmupController.onRouteApplied();
        NavigationLocation lastFiltered = components.locationState.getLastFilteredLocation();
        float speedMps = lastFiltered != null ? components.locationState.speedMps(lastFiltered) : 0f;
        return components.routeState.applyRouteResult(
                context,
                snapshot,
                newRoute,
                lastFiltered,
                speedMps,
                components.locationState.isLikelyStationary(),
                beganAt
        );
    }

    public void applyRouteFailure(
            @NonNull Context context,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        components.routeRequestManager.onRouteFailure(context, snapshot, error);
    }

    public boolean consumePendingRouteRecalculation() {
        return components.routeRequestManager.consumePendingRecalculation();
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
        return components.stateBuilder.build(
                context,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                fixedSatelliteCount,
                displayHeadingDegrees,
                displayHeadingAccuracyDegrees,
                orientationCue,
                components.locationEvaluator.acquiredFixCount(),
                paused
        );
    }

}
