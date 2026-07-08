package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import java.util.List;

import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;

final class NavigationSessionDirectionDetailsBuilder {
    @NonNull
    private final NavigationSessionLocationState locationState;
    @NonNull
    private final NavigationSessionRouteState routeState;
    @NonNull
    private final StraightLineNavigationState straightLineState;

    NavigationSessionDirectionDetailsBuilder(
            @NonNull NavigationSessionLocationState locationState,
            @NonNull NavigationSessionRouteState routeState,
            @NonNull StraightLineNavigationState straightLineState
    ) {
        this.locationState = locationState;
        this.routeState = routeState;
        this.straightLineState = straightLineState;
    }

    @NonNull
    List<String> build(
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRequest currentRequest
    ) {
        NavigationDisplaySnapshot snapshot = buildSnapshot(textResources);
        return currentRequest.isStraightLine()
                ? StraightLineNavigationGuidanceText.buildDetailLines(
                        currentRequest,
                        snapshot,
                        straightLineState.isDestinationReachedForDisplay(),
                        straightLineState.nextStopIndexForDisplay()
                )
                : routeState.buildDirectionDetails(snapshot);
    }

    @NonNull
    private NavigationDisplaySnapshot buildSnapshot(@NonNull NavigationTextResources textResources) {
        NavigationLocation lastFiltered = locationState.getLastFilteredLocation();
        float speedMps = lastFiltered != null ? locationState.speedMps(lastFiltered) : 0f;
        float displaySpeedMps = lastFiltered != null ? locationState.displaySpeedMps(lastFiltered) : 0f;
        boolean likelyStationary = locationState.isLikelyStationary();
        float accuracyMeters = lastFiltered != null
                ? locationState.accuracyMeters(lastFiltered)
                : Float.MAX_VALUE;
        return NavigationDisplaySnapshot.builder(textResources)
                .location(lastFiltered, speedMps, displaySpeedMps, likelyStationary, accuracyMeters)
                .build();
    }
}
