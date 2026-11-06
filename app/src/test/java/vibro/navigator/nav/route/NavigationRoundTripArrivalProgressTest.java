package vibro.navigator.nav.route;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class NavigationRoundTripArrivalProgressTest {
    @Test
    public void losingEndProgressRestartsConfirmationEvenInsideTheArrivalRadius() {
        LatLon origin = new LatLon(0, 0);
        LatLon outside = new LatLon(0, .001);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(origin, outside, new LatLon(.001, .001), origin),
                Collections.emptyList(), 380, 380);
        PolylineIndex index = new PolylineIndex(route.track);
        NavigationRouteGeometryState state = new NavigationRouteGeometryState();
        state.loadRoute(route, true);
        PolylineIndex.Match end = new PolylineIndex.Match(0, index.totalLengthMeters(), 0, 2);
        PolylineIndex.Match start = new PolylineIndex.Match(0, 0, 0, 0);
        assertFalse(observe(state, outside, index.match(outside, -1), 1_000));
        assertFalse(observe(state, origin, end, 4_000));
        assertFalse(observe(state, origin, start, 6_000));
        assertFalse(observe(state, origin, end, 7_000));
        assertTrue(observe(state, origin, end, 10_000));
    }

    private static boolean observe(NavigationRouteGeometryState state, LatLon point,
            PolylineIndex.Match match, long timeMs) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(timeMs, timeMs);
        location.setLatitude(point.lat);
        location.setLongitude(point.lon);
        location.setAccuracy(5);
        return state.isWithinDestinationReachedRadius(location, 5, match, timeMs);
    }
}
