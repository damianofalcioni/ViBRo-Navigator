package vibro.navigator.nav.streets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.location.NavigationLocation;

public class SurroundingStreetChunkPlannerTest {
    private final SurroundingStreetChunkPlanner planner = new SurroundingStreetChunkPlanner();

    @Test
    public void select_withoutRouteGeometryLoadsVisibleViewportChunks() {
        SurroundingStreetChunkSelection selection = planner.select(
                NavCompassState.fromProjectedPoints(
                        0f,
                        null,
                        1f,
                        90f,
                        5f,
                        true,
                        0f,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        0f,
                        0f,
                        true
                ),
                location(48.2082d, 16.3738d)
        );

        assertTrue(selection.displayKeys.size() > 1);
        assertEquals(selection.displayKeys.size(), selection.prefetchKeys.size());
    }

    @Test
    public void select_nearChunkCornerStillLoadsNeighboringViewportChunks() {
        SurroundingStreetChunkSelection selection = planner.select(
                NavCompassState.fromProjectedPoints(
                        0f,
                        null,
                        1f,
                        90f,
                        5f,
                        true,
                        0f,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        0f,
                        0f,
                        true
                ),
                location(0.0d, 0.0d)
        );

        assertTrue(selection.displayKeys.size() >= 4);
    }

    @Test
    public void select_displayCacheExpandsBeyondVisibleCompassRadius() {
        SurroundingStreetChunkSelection narrow = planner.select(
                compassState(90f, 90f, 1),
                location(48.2082d, 16.3738d)
        );
        SurroundingStreetChunkSelection wider = planner.select(
                compassState(300f, 300f, 1),
                location(48.2082d, 16.3738d)
        );

        assertTrue(wider.displayKeys.size() > narrow.displayKeys.size());
    }

    @Test
    public void select_prefetchesLateralRouteCorridor() {
        SurroundingStreetChunkSelection selection = planner.select(
                compassState(300f, 1_000f, 1),
                location(0.0d, 0.0d)
        );

        SurroundingStreetChunkKey current = SurroundingStreetChunkKey.from(0.0d, 0.0d);
        SurroundingStreetChunkKey lateral = SurroundingStreetChunkKey.fromIndexes(
                current.latIndex(),
                current.lonIndex() + 1
        );
        assertTrue(selection.prefetchKeys.contains(lateral));
    }

    @Test
    public void select_prefetchesRouteAheadBeyondDisplayedChunks() {
        SurroundingStreetChunkSelection selection = planner.select(
                compassState(90f, 1_000f, 1),
                location(0.0d, 0.0d)
        );

        assertTrue(selection.displayKeys.size() < selection.prefetchKeys.size());
        assertTrue(selection.prefetchKeys.size() > 1);
    }

    @Test
    public void select_capsHighSpeedRouteAheadChunks() {
        SurroundingStreetChunkSelection selection = planner.select(
                compassState(2_500f, 2_500f, 1),
                location(0.0d, 0.0d)
        );

        assertTrue(selection.prefetchKeys.size() <= 128);
        assertTrue(selection.prefetchKeys.size() > selection.displayKeys.size());
    }

    @Test
    public void select_capsLongRouteTransitionRadiusBeforeChunkEnumeration() {
        SurroundingStreetChunkSelection boundedSelection = planner.select(
                compassState(2_500f, 90f, 1),
                location(44.5068d, 11.3416d)
        );
        SurroundingStreetChunkSelection transitionSelection = planner.select(
                compassState(284_000f, 90f, 1),
                location(44.5068d, 11.3416d)
        );

        assertEquals(boundedSelection.displayKeys, transitionSelection.displayKeys);
        assertEquals(boundedSelection.prefetchKeys, transitionSelection.prefetchKeys);
        assertTrue(transitionSelection.displayKeys.size() <= 64);
        assertTrue(transitionSelection.prefetchKeys.size() <= 128);
    }

    private static NavCompassState compassState(
            float visibleRadiusMeters,
            float sixtySecondVisibleRadiusMeters,
            int passedRouteSamplePointCount
    ) {
        return NavCompassState.fromRouteGeometry(
                0f,
                null,
                1f,
                1f,
                1f,
                visibleRadiusMeters,
                10_000f,
                sixtySecondVisibleRadiusMeters,
                5f,
                true,
                0f,
                routeGeometry(),
                0.0d,
                0.0d,
                passedRouteSamplePointCount,
                0f,
                0f,
                5f,
                true
        );
    }

    private static CompassRouteGeometry routeGeometry() {
        List<CompassRouteGeometry.SamplePoint> samples = new ArrayList<>();
        for (int i = 0; i <= 60; i++) {
            double meters = i * 500.0d;
            samples.add(new CompassRouteGeometry.SamplePoint(
                    new LatLon(meters / 111_320.0d, 0.0d),
                    meters
            ));
        }
        return new CompassRouteGeometry(samples, Collections.emptyList());
    }

    private static NavigationLocation location(double lat, double lon) {
        NavigationLocation location = new NavigationLocation("test");
        location.setLatitude(lat);
        location.setLongitude(lon);
        return location;
    }
}
