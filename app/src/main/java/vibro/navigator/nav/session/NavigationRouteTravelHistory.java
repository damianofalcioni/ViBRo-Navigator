package vibro.navigator.nav.session;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.export.NavigationRouteGpxExportHistory.PassedRoute;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteSection;

/** Stores travelled sections in occurrence order, independently of route request count. */
final class NavigationRouteTravelHistory {
    private final List<PassedRoute> archived = new ArrayList<>();
    private final List<List<LatLon>> bridges = new ArrayList<>();
    private final List<List<LatLon>> ordered = new ArrayList<>();
    private GeoJsonRoute activeRoute;
    private PolylineIndex activeIndex;
    private PolylineIndex.Match lastActiveMatch;
    private double entryMeters;

    void reset() {
        archived.clear();
        bridges.clear();
        ordered.clear();
        activeRoute = null;
        activeIndex = null;
        lastActiveMatch = null;
        entryMeters = 0;
    }

    void onRouteApplied(GeoJsonRoute route, PolylineIndex index,
            @Nullable PolylineIndex.Match previousMatch, boolean appendDirectBridge) {
        if (previousMatch != null) {
            recordProgress(previousMatch);
        }
        LatLon end = progressPoint();
        archiveActive();
        if (appendDirectBridge) {
            appendRecalculationBridgeSegment(RouteRecalculationBridge.segment(end,
                    RouteRecalculationBridge.firstRoutePoint(route)));
        }
        activeRoute = route;
        activeIndex = index;
        lastActiveMatch = null;
        entryMeters = 0;
    }

    void appendRecalculationBridgeSegment(List<LatLon> segment) {
        if (segment.size() >= 2) {
            List<LatLon> copy = RouteRecalculationBridge.copiedPoints(segment);
            bridges.add(copy);
            ordered.add(copy);
        }
    }

    void recordProgress(PolylineIndex.Match match) {
        lastActiveMatch = match;
    }

    void enterAt(PolylineIndex.Match match) {
        entryMeters = match.alongTrackMeters;
        lastActiveMatch = match;
    }

    void archiveActive() {
        PassedRoute section = activeSection();
        if (section != null) {
            archived.add(section);
            ordered.add(section.segment);
        }
        if (lastActiveMatch != null) {
            entryMeters = lastActiveMatch.alongTrackMeters;
        }
    }

    List<PassedRoute> passedRoutesSnapshot() {
        List<PassedRoute> result = new ArrayList<>(archived);
        PassedRoute active = activeSection();
        if (active != null) {
            result.add(active);
        }
        return Collections.unmodifiableList(result);
    }

    List<List<LatLon>> orderedSegmentsSnapshot() {
        List<List<LatLon>> result = new ArrayList<>(ordered);
        PassedRoute active = activeSection();
        if (active != null) {
            result.add(active.segment);
        }
        return result;
    }

    List<List<LatLon>> recalculationBridgeSegmentsSnapshot() {
        return RouteRecalculationBridge.copiedSegments(bridges);
    }

    @Nullable
    PolylineIndex.Match activeProgressMatch(@Nullable PolylineIndex.Match replacement) {
        return lastActiveMatch == null ? replacement : lastActiveMatch;
    }

    @Nullable
    LatLon progressPoint() {
        return activeIndex == null || lastActiveMatch == null ? null
                : activeIndex.pointAtDistance(lastActiveMatch.alongTrackMeters);
    }

    @Nullable
    GeoJsonRoute remainingRoute(double minimumAlongMeters) {
        return activeRoute == null ? null : RouteSection.between(activeRoute,
                Math.max(minimumAlongMeters, lastActiveMatch == null ? 0 : lastActiveMatch.alongTrackMeters),
                activeIndex.totalLengthMeters());
    }

    double entryMeters() {
        return entryMeters;
    }

    List<List<LatLon>> archivedSegmentsSnapshot() {
        List<List<LatLon>> result = new ArrayList<>();
        for (PassedRoute section : archived) {
            result.add(section.segment);
        }
        return result;
    }

    @Nullable
    private PassedRoute activeSection() {
        if (activeRoute == null || lastActiveMatch == null
                || lastActiveMatch.alongTrackMeters <= entryMeters) {
            return null;
        }
        GeoJsonRoute section = RouteSection.between(activeRoute, entryMeters, lastActiveMatch.alongTrackMeters);
        return new PassedRoute(section, section.track, section.track.size() - 1, true);
    }
}
