package vibro.navigator.nav.session;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.export.NavigationRouteGpxExportHistory;
import vibro.navigator.nav.export.NavigationRouteGpxExportHistory.PassedRoute;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;

/** Freezes history cheaply; route slicing and export copies happen on the export worker. */
final class NavigationRouteHistoryExportSnapshot {
    private final NavigationRouteTravelHistory travel;
    private final List<PassedRoute> instructions;
    private final List<LatLon> live;
    private final double minimumAlongMeters;

    NavigationRouteHistoryExportSnapshot(NavigationRouteTravelHistory travel,
            List<PassedRoute> instructions, List<LatLon> live, double minimumAlongMeters) {
        this.travel = travel;
        this.instructions = instructions;
        this.live = live;
        this.minimumAlongMeters = minimumAlongMeters;
    }

    GeoJsonRoute remainingRoute() {
        return travel.remainingRoute(minimumAlongMeters);
    }

    NavigationRouteGpxExportHistory history(List<NavigationLocation> acceptedFixes) {
        List<PassedRoute> passed = new ArrayList<>(travel.passedRoutesSnapshot());
        passed.addAll(instructions);
        return new NavigationRouteGpxExportHistory(passed,
                withLive(travel.recalculationBridgeSegmentsSnapshot()), acceptedFixes, orderedSegments());
    }

    List<List<LatLon>> orderedSegments() {
        return withLive(travel.orderedSegmentsSnapshot());
    }

    private List<List<LatLon>> withLive(List<List<LatLon>> segments) {
        List<List<LatLon>> result = new ArrayList<>(segments);
        if (live.size() >= 2) {
            result.add(live);
        }
        return result;
    }
}
