package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CompassRouteGeometryFactory {
    private static final int MAX_COMPASS_ROUTE_POINTS = 240;
    private static final int MAX_COMPASS_HINT_POINTS = 48;

    private CompassRouteGeometryFactory() {
    }

    @NonNull
    public static CompassRouteGeometry build(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index
    ) {
        return build(route, index, Collections.emptyList());
    }

    @NonNull
    public static CompassRouteGeometry build(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<LatLon> intermediateStops
    ) {
        return build(route, index, intermediateStops, Collections.emptyList());
    }

    @NonNull
    public static CompassRouteGeometry build(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<LatLon> intermediateStops,
            @NonNull List<List<LatLon>> archivedPassedRouteSegments
    ) {
        return build(route, index, intermediateStops, archivedPassedRouteSegments, Collections.emptyList());
    }

    @NonNull
    public static CompassRouteGeometry build(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<LatLon> intermediateStops,
            @NonNull List<List<LatLon>> archivedPassedRouteSegments,
            @NonNull List<List<LatLon>> recalculationBridgeSegments
    ) {
        return new CompassRouteGeometry(
                buildRouteSamplePoints(route, index),
                buildFullRoutePoints(route, index),
                buildHintSamplePoints(route, index),
                buildIntermediateSamplePoints(index, intermediateStops),
                archivedPassedRouteSegments,
                recalculationBridgeSegments
        );
    }

    @NonNull
    private static List<CompassRouteGeometry.SamplePoint> buildRouteSamplePoints(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index
    ) {
        return CompassRouteShapeSampler.sample(route.track, index, MAX_COMPASS_ROUTE_POINTS);
    }

    @NonNull
    private static List<CompassRouteGeometry.SamplePoint> buildFullRoutePoints(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index
    ) {
        List<CompassRouteGeometry.SamplePoint> points = new ArrayList<>(route.track.size());
        for (int i = 0; i < route.track.size(); i++) {
            points.add(new CompassRouteGeometry.SamplePoint(
                    route.track.get(i),
                    index.distanceAtPointIndex(i)
            ));
        }
        return points;
    }

    @NonNull
    private static List<LatLon> buildHintSamplePoints(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index
    ) {
        List<LatLon> hintSamplePoints = new ArrayList<>();
        if (route.voiceHints.isEmpty()) {
            return hintSamplePoints;
        }
        if (route.voiceHints.size() <= MAX_COMPASS_HINT_POINTS) {
            addHintSamplePoints(route, index, 0, route.voiceHints.size() - 1, hintSamplePoints);
            return hintSamplePoints;
        }
        addDownsampledHintPoints(route, index, hintSamplePoints);
        return hintSamplePoints;
    }

    private static void addDownsampledHintPoints(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<LatLon> hintSamplePoints
    ) {
        double hintStep = (route.voiceHints.size() - 1d) / (MAX_COMPASS_HINT_POINTS - 1d);
        int lastSelectedIndex = -1;
        for (int i = 0; i < MAX_COMPASS_HINT_POINTS; i++) {
            int selectedIndex = clampHintIndex(route, (int) Math.round(i * hintStep));
            if (selectedIndex == lastSelectedIndex) {
                continue;
            }
            addHintSamplePoints(route, index, selectedIndex, selectedIndex, hintSamplePoints);
            lastSelectedIndex = selectedIndex;
        }
    }

    private static int clampHintIndex(@NonNull GeoJsonRoute route, int selectedIndex) {
        return Math.min(route.voiceHints.size() - 1, Math.max(0, selectedIndex));
    }

    private static void addHintSamplePoints(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            int startHintIndex,
            int endHintIndex,
            @NonNull List<LatLon> target
    ) {
        for (int i = startHintIndex; i <= endHintIndex; i++) {
            VoiceHint hint = route.voiceHints.get(i);
            LatLon hintPoint = index.pointAtDistance(index.distanceAtPointIndex(hint.indexInTrack));
            if (hintPoint != null) {
                target.add(hintPoint);
            }
        }
    }

    @NonNull
    private static List<LatLon> buildIntermediateSamplePoints(
            @NonNull PolylineIndex index,
            @NonNull List<LatLon> intermediateStops
    ) {
        List<LatLon> intermediateSamplePoints = new ArrayList<>();
        for (LatLon stop : intermediateStops) {
            PolylineIndex.Match match = index.match(stop, -1);
            if (match == null) {
                continue;
            }
            LatLon point = index.pointAtDistance(match.alongTrackMeters);
            if (point != null) {
                intermediateSamplePoints.add(point);
            }
        }
        return intermediateSamplePoints;
    }
}
