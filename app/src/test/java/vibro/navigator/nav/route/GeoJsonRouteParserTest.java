package vibro.navigator.nav.route;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GeoJsonRouteParserTest {

    @Test
    public void parsesMode9VoiceHintWithExtraGeometryField() {
        String geoJson = "{"
                + "\"type\":\"FeatureCollection\","
                + "\"features\":[{"
                + "\"type\":\"Feature\","
                + "\"properties\":{"
                + "\"track-length\":\"1234\","
                + "\"total-time\":\"321\","
                + "\"times\":[0,60,120,180,240,300],"
                + "\"voicehints\":[[5,17,0,42.0,-10,\" (0)(0)\"]]"
                + "},"
                + "\"geometry\":{"
                + "\"type\":\"LineString\","
                + "\"coordinates\":[[16.0,48.0],[16.1,48.1],[16.2,48.2],[16.3,48.3],[16.4,48.4],[16.5,48.5]]"
                + "}"
                + "}]"
                + "}";

        GeoJsonRoute route = GeoJsonRouteParser.parse(geoJson);

        assertEquals(6, route.track.size());
        assertEquals(1, route.voiceHints.size());
        assertEquals(5, route.voiceHints.get(0).indexInTrack);
        assertEquals(17, route.voiceHints.get(0).command);
        assertEquals(0, route.voiceHints.get(0).exitNumber);
        assertEquals(42.0, route.voiceHints.get(0).distanceToNextMeters, 0.0);
        assertEquals(-10, route.voiceHints.get(0).angleDegrees);
        assertEquals(6, route.timesSeconds.size());
        assertEquals(300.0, route.timesSeconds.get(5), 0.0);
    }

    @Test
    public void parsesSpeedLimitSegmentsFromMessagesWayTags() {
        String geoJson = "{"
                + "\"type\":\"FeatureCollection\","
                + "\"features\":[{"
                + "\"type\":\"Feature\","
                + "\"properties\":{"
                + "\"messages\":["
                + "[\"Longitude\",\"Latitude\",\"Elevation\",\"Distance\",\"CostPerKm\",\"ElevCost\","
                + "\"TurnCost\",\"NodeCost\",\"InitialCost\",\"WayTags\",\"NodeTags\",\"Time\",\"Energy\"],"
                + "[\"0\",\"0\",\"0\",\"50\",\"0\",\"0\",\"0\",\"0\",\"0\","
                + "\"highway=residential surface=asphalt maxspeed=30\",\"\",\"0\",\"0\"],"
                + "[\"0\",\"0\",\"0\",\"75\",\"0\",\"0\",\"0\",\"0\",\"0\","
                + "\"highway=primary maxspeed=50 mph\",\"\",\"0\",\"0\"],"
                + "[\"0\",\"0\",\"0\",\"25\",\"0\",\"0\",\"0\",\"0\",\"0\","
                + "\"highway=service surface=paved\",\"\",\"0\",\"0\"]"
                + "]"
                + "},"
                + "\"geometry\":{"
                + "\"type\":\"LineString\","
                + "\"coordinates\":[[16.0,48.0],[16.001,48.0],[16.002,48.0]]"
                + "}"
                + "}]"
                + "}";

        GeoJsonRoute route = GeoJsonRouteParser.parse(geoJson);

        assertEquals(2, route.speedLimitSegments.size());
        assertEquals(0.0, route.speedLimitSegments.get(0).startMeters, 0.0);
        assertEquals(50.0, route.speedLimitSegments.get(0).endMeters, 0.0);
        assertEquals(30, route.speedLimitSegments.get(0).speedLimit.value);
        assertEquals(RouteSpeedLimit.Unit.KILOMETERS_PER_HOUR, route.speedLimitAt(10.0).unit);
        assertEquals(50, route.speedLimitAt(60.0).value);
        assertEquals(RouteSpeedLimit.Unit.MILES_PER_HOUR, route.speedLimitAt(60.0).unit);
        assertNull(route.speedLimitAt(140.0));
    }

    @Test
    public void parse_discardsRouteTimesWhenAnyTimingEntryIsInvalid() {
        String geoJson = "{"
                + "\"type\":\"FeatureCollection\","
                + "\"features\":[{"
                + "\"type\":\"Feature\","
                + "\"properties\":{"
                + "\"times\":[0,\"bad\",120]"
                + "},"
                + "\"geometry\":{"
                + "\"type\":\"LineString\","
                + "\"coordinates\":[[16.0,48.0],[16.1,48.1],[16.2,48.2]]"
                + "}"
                + "}]"
                + "}";

        GeoJsonRoute route = GeoJsonRouteParser.parse(geoJson);

        assertEquals(3, route.track.size());
        assertTrue(route.timesSeconds.isEmpty());
    }
}
