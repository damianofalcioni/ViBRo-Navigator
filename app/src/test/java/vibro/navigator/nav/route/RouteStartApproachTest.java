package vibro.navigator.nav.route;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RouteStartApproachTest {

    @Test
    public void plan_isInactiveWhenReturnedStartIsInsideThreshold() {
        GeoJsonRoute route = routeStartingAt(new LatLon(48.0, 16.00005));

        RouteStartApproach.Plan plan = RouteStartApproach.plan(route, new LatLon(48.0, 16.0), 3.0f);

        assertFalse(plan.active);
    }

    @Test
    public void plan_keepsOriginalRouteAndCreatesApproachTargetWhenReturnedStartIsOutsideThreshold() {
        LatLon requestedStart = new LatLon(48.0, 16.0);
        LatLon routeStart = new LatLon(48.0, 16.0005);
        LatLon routeEnd = new LatLon(48.0, 16.0015);
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(routeStart, routeEnd),
                Collections.singletonList(new VoiceHint(1, 5, 0, 12.0, 90)),
                Arrays.asList(0.0, 10.0),
                10.0,
                100.0
        );

        RouteStartApproach.Plan plan = RouteStartApproach.plan(route, requestedStart, 3.0f);

        double distanceMeters = GeoMath.distanceMeters(
                requestedStart.lat,
                requestedStart.lon,
                routeStart.lat,
                routeStart.lon
        );
        assertTrue(plan.active);
        assertEquals(distanceMeters, plan.distanceMeters, 0.001);
        assertNotNull(plan.target);
        assertEquals(routeStart.lat, plan.target.lat, 0.0);
        assertEquals(routeStart.lon, plan.target.lon, 0.0);
        assertEquals(2, route.track.size());
        assertEquals(1, route.voiceHints.size());
        assertEquals(10.0, route.totalTimeSeconds, 0.0);
        assertEquals(100.0, route.trackLengthMeters, 0.0);
    }

    @Test
    public void isInsideOriginalRouteThreshold_usesMatchDistanceToOriginalRoute() {
        GeoJsonRoute route = routeStartingAt(new LatLon(48.0, 16.0005));
        PolylineIndex.Match match = new PolylineIndex(route.track).match(new LatLon(48.0, 16.0), -1);

        assertNotNull(match);
        assertFalse(RouteStartApproach.isInsideOriginalRouteThreshold(match, 3.0));
    }

    @Test
    public void estimateApproachTime_prefersLiveSpeedWhenMoving() {
        assertEquals(10.0, RouteStartApproach.estimateApproachTimeSeconds(50.0, 5.0f, false), 0.001);
    }

    @Test
    public void estimateApproachTime_usesWalkingFallbackWhenStationary() {
        assertEquals(10.0, RouteStartApproach.estimateApproachTimeSeconds(14.0, 0.0f, true), 0.001);
    }

    private static GeoJsonRoute routeStartingAt(LatLon routeStart) {
        return new GeoJsonRoute(
                Arrays.asList(routeStart, new LatLon(routeStart.lat, routeStart.lon + 0.001)),
                Collections.emptyList(),
                Arrays.asList(0.0, 10.0),
                10.0,
                100.0
        );
    }
}
