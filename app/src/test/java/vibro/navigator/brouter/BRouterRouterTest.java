package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;

public class BRouterRouterTest {
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
                "trekking",
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
                "trekking",
                Collections.emptyList()
        );

        assertEquals(0, route.track.size());
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
}
