package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.export.NavigationRouteGpxExportHistory;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

final class NavigationRouteHistory {
    @NonNull
    private final NavigationRouteTravelHistory travelHistory = new NavigationRouteTravelHistory();
    @NonNull
    private final NavigationRerouteFixPath rerouteFixPath = new NavigationRerouteFixPath();

    void reset() {
        travelHistory.reset();
        rerouteFixPath.reset();
    }

    void onRouteApplied(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @Nullable PolylineIndex.Match previousRouteMatch,
            boolean appendDirectBridge
    ) {
        travelHistory.onRouteApplied(route, polylineIndex, previousRouteMatch, appendDirectBridge);
        rerouteFixPath.onRouteApplied();
    }

    void recordProgress(@NonNull PolylineIndex.Match match) {
        travelHistory.recordProgress(match);
    }

    boolean hasPendingRerouteFixPath() {
        return rerouteFixPath.isActive();
    }

    @Nullable
    PolylineIndex.Match lastActiveRouteMatch() {
        return travelHistory.activeProgressMatch(null);
    }

    @NonNull
    List<LatLon> recordRerouteFixPath(
            @NonNull NavigationLocation location,
            @NonNull NavigationRouteEvaluation evaluation,
            boolean routeCalculationInProgress
    ) {
        List<LatLon> completed = rerouteFixPath.recordEvaluation(
                location,
                evaluation,
                routeCalculationInProgress
        );
        if (completed.size() >= 2) {
            travelHistory.appendRecalculationBridgeSegment(completed);
        }
        return completed;
    }

    @NonNull
    List<NavigationRouteGpxExportHistory.PassedRoute> passedRoutesSnapshot() {
        return travelHistory.passedRoutesSnapshot();
    }

    @NonNull
    List<List<LatLon>> recalculationBridgeSegmentsSnapshot() {
        return travelHistory.recalculationBridgeSegmentsSnapshot();
    }
}
