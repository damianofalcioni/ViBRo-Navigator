package vibro.navigator.nav.session;


import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.routing.PendingRouteRecalculation;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextResources;
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
        return ResourceAdapter.start(this, new AndroidNavigationTextResources(context), nowMs);
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
        return components.routeState.hasActiveRoute() || (started && currentRequest.isStraightLine());
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
        return !currentRequest.isStraightLine() && components.routeRequestManager.isRouteCalculationInProgress();
    }

    @Nullable
    public Double currentRouteBearingDegrees() {
        if (currentRequest.isStraightLine()) {
            return components.straightLineState.currentTargetBearingDegrees(
                    currentRequest,
                    components.locationState.getLastFilteredLocation()
            );
        }
        return components.routeState.currentSegmentBearingDegrees(components.locationState.getLastFilteredLocation());
    }

    @NonNull
    public NavigationRequest currentNavigationRequest() {
        return currentRequest;
    }

    @NonNull
    public NavigationLocationUpdateResult onRawLocationChanged(@NonNull Context context, @NonNull NavigationLocation location, long nowMs) {
        return ResourceAdapter.onRawLocationChanged(
                this,
                new AndroidNavigationTextResources(context),
                location,
                nowMs
        );
    }

    @NonNull
    public List<NogoPoint> addBlockedPointsAhead(long nowMs) {
        return components.routeState.addBlockedPointsAhead(
                components.locationState.getLastFilteredLocation(),
                nowMs
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
        return ResourceAdapter.applyRouteResult(
                this,
                new AndroidNavigationTextResources(context),
                snapshot,
                newRoute,
                beganAt
        );
    }

    public boolean isCurrentRouteRequest(@NonNull NavigationRouteRequestSnapshot snapshot) {
        return components.routeRequestManager.isCurrentRequest(snapshot);
    }

    public boolean applyRouteFailure(
            @NonNull Context context,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        return ResourceAdapter.applyRouteFailure(this, new AndroidNavigationTextResources(context), snapshot, error);
    }

    @Nullable
    public PendingRouteRecalculation consumePendingRouteRecalculation() {
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
        return ResourceAdapter.buildState(
                this,
                new AndroidNavigationTextResources(context),
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                fixedSatelliteCount,
                displayHeadingDegrees,
                displayHeadingAccuracyDegrees,
                orientationCue
        );
    }

    public static final class ResourceAdapter {
        private ResourceAdapter() {
        }

        public static boolean start(
                @NonNull NavigationSession session,
                @NonNull NavigationTextResources textResources,
                long nowMs
        ) {
            session.started = false;
            session.paused = false;
            session.components.reset(nowMs);

            if (!session.currentRequest.isComplete()) {
                session.components.routeRequestManager.markInvalidRequest(textResources);
                AppLogger.e(TAG, "Navigation start aborted because the request is incomplete "
                        + session.currentRequest.describe(), null);
                return false;
            }
            if (session.currentRequest.isStraightLine()) {
                session.components.straightLineState.onRequestStarted(session.currentRequest);
            }
            session.started = true;
            return true;
        }

        @NonNull
        public static NavigationLocationUpdateResult onRawLocationChanged(
                @NonNull NavigationSession session,
                @NonNull NavigationTextResources textResources,
                @NonNull NavigationLocation location,
                long nowMs
        ) {
            return session.components.locationEvaluator.onRawLocationChanged(
                    textResources,
                    session.currentRequest,
                    location,
                    nowMs
            );
        }

        @NonNull
        public static List<NavigationTurnEvent> applyRouteResult(
                @NonNull NavigationSession session,
                @NonNull NavigationTextResources textResources,
                @NonNull NavigationRouteRequestSnapshot snapshot,
                @NonNull GeoJsonRoute newRoute,
                long beganAt
        ) {
            if (!session.components.routeRequestManager.onRouteApplied(snapshot)) {
                return Collections.emptyList();
            }
            session.components.warmupController.onRouteApplied();
            NavigationLocation lastFiltered = session.components.locationState.getLastFilteredLocation();
            float speedMps = lastFiltered != null ? session.components.locationState.speedMps(lastFiltered) : 0f;
            return session.components.routeState.applyRouteResult(
                    textResources,
                    snapshot,
                    newRoute,
                    lastFiltered,
                    speedMps,
                    session.components.locationState.isLikelyStationary(),
                    beganAt
            );
        }

        public static boolean applyRouteFailure(
                @NonNull NavigationSession session,
                @NonNull NavigationTextResources textResources,
                @NonNull NavigationRouteRequestSnapshot snapshot,
                @NonNull Exception error
        ) {
            return session.components.routeRequestManager.onRouteFailure(textResources, snapshot, error);
        }

        @NonNull
        public static NavState buildState(
                @NonNull NavigationSession session,
                @NonNull NavigationTextResources textResources,
                long nextEvaluationDeadlineElapsedMs,
                long nowMs,
                @Nullable Integer fixedSatelliteCount,
                @Nullable Double displayHeadingDegrees,
                @Nullable Float displayHeadingAccuracyDegrees
        ) {
            return buildState(
                    session,
                    textResources,
                    nextEvaluationDeadlineElapsedMs,
                    nowMs,
                    fixedSatelliteCount,
                    displayHeadingDegrees,
                    displayHeadingAccuracyDegrees,
                    null
            );
        }

        @NonNull
        public static NavState buildState(
                @NonNull NavigationSession session,
                @NonNull NavigationTextResources textResources,
                long nextEvaluationDeadlineElapsedMs,
                long nowMs,
                @Nullable Integer fixedSatelliteCount,
                @Nullable Double displayHeadingDegrees,
                @Nullable Float displayHeadingAccuracyDegrees,
                @Nullable CompassOrientationCue orientationCue
        ) {
            return session.components.stateBuilder.build(
                    textResources,
                    session.currentRequest,
                    nextEvaluationDeadlineElapsedMs,
                    nowMs,
                    fixedSatelliteCount,
                    displayHeadingDegrees,
                    displayHeadingAccuracyDegrees,
                    orientationCue,
                    session.components.locationEvaluator.acquiredFixCount(),
                    session.paused
            );
        }
    }

}
