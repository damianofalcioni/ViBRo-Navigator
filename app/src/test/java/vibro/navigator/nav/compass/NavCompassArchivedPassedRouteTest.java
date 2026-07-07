package vibro.navigator.nav.compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

public class NavCompassArchivedPassedRouteTest {

    @Test
    public void buildCompassState_whenStationaryOverviewIncludesArchivedPassedRouteSegments() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.emptyList(),
                60.0,
                111.0
        );
        CompassRouteGeometry compassRouteGeometry = new CompassRouteGeometry(
                Arrays.asList(
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.0), 0.0),
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.001), 111.0)
                ),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList(Arrays.asList(
                        new LatLon(0.0, -0.012),
                        new LatLon(0.0, -0.006)
                ))
        );

        NavCompassState state = NavCompassStateFactory.buildCompassState(
                route,
                new PolylineIndex(route.track),
                0.0,
                locationAt(0.0, 0.0),
                0f,
                true,
                5f,
                0.0,
                null,
                null,
                null,
                0L,
                compassRouteGeometry,
                null,
                0L
        );

        assertNotNull(state);
        assertTrue(state.radiusState.visibleRadiusMeters > 1_000f);
        assertEquals(1, state.archivedPassedRouteSegments().segmentCount());
        assertTrue(state.passedRoutePoints.size() > state.passedRouteSamplePointCount());
    }

    private static NavigationLocation locationAt(double lat, double lon) {
        NavigationLocation location = new NavigationLocation("test");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(1L);
        return location;
    }
}
