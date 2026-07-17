package vibro.navigator.nav.session;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NavigationRerouteFixPathTest {
    @Test
    public void pathRunsFromLastStableFixThroughOffRouteFixesToFinalAppliedRoute() {
        NavigationRerouteFixPath path = new NavigationRerouteFixPath();
        path.recordEvaluation(location(0.0, 0.001), stableEvaluation(), false);
        path.recordEvaluation(location(0.001, 0.002), tentativeDeviationEvaluation(), false);
        path.recordEvaluation(location(0.002, 0.003), deviationEvaluation(), true);
        path.onRouteApplied();

        assertTrue(path.recordEvaluation(location(0.003, 0.004), stableEvaluation(), true).isEmpty());
        path.onRouteApplied();
        List<LatLon> completed = path.recordEvaluation(
                location(0.004, 0.005),
                stableEvaluation(),
                false
        );

        assertEquals(5, completed.size());
        assertPoint(completed.get(0), 0.0, 0.001);
        assertPoint(completed.get(1), 0.001, 0.002);
        assertPoint(completed.get(2), 0.002, 0.003);
        assertPoint(completed.get(3), 0.003, 0.004);
        assertPoint(completed.get(4), 0.004, 0.005);
    }

    private static NavigationRouteEvaluation stableEvaluation() {
        return NavigationRouteEvaluation.keepRoute(Collections.emptyList(), 3_000L, true);
    }

    private static NavigationRouteEvaluation deviationEvaluation() {
        return NavigationRouteEvaluation.requestRecalculation(
                null,
                NavigationRouteRecalculationReason.ROUTE_DEVIATION
        );
    }

    private static NavigationRouteEvaluation tentativeDeviationEvaluation() {
        return NavigationRouteEvaluation.waitForDeviationConfirmation(RouteDeviationPolicy.Reason.OFF_TRACK);
    }

    private static NavigationLocation location(double lat, double lon) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAccuracy(5f);
        return location;
    }

    private static void assertPoint(LatLon point, double lat, double lon) {
        assertEquals(lat, point.lat, 0.0);
        assertEquals(lon, point.lon, 0.0);
    }
}
