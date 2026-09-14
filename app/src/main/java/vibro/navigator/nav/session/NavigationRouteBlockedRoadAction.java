package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.location.NavigationLocation;

final class NavigationRouteBlockedRoadAction {
    @NonNull
    private final NavigationRouteDirectGuidance directGuidance;
    @NonNull
    private final NavigationBlockedPointSelector blockedPointSelector;

    NavigationRouteBlockedRoadAction(
            @NonNull NavigationRouteDirectGuidance directGuidance,
            @NonNull NavigationBlockedPointSelector blockedPointSelector
    ) {
        this.directGuidance = directGuidance;
        this.blockedPointSelector = blockedPointSelector;
    }

    @NonNull
    NavigationBlockedRoadActionResult perform(
            @Nullable NavigationLocation lastFiltered,
            long nowMs
    ) {
        if (directGuidance.activeTarget() != null
                && directGuidance.skipActiveTarget(lastFiltered, nowMs)) {
            directGuidance.state().recovery.setTarget(directGuidance.activeTarget());
            return NavigationBlockedRoadActionResult.targetSkipped();
        }
        return NavigationBlockedRoadActionResult.blockedPointsAdded(
                blockedPointSelector.addBlockedPointsAhead(lastFiltered, nowMs)
        );
    }
}
