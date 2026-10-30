package vibro.navigator.nav.route;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class NavigationRoundTripArrivalFreshnessTest {
    @Test
    public void aStaleOutsideFixCannotAuthorizeArrivalAtTheLoopStart() {
        LatLon origin = new LatLon(0, 0);
        LatLon outside = new LatLon(.0005, .0005);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(origin, new LatLon(0, .001), new LatLon(.001, .001), origin),
                Collections.emptyList(), 300, 380);
        PolylineIndex index = new PolylineIndex(route.track);
        NavigationRouteGeometryState state = new NavigationRouteGeometryState();
        state.loadRoute(route, true);
        assertFalse(observe(state, index, outside, 1_000, 15_000));
        assertFalse(observe(state, index, origin, 16_000, 16_000));
        assertFalse(observe(state, index, origin, 19_000, 19_000));
        assertFalse(observe(state, index, outside, 22_000, 22_000));
        assertFalse(observe(state, index, origin, 25_000, 25_000));
        assertTrue(observe(state, index, origin, 28_000, 28_000));
    }

    private static boolean observe(NavigationRouteGeometryState state, PolylineIndex index,
            LatLon point, long measured, long now) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(measured, measured);
        location.setLatitude(point.lat);
        location.setLongitude(point.lon);
        location.setAccuracy(5);
        return state.isWithinDestinationReachedRadius(location, 5, index.matchFromSegmentIndex(point, 2), now);
    }
}
