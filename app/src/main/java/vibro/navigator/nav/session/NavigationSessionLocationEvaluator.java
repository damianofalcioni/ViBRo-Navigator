package vibro.navigator.nav.session;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;

import java.util.Collections;

import vibro.navigator.logging.AppLogger;
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
    private final NavigationWarmupController warmupController;
    @NonNull
    private final NavigationRouteRequestManager routeRequestManager;
    private int acquiredFixCount;

    NavigationSessionLocationEvaluator(
            @NonNull NavigationSessionLocationState locationState,
            @NonNull NavigationSessionRouteState routeState,
            @NonNull NavigationWarmupController warmupController,
            @NonNull NavigationRouteRequestManager routeRequestManager
    ) {
        this.locationState = locationState;
        this.routeState = routeState;
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
            @NonNull Location location,
            long nowMs
    ) {
        NavigationSessionLocationState.Update update = locationState.onRawLocationChanged(location, nowMs);
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
