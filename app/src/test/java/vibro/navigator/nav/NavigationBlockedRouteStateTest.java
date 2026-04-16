package vibro.navigator.nav;

import static org.junit.Assert.assertEquals;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavigationBlockedRouteStateTest {

    @Test
    public void addBlockedPointsAhead_escalatesNearbyRepeatsAndReplacesOldMarkers() {
        NavigationBlockedRouteState state = new NavigationBlockedRouteState();
        PolylineIndex polylineIndex = new PolylineIndex(routeWithoutHints().track);

        state.reset();
        assertEquals(1, state.addBlockedPointsAhead(polylineIndex, 0.0, 10_000L).size());
        assertEquals(12.0, state.copyBlockedPoints().get(0).radiusMeters, 0.0);

        assertEquals(2, state.addBlockedPointsAhead(polylineIndex, 0.0, 12_000L).size());
        assertEquals(2, state.copyBlockedPoints().size());
        assertEquals(18.0, state.copyBlockedPoints().get(0).radiusMeters, 0.0);
        assertEquals(18.0, state.copyBlockedPoints().get(1).radiusMeters, 0.0);
    }

    private static GeoJsonRoute routeWithoutHints() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        new LatLon(0.0, 0.003)
                ),
                Collections.emptyList(),
                180.0,
                333.0
        );
    }
}
