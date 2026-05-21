package vibro.navigator.nav.route;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RouteStartConnectorTest {

    @Test
    public void apply_keepsRouteWhenReturnedStartIsInsideThreshold() {
        GeoJsonRoute route = routeStartingAt(new LatLon(48.0, 16.00005));

        RouteStartConnector.Result result = RouteStartConnector.apply(route, new LatLon(48.0, 16.0), 3.0f);

        assertSame(route, result.route);
        assertFalse(result.connectorAdded);
    }

    @Test
    public void apply_prependsSyntheticBeelineWhenReturnedStartIsOutsideThreshold() {
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

        RouteStartConnector.Result result = RouteStartConnector.apply(route, requestedStart, 3.0f);

        double connectorDistanceMeters = GeoMath.distanceMeters(
                requestedStart.lat,
                requestedStart.lon,
                routeStart.lat,
                routeStart.lon
        );
        double connectorTimeSeconds = connectorDistanceMeters * 0.1;
        assertTrue(result.connectorAdded);
        assertEquals(connectorDistanceMeters, result.connectorDistanceMeters, 0.001);
        assertEquals(3, result.route.track.size());
        assertEquals(requestedStart.lat, result.route.track.get(0).lat, 0.0);
        assertEquals(requestedStart.lon, result.route.track.get(0).lon, 0.0);
        assertEquals(2, result.route.voiceHints.size());
        assertEquals(1, result.route.voiceHints.get(0).indexInTrack);
        assertEquals(16, result.route.voiceHints.get(0).command);
        assertEquals(connectorDistanceMeters, result.route.voiceHints.get(0).distanceToNextMeters, 0.001);
        assertEquals(2, result.route.voiceHints.get(1).indexInTrack);
        assertEquals(5, result.route.voiceHints.get(1).command);
        assertEquals(0.0, result.route.timesSeconds.get(0), 0.0);
        assertEquals(connectorTimeSeconds, result.route.timesSeconds.get(1), 0.001);
        assertEquals(10.0 + connectorTimeSeconds, result.route.timesSeconds.get(2), 0.001);
        assertEquals(10.0 + connectorTimeSeconds, result.route.totalTimeSeconds, 0.001);
        assertEquals(100.0 + connectorDistanceMeters, result.route.trackLengthMeters, 0.001);
    }

    @Test
    public void apply_syntheticConnectorMatchesRequestedStartOnRoute() {
        LatLon requestedStart = new LatLon(48.0, 16.0);
        GeoJsonRoute route = routeStartingAt(new LatLon(48.0, 16.0005));

        RouteStartConnector.Result result = RouteStartConnector.apply(route, requestedStart, 3.0f);

        PolylineIndex.Match match = new PolylineIndex(result.route.track).match(requestedStart, -1);
        assertNotNull(match);
        assertEquals(0.0, match.distanceToTrackMeters, 0.001);
        assertEquals(0.0, match.alongTrackMeters, 0.001);
    }

    @Test
    public void apply_dropsIncompleteTimingWhenAddingConnector() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(new LatLon(48.0, 16.0005), new LatLon(48.0, 16.0015)),
                Collections.emptyList(),
                Collections.singletonList(0.0),
                10.0,
                100.0
        );

        RouteStartConnector.Result result = RouteStartConnector.apply(route, new LatLon(48.0, 16.0), 3.0f);

        assertTrue(result.connectorAdded);
        assertTrue(result.route.timesSeconds.isEmpty());
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
