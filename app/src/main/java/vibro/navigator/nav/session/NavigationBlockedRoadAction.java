package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;

final class NavigationBlockedRoadAction {
    @NonNull
    private final NavigationSessionRouteState routeState;
    @NonNull
    private final StraightLineNavigationState straightLineState;

    NavigationBlockedRoadAction(
            @NonNull NavigationSessionRouteState routeState,
            @NonNull StraightLineNavigationState straightLineState
    ) {
        this.routeState = routeState;
        this.straightLineState = straightLineState;
    }

    boolean isAvailable(
            boolean started,
            boolean paused,
            @NonNull NavigationRequest request
    ) {
        return started && !paused && !request.isRoundTrip()
                && (request.isStraightLine()
                ? straightLineState.hasActiveTarget(request)
                : !routeState.isDestinationReached());
    }

    @NonNull
    NavigationBlockedRoadActionResult perform(
            boolean started,
            boolean paused,
            @NonNull NavigationRequest request,
            @Nullable NavigationLocation lastFiltered,
            long nowMs,
            @NonNull Runnable onTargetSkipped
    ) {
        if (!isAvailable(started, paused, request)) {
            return NavigationBlockedRoadActionResult.none();
        }
        NavigationBlockedRoadActionResult result = request.isStraightLine()
                ? skipStraightLineTarget(request)
                : routeState.performBlockedRoadAction(lastFiltered, nowMs);
        if (result.isTargetSkipped()) {
            onTargetSkipped.run();
        }
        return result;
    }

    @NonNull
    private NavigationBlockedRoadActionResult skipStraightLineTarget(
            @NonNull NavigationRequest request
    ) {
        return straightLineState.skipActiveTarget(request)
                ? NavigationBlockedRoadActionResult.targetSkipped()
                : NavigationBlockedRoadActionResult.none();
    }
}
