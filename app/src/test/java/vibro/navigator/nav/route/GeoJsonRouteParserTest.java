package vibro.navigator.nav.route;

import org.junit.Test;

import vibro.navigator.geo.LatLon;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GeoJsonRouteParserTest {
    private static final String TYPE_FEATURE_COLLECTION = "\"type\":\"FeatureCollection\",";
    private static final String FEATURES_START = "\"features\":[{";
    private static final String TYPE_FEATURE = "\"type\":\"Feature\",";
    private static final String PROPERTIES_START = "\"properties\":{";
    private static final String GEOMETRY_START = "\"geometry\":{";
    private static final String TYPE_LINESTRING = "\"type\":\"LineString\",";
    private static final String THREE_POINT_COORDINATES = "[[16.0,48.0],[16.1,48.1],[16.2,48.2]]";

    @Test
    public void parsesMode9VoiceHintWithExtraGeometryField() {
        String geoJson = routeJson(
                "\"track-length\":\"1234\","
                + "\"total-time\":\"321\","
                + "\"times\":[0,60,120,180,240,300],"
                + "\"voicehints\":[[5,17,0,42.0,-10,\" (0)(0)\"]]",
                "[[16.0,48.0],[16.1,48.1],[16.2,48.2],[16.3,48.3],[16.4,48.4],[16.5,48.5]]"
        );

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
    public void parse_discardsVoiceHintsThatCannotMapToParsedTrack() {
        String geoJson = routeJson(
                "\"voicehints\":["
                        + "[1,2,0,42.0,-10],"
                        + "[3,5,0,42.0,90],"
                        + "[0,5,0,\"bad\",90]"
                        + "]",
                THREE_POINT_COORDINATES
        );

        GeoJsonRoute route = GeoJsonRouteParser.parse(geoJson);

        assertEquals(1, route.voiceHints.size());
        assertEquals(1, route.voiceHints.get(0).indexInTrack);
    }

    @Test
    public void parsesSpeedLimitSegmentsFromMessagesWayTags() {
        String geoJson = routeJson(
                "\"messages\":["
                + "[\"Longitude\",\"Latitude\",\"Elevation\",\"Distance\",\"CostPerKm\",\"ElevCost\","
                + "\"TurnCost\",\"NodeCost\",\"InitialCost\",\"WayTags\",\"NodeTags\",\"Time\",\"Energy\"],"
                + "[\"0\",\"0\",\"0\",\"50\",\"0\",\"0\",\"0\",\"0\",\"0\","
                + "\"highway=residential surface=asphalt maxspeed=30\",\"\",\"0\",\"0\"],"
                + "[\"0\",\"0\",\"0\",\"75\",\"0\",\"0\",\"0\",\"0\",\"0\","
                + "\"highway=primary maxspeed=50 mph\",\"\",\"0\",\"0\"],"
                + "[\"0\",\"0\",\"0\",\"25\",\"0\",\"0\",\"0\",\"0\",\"0\","
                + "\"highway=service surface=paved\",\"\",\"0\",\"0\"]"
                + "]",
                "[[16.0,48.0],[16.001,48.0],[16.002,48.0]]"
        );

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
    public void parse_discardsSpeedLimitRowsWithNonFiniteDistance() {
        String geoJson = routeJson(
                "\"messages\":["
                        + "[\"Distance\",\"WayTags\"],"
                        + "[\"Infinity\",\"maxspeed=30\"],"
                        + "[\"50\",\"maxspeed=50\"]"
                        + "]",
                THREE_POINT_COORDINATES
        );

        GeoJsonRoute route = GeoJsonRouteParser.parse(geoJson);

        assertEquals(1, route.speedLimitSegments.size());
        assertEquals(0.0, route.speedLimitSegments.get(0).startMeters, 0.0);
        assertEquals(50.0, route.speedLimitSegments.get(0).endMeters, 0.0);
        assertEquals(50, route.speedLimitAt(10.0).value);
        assertNull(route.speedLimitAt(60.0));
    }

    @Test
    public void parse_discardsRouteTimesWhenAnyTimingEntryIsInvalid() {
        String geoJson = routeJson("\"times\":[0,\"bad\",120]", THREE_POINT_COORDINATES);

        GeoJsonRoute route = GeoJsonRouteParser.parse(geoJson);

        assertEquals(3, route.track.size());
        assertTrue(route.timesSeconds.isEmpty());
    }

    @Test
    public void parse_discardsRouteTimesWhenTimingEntriesDecrease() {
        String geoJson = routeJson("\"times\":[0,120,60]", THREE_POINT_COORDINATES);

        GeoJsonRoute route = GeoJsonRouteParser.parse(geoJson);

        assertEquals(3, route.track.size());
        assertTrue(route.timesSeconds.isEmpty());
    }

    @Test
    public void parse_discardsRouteTimesWhenTimingEntryIsNegative() {
        String geoJson = routeJson("\"times\":[0,-1,120]", THREE_POINT_COORDINATES);

        GeoJsonRoute route = GeoJsonRouteParser.parse(geoJson);

        assertEquals(3, route.track.size());
        assertTrue(route.timesSeconds.isEmpty());
    }

    @Test
    public void parse_ignoresOutOfRangeTrackCoordinatesAndInvalidMetrics() {
        String geoJson = routeJson(
                "\"track-length\":\"-5\","
                + "\"total-time\":\"Infinity\"",
                "[[16.0,48.0],[181.0,48.1],[16.2,91.0],[16.3,48.3]]"
        );

        GeoJsonRoute route = GeoJsonRouteParser.parse(geoJson);

        assertEquals(2, route.track.size());
        assertEquals(0.0, route.totalTimeSeconds, 0.0);
        assertEquals(0.0, route.trackLengthMeters, 0.0);
    }

    @Test
    public void constructor_defensivelyCopiesRouteLists() {
        List<LatLon> track = new ArrayList<>();
        track.add(new LatLon(48.0, 16.0));
        List<VoiceHint> voiceHints = new ArrayList<>();
        voiceHints.add(new VoiceHint(0, 2, 0, 0.0, 0));
        List<Double> timesSeconds = new ArrayList<>();
        timesSeconds.add(0.0);
        List<RouteSpeedLimitSegment> speedLimitSegments = new ArrayList<>();
        speedLimitSegments.add(new RouteSpeedLimitSegment(
                0.0,
                10.0,
                new RouteSpeedLimit(30, RouteSpeedLimit.Unit.KILOMETERS_PER_HOUR)
        ));

        GeoJsonRoute route = new GeoJsonRoute(
                track,
                voiceHints,
                timesSeconds,
                speedLimitSegments,
                10.0,
                10.0
        );
        track.clear();
        voiceHints.clear();
        timesSeconds.clear();
        speedLimitSegments.clear();

        assertEquals(1, route.track.size());
        assertEquals(1, route.voiceHints.size());
        assertEquals(1, route.timesSeconds.size());
        assertEquals(1, route.speedLimitSegments.size());
        assertCannotMutate(route.track);
    }

    private static String routeJson(String properties, String coordinates) {
        return "{"
                + TYPE_FEATURE_COLLECTION
                + FEATURES_START
                + TYPE_FEATURE
                + PROPERTIES_START
                + properties
                + "},"
                + GEOMETRY_START
                + TYPE_LINESTRING
                + "\"coordinates\":" + coordinates
                + "}"
                + "}]"
                + "}";
    }

    private static void assertCannotMutate(List<LatLon> values) {
        try {
            values.clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Expected route list to be immutable");
    }
}
