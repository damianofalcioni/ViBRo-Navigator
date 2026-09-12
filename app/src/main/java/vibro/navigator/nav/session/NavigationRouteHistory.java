package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.ArrayList;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.export.NavigationRouteGpxExportHistory;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteSection;

final class NavigationRouteHistory {
    @NonNull
    private final NavigationRouteTravelHistory travelHistory = new NavigationRouteTravelHistory();
    @NonNull
    private final NavigationRerouteFixPath rerouteFixPath = new NavigationRerouteFixPath();
    private PolylineIndex.Match pendingMatch;
    private LatLon departurePoint;
    private double directLegEndMeters;
    private final NavigationTravelInstructions instructions = new NavigationTravelInstructions();
    private long revision;

    void reset() {
        revision++;
        travelHistory.reset();
        rerouteFixPath.reset();
        pendingMatch = null;
        departurePoint = null;
        directLegEndMeters = 0;
        instructions.reset();
    }

    void onRouteApplied(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @Nullable PolylineIndex.Match previousRouteMatch,
            boolean appendDirectBridge
    ) {
        revision++;
        directLegEndMeters = 0;
        travelHistory.onRouteApplied(route, polylineIndex, previousRouteMatch, appendDirectBridge);
        rerouteFixPath.onRouteApplied();
        pendingMatch = null;
        instructions.onRouteApplied(route, polylineIndex);
    }

    void recordProgress(@NonNull PolylineIndex.Match match) {
        pendingMatch = match;
        if (!rerouteFixPath.isActive()) {
            travelHistory.recordProgress(match);
        }
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
        boolean wasActive = rerouteFixPath.isActive();
        if (!wasActive) {
            instructions.rememberBoundary(location, lastActiveRouteMatch(), travelHistory.entryMeters());
        }
        LatLon previousPoint = travelHistory.progressPoint();
        List<LatLon> completed = rerouteFixPath.recordEvaluation(
                location,
                evaluation,
                routeCalculationInProgress
        );
        if (!wasActive && rerouteFixPath.isActive()) {
            departurePoint = previousPoint;
        }
        if (wasActive || rerouteFixPath.isActive()) {
            revision++;
        }
        return finishEvaluation(completed);
    }

    private List<LatLon> finishEvaluation(List<LatLon> completed) {
        if (!completed.isEmpty() && pendingMatch != null) {
            List<LatLon> connected = connectedPath(completed);
            travelHistory.enterAt(pendingMatch);
            RouteSection.appendDistinct(connected, travelHistory.progressPoint());
            travelHistory.appendRecalculationBridgeSegment(connected);
            departurePoint = null;
            directLegEndMeters = 0;
            return connected;
        }
        if (!rerouteFixPath.isActive() && pendingMatch != null) {
            travelHistory.recordProgress(pendingMatch);
            departurePoint = null;
        }
        return completed;
    }

    @NonNull
    List<NavigationRouteGpxExportHistory.PassedRoute> passedRoutesSnapshot() {
        List<NavigationRouteGpxExportHistory.PassedRoute> result = new ArrayList<>(travelHistory.passedRoutesSnapshot());
        result.addAll(instructions.snapshot());
        return result;
    }

    @NonNull
    List<List<LatLon>> recalculationBridgeSegmentsSnapshot() {
        List<List<LatLon>> result = new ArrayList<>(travelHistory.recalculationBridgeSegmentsSnapshot());
        List<LatLon> live = connectedPath(rerouteFixPath.snapshot());
        if (live.size() >= 2) {
            result.add(live);
        }
        return result;
    }

    void startApproach(@Nullable NavigationLocation location) {
        revision++;
        if (!rerouteFixPath.isActive()) {
            departurePoint = travelHistory.progressPoint();
        }
        rerouteFixPath.startApproach(location);
    }

    void startDirectLeg(
            NavigationLocation location,
            PolylineIndex.Match match,
            PolylineIndex.Match target,
            double reachedRadiusMeters
    ) {
        if (target.alongTrackMeters != directLegEndMeters) {
            directLegEndMeters = target.alongTrackMeters;
            revision++;
        }
        if (!rerouteFixPath.isActive()) {
            travelHistory.recordProgress(match);
            travelHistory.archiveActive();
            departurePoint = null;
            rerouteFixPath.startDirectLeg(
                    location,
                    travelHistory.progressPoint(),
                    reachedRadiusMeters
            );
        }
    }

    void skipDirectLegs(
            PolylineIndex.Match travelledMatch,
            PolylineIndex.Match completedTarget
    ) {
        directLegEndMeters = completedTarget.alongTrackMeters;
        revision++;
        travelHistory.recordProgress(travelledMatch);
        travelHistory.archiveActive();
        travelHistory.enterAt(completedTarget);
        pendingMatch = completedTarget;
    }

    void startDirectLegFromCurrentFix(
            NavigationLocation location,
            PolylineIndex.Match target
    ) {
        directLegEndMeters = target.alongTrackMeters;
        revision++;
        travelHistory.archiveActive();
        departurePoint = null;
        rerouteFixPath.startDirectLegFromCurrentFix(location);
    }

    void initializeRouteStart(NavigationLocation location, PolylineIndex.Match match) {
        if (!rerouteFixPath.isActive() && match != null) {
            travelHistory.enterAt(match);
            pendingMatch = match;
            rerouteFixPath.rememberStableFix(location);
            instructions.rememberBoundary(location, match, travelHistory.entryMeters());
            revision++;
        }
    }

    long revision() {
        return revision;
    }

    List<List<LatLon>> orderedSegmentsSnapshot() {
        List<List<LatLon>> result = travelHistory.orderedSegmentsSnapshot();
        List<LatLon> live = connectedPath(rerouteFixPath.snapshot());
        if (live.size() >= 2) {
            result.add(live);
        }
        return result;
    }

    @Nullable
    GeoJsonRoute remainingRoute() {
        return travelHistory.remainingRoute(directLegEndMeters);
    }

    double entryMeters() {
        return Math.max(directLegEndMeters, travelHistory.entryMeters());
    }

    List<List<LatLon>> archivedSegmentsSnapshot() {
        return travelHistory.archivedSegmentsSnapshot();
    }

    private List<LatLon> connectedPath(List<LatLon> fixes) {
        List<LatLon> result = new ArrayList<>();
        if (!fixes.isEmpty()) {
            RouteSection.appendDistinct(result, departurePoint);
            for (LatLon point : fixes) {
                RouteSection.appendDistinct(result, point);
            }
        }
        return result;
    }
}
