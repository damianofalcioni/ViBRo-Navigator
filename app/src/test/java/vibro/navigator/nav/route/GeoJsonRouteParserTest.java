package vibro.navigator.nav.route;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
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
}
