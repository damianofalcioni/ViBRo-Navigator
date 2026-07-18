package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.RouteStartApproach;

public class BRouterRouterTest {
    private static final String PROFILE_TREKKING = "trekking";
    private static final LatLon START = new LatLon(48.0, 16.0);
    private static final LatLon END = new LatLon(48.1, 16.1);

    @Test
    public void routeGeoJson_parsesRouteFeatureAfterLeadingNonRouteFeature() throws Exception {
        String payload = "{"
                + "\"type\":\"FeatureCollection\","
                + "\"features\":["
                + "{\"type\":\"Feature\",\"properties\":{},"
                + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[16.0,48.0]}},"
                + "{\"type\":\"Feature\",\"properties\":{\"track-length\":\"100\"},"
                + "\"geometry\":{\"type\":\"LineString\","
                + "\"coordinates\":[[16.0,48.0],[16.1,48.1]]}}"
                + "]"
                + "}";

        GeoJsonRoute route = new BRouterRouter().routeGeoJson(
                new FixedPayloadClient(payload),
                START,
                Collections.emptyList(),
                END,
                PROFILE_TREKKING,
                Collections.emptyList()
        );

        assertEquals(2, route.track.size());
        assertEquals(100.0, route.trackLengthMeters, 0.0);
    }

    @Test
    public void routeGeoJson_acceptsEmptyGeoJsonAsTemporaryRouteState() throws Exception {
        String payload = "{\"type\":\"FeatureCollection\",\"features\":[]}";

        GeoJsonRoute route = new BRouterRouter().routeGeoJson(
                new FixedPayloadClient(payload),
                START,
                Collections.emptyList(),
                END,
                PROFILE_TREKKING,
                Collections.emptyList()
        );

        assertEquals(0, route.track.size());
    }

    @Test
    public void routeGeoJson_appendsBeelineToRequestedDestinationWhenBRouterSnapsEnd() throws Exception {
        String payload = routePayload(
                "\"track-length\":\"100\",\"total-time\":\"10\"",
                "[[16.0,48.0],[16.09,48.09]]"
        );

        GeoJsonRoute route = new BRouterRouter().routeGeoJson(
                new FixedPayloadClient(payload),
                START,
                Collections.emptyList(),
                END,
                PROFILE_TREKKING,
                Collections.emptyList()
        );

        assertEquals(3, route.track.size());
        assertPointEquals(END, route.track.get(2));
        assertEquals(1, route.voiceHints.size());
        assertEquals(RouteStartApproach.BEELINE_COMMAND, route.voiceHints.get(0).command);
        assertEquals(1, route.voiceHints.get(0).indexInTrack);
        assertTrue(route.trackLengthMeters > 100.0);
        assertTrue(route.totalTimeSeconds > 10.0);
        assertTrue(route.timesSeconds.isEmpty());
    }

    @Test
    public void routeGeoJson_movesExistingArrivalHintToRequestedFinalDestinationAfterBeeline() throws Exception {
        String payload = routePayload(
                "\"voicehints\":[[1,100,0,0.0,0]]",
                "[[16.0,48.0],[16.09,48.09]]"
        );

        GeoJsonRoute route = new BRouterRouter().routeGeoJson(
                new FixedPayloadClient(payload),
                START,
                Collections.emptyList(),
                END,
                PROFILE_TREKKING,
                Collections.emptyList()
        );

        assertEquals(3, route.track.size());
        assertPointEquals(END, route.track.get(2));
        assertEquals(2, route.voiceHints.size());
        assertEquals(RouteStartApproach.BEELINE_COMMAND, route.voiceHints.get(0).command);
        assertEquals(1, route.voiceHints.get(0).indexInTrack);
        assertEquals(100, route.voiceHints.get(1).command);
        assertEquals(2, route.voiceHints.get(1).indexInTrack);
    }

    @Test
    public void routeGeoJson_appendsOutAndBackBeelineSpurToRequestedIntermediateDestination() throws Exception {
        LatLon requestedStop = new LatLon(48.0, 16.051);
        LatLon snappedStop = new LatLon(48.0, 16.05);
        LatLon nextRoutePoint = new LatLon(48.05, 16.05);
        String payload = routePayload(
                "\"voicehints\":[[2,5,0,25.0,90]]",
                "[[16.0,48.0],[16.05,48.0],[16.05,48.05],[16.1,48.1]]"
        );

        GeoJsonRoute route = new BRouterRouter().routeGeoJson(
                new FixedPayloadClient(payload),
                START,
                Collections.singletonList(requestedStop),
                END,
                PROFILE_TREKKING,
                Collections.emptyList()
        );

        assertEquals(6, route.track.size());
        assertPointEquals(snappedStop, route.track.get(1));
        assertPointEquals(requestedStop, route.track.get(2));
        assertPointEquals(snappedStop, route.track.get(3));
        assertPointEquals(nextRoutePoint, route.track.get(4));
        assertPointEquals(END, route.track.get(5));
        assertEquals(3, route.voiceHints.size());
        assertEquals(RouteStartApproach.BEELINE_COMMAND, route.voiceHints.get(0).command);
        assertEquals(1, route.voiceHints.get(0).indexInTrack);
        assertEquals(RouteStartApproach.BEELINE_COMMAND, route.voiceHints.get(1).command);
        assertEquals(2, route.voiceHints.get(1).indexInTrack);
        assertEquals(5, route.voiceHints.get(2).command);
        assertEquals(4, route.voiceHints.get(2).indexInTrack);
    }

    @Test
    public void routeGeoJson_ordersIntermediateBeelineBeforeRouteHintAtSameSnappedPoint() throws Exception {
        LatLon requestedStop = new LatLon(48.0, 16.051);
        String payload = routePayload(
                "\"voicehints\":[[1,5,0,25.0,90]]",
                "[[16.0,48.0],[16.05,48.0],[16.05,48.05],[16.1,48.1]]"
        );

        GeoJsonRoute route = new BRouterRouter().routeGeoJson(
                new FixedPayloadClient(payload),
                START,
                Collections.singletonList(requestedStop),
                END,
                PROFILE_TREKKING,
                Collections.emptyList()
        );

        assertEquals(3, route.voiceHints.size());
        assertEquals(RouteStartApproach.BEELINE_COMMAND, route.voiceHints.get(0).command);
        assertEquals(1, route.voiceHints.get(0).indexInTrack);
        assertEquals(5, route.voiceHints.get(1).command);
        assertEquals(1, route.voiceHints.get(1).indexInTrack);
        assertEquals(RouteStartApproach.BEELINE_COMMAND, route.voiceHints.get(2).command);
        assertEquals(2, route.voiceHints.get(2).indexInTrack);
    }

    @Test
    public void routeGeoJson_marksCustomProfileRequest() throws Exception {
        CapturingClient client = new CapturingClient("{\"type\":\"FeatureCollection\",\"features\":[]}");

        new BRouterRouter().routeGeoJson(
                client,
                START,
                Collections.emptyList(),
                END,
                "custom-car",
                true,
                Collections.emptyList(),
                "avoid_path=1"
        );

        assertTrue(client.request.customProfile);
        assertEquals("custom-car", client.request.profile);
        assertEquals("avoid_path=1", client.request.profileParameters);
    }

    private static String routePayload(String properties, String coordinates) {
        return "{"
                + "\"type\":\"FeatureCollection\","
                + "\"features\":[{"
                + "\"type\":\"Feature\","
                + "\"properties\":{" + properties + "},"
                + "\"geometry\":{\"type\":\"LineString\",\"coordinates\":" + coordinates + "}"
                + "}]"
                + "}";
    }

    private static void assertPointEquals(LatLon expected, LatLon actual) {
        assertEquals(expected.lat, actual.lat, 0.0);
        assertEquals(expected.lon, actual.lon, 0.0);
    }

    private static final class FixedPayloadClient implements BRouterRouteClient {
        private final String payload;

        private FixedPayloadClient(String payload) {
            this.payload = payload;
        }

        @Override
        public String requestRoutePayload(BRouterRouteRequest request) {
            return payload;
        }
    }

    private static final class CapturingClient implements BRouterRouteClient {
        private final String payload;
        private BRouterRouteRequest request;

        private CapturingClient(String payload) {
            this.payload = payload;
        }

        @Override
        public String requestRoutePayload(BRouterRouteRequest request) {
            this.request = request;
            return payload;
        }
    }
}
