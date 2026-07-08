package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

public final class NavigationSessionResourceAdapter {
    private static final String TAG = "NavigationSession";

    private NavigationSessionResourceAdapter() {
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
        session.components.tripStatsTracker.start(nowMs);
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
        return onRawLocationChanged(
                session,
                textResources,
                location,
                nowMs,
                NavigationLocationController.DEFAULT_UPDATE_INTERVAL_MS
        );
    }

    @NonNull
    public static NavigationLocationUpdateResult onRawLocationChanged(
            @NonNull NavigationSession session,
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationLocation location,
            long nowMs,
            long expectedUpdateIntervalMs
    ) {
        return session.components.locationEvaluator.onRawLocationChanged(
                textResources,
                session.currentRequest,
                location,
                nowMs,
                expectedUpdateIntervalMs
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
        return applyRouteResult(session, textResources, snapshot, newRoute, beganAt, beganAt);
    }

    @NonNull
    public static List<NavigationTurnEvent> applyRouteResult(
            @NonNull NavigationSession session,
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt,
            long routeAppliedAtElapsedMs
    ) {
        if (!session.components.routeRequestManager.onRouteApplied(snapshot)) {
            return Collections.emptyList();
        }
        session.components.warmupController.onRouteApplied(routeAppliedAtElapsedMs);
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
    public static List<String> buildCurrentDirectionDetails(
            @NonNull NavigationSession session,
            @NonNull NavigationTextResources textResources
    ) {
        if (!session.started || !session.currentRequest.isComplete()) {
            return Collections.emptyList();
        }
        return session.components.directionDetailsBuilder.build(
                textResources,
                session.currentRequest
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
