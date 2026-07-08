package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.export.NavigationRouteGpxExportHistory;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

final class NavigationRouteTravelHistory {
    private static final double TRACK_INDEX_TOLERANCE_METERS = 1.0;
    private static final double DUPLICATE_POINT_TOLERANCE_DEGREES = 0.0000001;

    @NonNull
    private final List<NavigationRouteGpxExportHistory.PassedRoute> archivedPassedRoutes = new ArrayList<>();
    @NonNull
    private final List<List<LatLon>> recalculationBridgeSegments = new ArrayList<>();
    @Nullable
    private GeoJsonRoute activeRoute;
    @Nullable
    private PolylineIndex activePolylineIndex;
    @Nullable
    private PolylineIndex.Match lastActiveMatch;

    void reset() {
        archivedPassedRoutes.clear();
        recalculationBridgeSegments.clear();
        activeRoute = null;
        activePolylineIndex = null;
        lastActiveMatch = null;
    }

    void onRouteApplied(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @Nullable PolylineIndex.Match previousRouteMatch
    ) {
        LatLon bridgeStart = archiveActiveRoute(previousRouteMatch);
        appendBridgeSegment(bridgeStart, RouteRecalculationBridge.firstRoutePoint(route));
        activeRoute = route;
        activePolylineIndex = polylineIndex;
        lastActiveMatch = null;
    }

    void recordProgress(@NonNull PolylineIndex.Match match) {
        lastActiveMatch = match;
    }

    @NonNull
    List<NavigationRouteGpxExportHistory.PassedRoute> passedRoutesSnapshot() {
        List<NavigationRouteGpxExportHistory.PassedRoute> snapshot =
                new ArrayList<>(archivedPassedRoutes);
        NavigationRouteGpxExportHistory.PassedRoute active = activePassedRoute();
        if (active != null) {
            snapshot.add(active);
        }
        return snapshot.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(snapshot);
    }

    @NonNull
    List<List<LatLon>> recalculationBridgeSegmentsSnapshot() {
        if (recalculationBridgeSegments.isEmpty()) {
            return Collections.emptyList();
        }
        return RouteRecalculationBridge.copiedSegments(recalculationBridgeSegments);
    }

    @Nullable
    private LatLon archiveActiveRoute(@Nullable PolylineIndex.Match replacementMatch) {
        NavigationRouteGpxExportHistory.PassedRoute route = passedRouteFor(activeProgressMatch(replacementMatch), true);
        if (route != null) {
            archivedPassedRoutes.add(route);
            return RouteRecalculationBridge.lastPoint(route.segment);
        }
        return null;
    }

    @Nullable
    private NavigationRouteGpxExportHistory.PassedRoute activePassedRoute() {
        return passedRouteFor(lastActiveMatch, false);
    }

    @Nullable
    private PolylineIndex.Match activeProgressMatch(@Nullable PolylineIndex.Match replacementMatch) {
        if (replacementMatch == null || lastActiveMatch == null) {
            return replacementMatch != null ? replacementMatch : lastActiveMatch;
        }
        return replacementMatch.alongTrackMeters >= lastActiveMatch.alongTrackMeters
                ? replacementMatch
                : lastActiveMatch;
    }

    @Nullable
    private NavigationRouteGpxExportHistory.PassedRoute passedRouteFor(
            @Nullable PolylineIndex.Match match,
            boolean includeInstructionWaypoints
    ) {
        if (activeRoute == null || activePolylineIndex == null || match == null) {
            return null;
        }
        List<LatLon> segment = passedSegment(activeRoute, activePolylineIndex, match.alongTrackMeters);
        if (segment.size() < 2) {
            return null;
        }
        return new NavigationRouteGpxExportHistory.PassedRoute(
                activeRoute,
                segment,
                maxPassedTrackIndex(activeRoute, activePolylineIndex, match.alongTrackMeters),
                includeInstructionWaypoints
        );
    }

    @NonNull
    private static List<LatLon> passedSegment(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters
    ) {
        if (route.track.isEmpty() || alongTrackMeters <= 0.0) {
            return Collections.emptyList();
        }
        List<LatLon> segment = new ArrayList<>();
        for (int i = 0; i < route.track.size(); i++) {
            if (polylineIndex.distanceAtPointIndex(i) > alongTrackMeters + TRACK_INDEX_TOLERANCE_METERS) {
                break;
            }
            segment.add(copy(route.track.get(i)));
        }
        LatLon progressPoint = polylineIndex.pointAtDistance(alongTrackMeters);
        if (progressPoint != null) {
            appendDistinct(segment, progressPoint);
        }
        return segment;
    }

    private static int maxPassedTrackIndex(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters
    ) {
        int maxIndex = 0;
        int lastTrackIndex = Math.max(0, route.track.size() - 1);
        for (int i = 0; i <= lastTrackIndex; i++) {
            if (polylineIndex.distanceAtPointIndex(i) <= alongTrackMeters + TRACK_INDEX_TOLERANCE_METERS) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private static void appendDistinct(@NonNull List<LatLon> points, @NonNull LatLon point) {
        if (points.isEmpty() || !samePoint(points.get(points.size() - 1), point)) {
            points.add(copy(point));
        }
    }

    private void appendBridgeSegment(@Nullable LatLon from, @Nullable LatLon to) {
        List<LatLon> bridgeSegment = RouteRecalculationBridge.segment(from, to);
        if (!bridgeSegment.isEmpty()) {
            recalculationBridgeSegments.add(bridgeSegment);
        }
    }

    private static boolean samePoint(@NonNull LatLon first, @NonNull LatLon second) {
        return Math.abs(first.lat - second.lat) <= DUPLICATE_POINT_TOLERANCE_DEGREES
                && Math.abs(first.lon - second.lon) <= DUPLICATE_POINT_TOLERANCE_DEGREES;
    }

    @NonNull
    private static LatLon copy(@NonNull LatLon point) {
        return new LatLon(point.lat, point.lon);
    }
}
