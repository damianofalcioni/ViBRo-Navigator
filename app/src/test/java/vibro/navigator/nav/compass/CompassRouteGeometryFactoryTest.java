package vibro.navigator.nav.compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CompassRouteGeometryFactoryTest {

    @Test
    public void build_emptyRouteHasNoSamples() {
        GeoJsonRoute route = new GeoJsonRoute(Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);

        CompassRouteGeometry geometry = CompassRouteGeometryFactory.build(route, new PolylineIndex(route.track));

        assertEquals(0, geometry.routeSamplePointCount());
        assertEquals(0, geometry.hintSamplePointCount());
    }

    @Test
    public void build_singlePointRouteKeepsOneRouteSample() {
        GeoJsonRoute route = new GeoJsonRoute(
                Collections.singletonList(new LatLon(48.2, 16.3)),
                Collections.emptyList(),
                0.0,
                0.0
        );

        CompassRouteGeometry geometry = CompassRouteGeometryFactory.build(route, new PolylineIndex(route.track));

        assertEquals(1, geometry.routeSamplePointCount());
        assertNotNull(geometry.routeSamplePointAt(0));
        assertEquals(1, geometry.passedRoutePointCount(0.0));
    }

    @Test
    public void build_longRouteDownsamplesHintsForCompassRendering() {
        List<LatLon> track = new ArrayList<>();
        List<VoiceHint> hints = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            track.add(new LatLon(0.0, i * 0.001));
            hints.add(new VoiceHint(i, 2, 0, 0.0, 0));
        }
        GeoJsonRoute route = new GeoJsonRoute(track, hints, 120.0, 8_000.0);

        CompassRouteGeometry geometry = CompassRouteGeometryFactory.build(route, new PolylineIndex(route.track));

        assertEquals(48, geometry.hintSamplePointCount());
        assertTrue(geometry.routeSamplePointCount() > 2);
    }

    @Test
    public void build_shortRouteKeepsAllHints() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Arrays.asList(
                        new VoiceHint(1, 2, 0, 0.0, 0),
                        new VoiceHint(2, 3, 0, 0.0, 0)
                ),
                30.0,
                222.0
        );

        CompassRouteGeometry geometry = CompassRouteGeometryFactory.build(route, new PolylineIndex(route.track));

        assertEquals(2, geometry.hintSamplePointCount());
        assertNotNull(geometry.hintSamplePointAt(0));
        assertNotNull(geometry.hintSamplePointAt(1));
    }

    @Test
    public void build_projectsIntermediateStopsForCompassRendering() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Collections.emptyList(),
                30.0,
                222.0
        );

        CompassRouteGeometry geometry = CompassRouteGeometryFactory.build(
                route,
                new PolylineIndex(route.track),
                Collections.singletonList(new LatLon(0.0, 0.001))
        );

        assertEquals(1, geometry.intermediateSamplePointCount());
        assertNotNull(geometry.intermediateSamplePointAt(0));
    }

    @Test
    public void build_keepsArchivedPassedRouteSegmentsForCompassRendering() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.emptyList(),
                30.0,
                111.0
        );
        List<LatLon> archivedSegment = new ArrayList<>();
        archivedSegment.add(new LatLon(0.0, -0.002));
        archivedSegment.add(new LatLon(0.0, -0.001));
        List<List<LatLon>> archivedSegments = new ArrayList<>();
        archivedSegments.add(archivedSegment);

        CompassRouteGeometry geometry = CompassRouteGeometryFactory.build(
                route,
                new PolylineIndex(route.track),
                Collections.emptyList(),
                archivedSegments
        );
        archivedSegment.clear();
        archivedSegments.clear();

        assertEquals(1, geometry.archivedPassedRouteSegments().segmentCount());
        assertEquals(2, geometry.archivedPassedRouteSegments().samplePointCount(0));
        assertNotNull(geometry.archivedPassedRouteSegments().samplePointAt(0, 0));
        assertNotNull(geometry.archivedPassedRouteSegments().samplePointAt(0, 1));
    }

    @Test
    public void geometry_defensivelyCopiesSampleLists() {
        List<CompassRouteGeometry.SamplePoint> routeSamples = new ArrayList<>();
        routeSamples.add(new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.0), 0.0));
        List<LatLon> hintSamples = new ArrayList<>();
        hintSamples.add(new LatLon(0.0, 0.001));
        List<LatLon> intermediateSamples = new ArrayList<>();
        intermediateSamples.add(new LatLon(0.0, 0.002));

        CompassRouteGeometry geometry = new CompassRouteGeometry(
                routeSamples,
                hintSamples,
                intermediateSamples
        );
        routeSamples.clear();
        hintSamples.clear();
        intermediateSamples.clear();

        assertEquals(1, geometry.routeSamplePointCount());
        assertEquals(1, geometry.hintSamplePointCount());
        assertEquals(1, geometry.intermediateSamplePointCount());
    }

    @Test
    public void projectedCompassState_defensivelyCopiesRoutePointLists() {
        List<CompassRoutePoint> routePoints = new ArrayList<>();
        routePoints.add(new CompassRoutePoint(1f, 2f));

        NavCompassState state = NavCompassState.fromProjectedPoints(
                0f,
                null,
                1f,
                100f,
                5f,
                Collections.emptyList(),
                routePoints,
                Collections.emptyList(),
                0f,
                0f,
                true
        );
        routePoints.clear();

        assertEquals(1, state.routePoints.size());
        assertCannotMutateCompassPoints(state.routePoints);
    }

    private static void assertCannotMutateCompassPoints(List<CompassRoutePoint> values) {
        try {
            values.clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Expected compass route points to be immutable");
    }
}
