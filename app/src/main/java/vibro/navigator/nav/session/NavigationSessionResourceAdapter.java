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
import vibro.navigator.nav.power.NavigationBatterySnapshot;
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
        return start(session, textResources, nowMs, true, NavigationBatterySnapshot.unavailable());
    }

    public static boolean start(
            @NonNull NavigationSession session,
            @NonNull NavigationTextResources textResources,
            long nowMs,
            boolean screenInteractive,
            @NonNull NavigationBatterySnapshot batterySnapshot
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
        session.components.tripStatsTracker.start(nowMs, screenInteractive, batterySnapshot);
        session.started = true;
        return true;
    }

    public static void recordScreenInteractive(
            @NonNull NavigationSession session,
            boolean interactive,
            long nowMs
    ) {
        session.components.tripStatsTracker.recordScreenInteractive(interactive, nowMs);
    }

    public static void recordBatterySnapshot(
            @NonNull NavigationSession session,
            @NonNull NavigationBatterySnapshot batterySnapshot
    ) {
        session.components.tripStatsTracker.recordBatterySnapshot(batterySnapshot);
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
        return onRawLocationChanged(
                session,
                textResources,
                location,
                nowMs,
                expectedUpdateIntervalMs,
                false
        );
    }

    @NonNull
    public static NavigationLocationUpdateResult onRawLocationChanged(
            @NonNull NavigationSession session,
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationLocation location,
            long nowMs,
            long expectedUpdateIntervalMs,
            boolean singleInstructionMode
    ) {
        session.components.routeState.setSingleInstructionMode(singleInstructionMode);
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
        return applyRouteResult(
                session,
                textResources,
                snapshot,
                newRoute,
                beganAt,
                routeAppliedAtElapsedMs,
                false
        );
    }

    @NonNull
    public static List<NavigationTurnEvent> applyRouteResult(
            @NonNull NavigationSession session,
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt,
            long routeAppliedAtElapsedMs,
            boolean singleInstructionMode
    ) {
        session.components.routeState.setSingleInstructionMode(singleInstructionMode);
        if (!session.components.routeRequestManager.onRouteApplied(snapshot)) {
            return Collections.emptyList();
        }
        session.components.warmupController.onRouteApplied(routeAppliedAtElapsedMs);
        NavigationLocation lastFiltered = session.components.locationState.getLastFilteredLocation();
        float speedMps = lastFiltered != null ? session.components.locationState.speedMps(lastFiltered) : 0f;
        List<NavigationTurnEvent> turnEvents = session.components.routeState.applyRouteResult(
                textResources,
                snapshot,
                newRoute,
                lastFiltered,
                speedMps,
                session.components.locationState.isLikelyStationary(),
                beganAt
        );
        session.components.speculativeRouteState.onRouteApplied(snapshot);
        return turnEvents;
    }

    public static boolean handleUnconfirmedSpeculativeRouteResult(
            @NonNull NavigationSession session,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt
    ) {
        if (!snapshot.speculative || session.components.speculativeRouteState.isConfirmed(snapshot)) {
            return false;
        }
        boolean deferred = session.components.speculativeRouteState.deferResult(snapshot, newRoute, beganAt);
        boolean handled = session.components.routeRequestManager.onSpeculativeRouteFinished(snapshot, deferred);
        if (!handled && deferred) {
            session.components.speculativeRouteState.onRouteFailed(snapshot);
        }
        return true;
    }

    public static boolean ignoreUnconfirmedSpeculativeRouteFailure(
            @NonNull NavigationSession session,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        if (!snapshot.speculative || session.components.speculativeRouteState.isConfirmed(snapshot)) {
            return false;
        }
        boolean ignored = session.components.routeRequestManager.onSpeculativeRouteFinished(snapshot, false);
        session.components.speculativeRouteState.onRouteFailed(snapshot);
        if (ignored) {
            AppLogger.w(TAG, "Ignoring unconfirmed speculative route recalculation failure", error);
        }
        return ignored;
    }

    @NonNull
    public static List<NavigationTurnEvent> applyConfirmedSpeculativeRouteResult(
            @NonNull NavigationSession session,
            @NonNull NavigationTextResources textResources,
            long routeAppliedAtElapsedMs,
            boolean singleInstructionMode
    ) {
        NavigationSpeculativeRouteState.PendingRouteResult result =
                session.components.speculativeRouteState.consumeConfirmedResult();
        if (result == null) {
            return Collections.emptyList();
        }
        session.components.routeRequestManager.onDeferredSpeculativeRouteApplied();
        session.components.routeState.setSingleInstructionMode(singleInstructionMode);
        session.components.warmupController.onRouteApplied(routeAppliedAtElapsedMs);
        NavigationLocation lastFiltered = session.components.locationState.getLastFilteredLocation();
        float speedMps = lastFiltered != null ? session.components.locationState.speedMps(lastFiltered) : 0f;
        return session.components.routeState.applyRouteResult(
                textResources,
                result.snapshot,
                result.route,
                lastFiltered,
                speedMps,
                session.components.locationState.isLikelyStationary(),
                result.beganAt
        );
    }

    public static boolean applyRouteFailure(
            @NonNull NavigationSession session,
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        boolean applied = session.components.routeRequestManager.onRouteFailure(textResources, snapshot, error);
        if (applied) {
            session.components.speculativeRouteState.onRouteFailed(snapshot);
        }
        return applied;
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
