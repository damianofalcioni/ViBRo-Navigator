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
    public void select_withoutRouteGeometryUsesCurrentChunkOnly() {
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

        assertEquals(1, selection.displayKeys.size());
        assertEquals(1, selection.prefetchKeys.size());
    }

    @Test
    public void select_prefetchesRouteAheadBeyondDisplayedChunks() {
        SurroundingStreetChunkSelection selection = planner.select(
                compassState(90f, 90f, 1),
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

        assertTrue(selection.prefetchKeys.size() <= 64);
        assertTrue(selection.prefetchKeys.size() > selection.displayKeys.size());
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
