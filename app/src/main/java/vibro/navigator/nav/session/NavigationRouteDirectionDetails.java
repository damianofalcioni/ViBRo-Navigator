package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavDirectionDetailsTextFactory;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.NavigationRouteGeometryState;
import vibro.navigator.nav.route.PolylineIndex;

final class NavigationRouteDirectionDetails {
    private NavigationRouteDirectionDetails() {
    }

    @NonNull
    static List<String> build(
            @NonNull NavigationDisplaySnapshot snapshot,
            @NonNull NavigationRouteGeometryState geometryState,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @Nullable LatLon routeStartApproachTarget,
            @NonNull List<NavTarget> targets
    ) {
        if (snapshot.lastFiltered == null || routeStartApproachTarget != null) {
            return routeStartApproachDetails(snapshot, routeStartApproachTarget);
        }
        GeoJsonRoute route = geometryState.route();
        PolylineIndex polylineIndex = geometryState.polylineIndex();
        if (route == null || polylineIndex == null) {
            return new ArrayList<>();
        }
        PolylineIndex.Match match = geometryState.match(snapshot.lastFiltered, snapshot.accuracyMeters);
        if (match == null) {
            return new ArrayList<>();
        }
        return NavDirectionDetailsTextFactory.buildRelativeLines(
                route,
                polylineIndex,
                match.alongTrackMeters,
                turnState.getNextHintIdx(),
                match.segmentIndex,
                progressTracker.resolveEtaSpeedMps(
                        snapshot.lastFiltered,
                        match.alongTrackMeters,
                        snapshot.accuracyMeters,
                        snapshot.likelyStationary
                ),
                snapshot.accuracyMeters,
                turnState.isDestinationReached(),
                turnState.getIntermediateDestinationReachedTrackIndex(),
                targets,
                snapshot.textResources
        );
    }

    @NonNull
    private static List<String> routeStartApproachDetails(
            @NonNull NavigationDisplaySnapshot snapshot,
            @Nullable LatLon routeStartApproachTarget
    ) {
        if (snapshot.lastFiltered == null || routeStartApproachTarget == null) {
            return new ArrayList<>();
        }
        return Collections.singletonList(NavigationRouteStartApproachText.buildLine(snapshot, routeStartApproachTarget));
    }
}
