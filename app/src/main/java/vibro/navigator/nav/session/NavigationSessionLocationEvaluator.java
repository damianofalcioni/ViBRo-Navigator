package vibro.navigator.nav.session;

import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;

import java.util.Collections;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.routing.NavigationRouteRequestManager;

final class NavigationSessionLocationEvaluator {
    private static final String TAG = "NavigationSession";
    private static final long NO_SUGGESTED_INTERVAL = -1L;

    @NonNull
    private final NavigationSessionLocationState locationState;
    @NonNull
    private final NavigationSessionRouteState routeState;
    @NonNull
    private final StraightLineNavigationState straightLineState;
    @NonNull
    private final NavigationWarmupController warmupController;
    @NonNull
    private final NavigationRouteRequestManager routeRequestManager;
    private int acquiredFixCount;

    NavigationSessionLocationEvaluator(
            @NonNull NavigationSessionLocationState locationState,
            @NonNull NavigationSessionRouteState routeState,
            @NonNull StraightLineNavigationState straightLineState,
            @NonNull NavigationWarmupController warmupController,
            @NonNull NavigationRouteRequestManager routeRequestManager
    ) {
        this.locationState = locationState;
        this.routeState = routeState;
        this.straightLineState = straightLineState;
        this.warmupController = warmupController;
        this.routeRequestManager = routeRequestManager;
    }

    void reset() {
        acquiredFixCount = 0;
    }

    int acquiredFixCount() {
        return acquiredFixCount;
    }

    @NonNull
    NavigationLocationUpdateResult onRawLocationChanged(
            @NonNull Context context,
            @NonNull NavigationRequest currentRequest,
            @NonNull NavigationLocation rawLocation,
            long nowMs
    ) {
        return onRawLocationChanged(
                new AndroidNavigationTextResources(context),
                currentRequest,
                rawLocation,
                nowMs
        );
    }

    @NonNull
    NavigationLocationUpdateResult onRawLocationChanged(
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRequest currentRequest,
            @NonNull NavigationLocation rawLocation,
            long nowMs
    ) {
        NavigationSessionLocationState.Update update = locationState.onRawLocationChanged(rawLocation, nowMs);
        if (update.isDropped()) {
            return NavigationLocationUpdateResult.dropped();
        }
        acquiredFixCount++;

        NavigationLocation filtered = update.getFilteredLocation();
        routeRequestManager.clearRouteFailure();
        if (!currentRequest.isComplete()) {
            routeRequestManager.markInvalidRequest(textResources);
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

        if (currentRequest.isStraightLine()) {
            long fastChecksUntilMs = warmupController.fastChecksUntilMsForEvaluation(nowMs);
            NavigationRouteEvaluation evaluation = straightLineState.evaluateLocation(
                    currentRequest,
                    filtered,
                    locationState.speedMps(filtered),
                    locationState.isLikelyStationary(),
                    locationState.accuracyMeters(filtered),
                    locationState.trustedActualBearingDegreesForReroute(filtered),
                    nowMs,
                    fastChecksUntilMs
            );
            warmupController.recordEvaluation(
                    evaluation.isStableOnRouteSample(),
                    locationState.accuracyMeters(filtered),
                    nowMs
            );
            return NavigationLocationUpdateResult.accepted(
                    filtered,
                    evaluation.shouldRecalculateRoute(),
                    evaluation.rerouteNotice,
                    evaluation.recalculationReason,
                    evaluation.getWrongDirectionNotice(),
                    evaluation.turnEvents,
                    evaluation.getSuggestedUpdateIntervalMs()
            );
        }

        long fastChecksUntilMs = warmupController.fastChecksUntilMsForEvaluation(nowMs);
        NavigationRouteEvaluation evaluation = routeState.evaluateLocation(
                filtered,
                locationState.speedMps(filtered),
                locationState.isLikelyStationary(),
                locationState.accuracyMeters(filtered),
                locationState.trustedActualBearingDegreesForReroute(filtered),
                nowMs,
                fastChecksUntilMs,
                update.isReacquiringAfterLongGap()
        );
        warmupController.recordEvaluation(evaluation.isStableOnRouteSample(), locationState.accuracyMeters(filtered), nowMs);
        return NavigationLocationUpdateResult.accepted(
                filtered,
                evaluation.shouldRecalculateRoute(),
                evaluation.rerouteNotice,
                evaluation.recalculationReason,
                evaluation.turnEvents,
                evaluation.getSuggestedUpdateIntervalMs()
        );
    }
}
