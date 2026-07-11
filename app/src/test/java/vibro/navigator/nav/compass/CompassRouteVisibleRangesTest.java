package vibro.navigator.nav.compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;

public class CompassRouteVisibleRangesTest {
    @Test
    public void select_keepsReenteringRouteSectionsAsSeparateRanges() {
        CompassRouteGeometry geometry = geometryWithFullRoute(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.0005),
                new LatLon(0.0, 0.002),
                new LatLon(0.0, 0.01),
                new LatLon(0.0, 0.002),
                new LatLon(0.0, 0.0005)
        ));

        CompassRouteVisibleRanges ranges = CompassRouteVisibleRanges.select(
                geometry,
                0.0,
                0.0,
                80f,
                24f
        );

        assertEquals(2, ranges.rangeCount());
        assertEquals(0, ranges.startIndexAt(0));
        assertEquals(3, ranges.endIndexAt(0));
        assertEquals(4, ranges.startIndexAt(1));
        assertEquals(6, ranges.endIndexAt(1));
    }

    @Test
    public void spatialIndex_rejectsDistantBlocksBeforeLocalSegmentChecks() {
        List<LatLon> route = new ArrayList<>();
        for (int i = 0; i <= 96; i++) {
            route.add(new LatLon(0.0, i * 0.001));
        }
        CompassRouteSpatialIndex index = geometryWithFullRoute(route).fullRouteSpatialIndex();

        assertEquals(3, index.blockCount());
        assertTrue(index.intersects(0, -0.001, 0.001, -0.001, 0.01));
        assertFalse(index.intersects(2, -0.001, 0.001, -0.001, 0.01));
    }

    @Test
    public void compassState_selectsFullRouteRangesOnlyForMovingScale() {
        CompassRouteGeometry geometry = geometryWithFullRoute(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.0005),
                new LatLon(0.0, 0.002)
        ));
        NavCompassState moving = compassState(geometry, true);

        assertEquals(3, moving.fullRouteView.pointCount());
        assertTrue(moving.fullRouteView.isActive());
        assertEquals(1, moving.fullRouteView.rangeCount());
        CompassFullRouteView overview = moving.withDisplayMode(false, 1_000f).fullRouteView;
        assertFalse(overview.isActive());
        assertEquals(0, overview.rangeCount());
    }

    private static NavCompassState compassState(CompassRouteGeometry geometry, boolean movingScale) {
        return NavCompassState.fromRouteGeometry(
                0f,
                null,
                1f,
                1f,
                1f,
                100f,
                1_000f,
                100f,
                5f,
                movingScale,
                13f,
                geometry,
                0.0,
                0.0,
                1,
                0f,
                0f,
                10f,
                true
        );
    }

    private static CompassRouteGeometry geometryWithFullRoute(List<LatLon> fullRoute) {
        List<CompassRouteGeometry.SamplePoint> fullPoints = new ArrayList<>();
        for (int i = 0; i < fullRoute.size(); i++) {
            fullPoints.add(new CompassRouteGeometry.SamplePoint(fullRoute.get(i), i * 111.0));
        }
        List<CompassRouteGeometry.SamplePoint> overview = fullPoints.isEmpty()
                ? Collections.emptyList()
                : Arrays.asList(fullPoints.get(0), fullPoints.get(fullPoints.size() - 1));
        return new CompassRouteGeometry(
                overview,
                fullPoints,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
