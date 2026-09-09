package vibro.navigator.nav.route;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import vibro.navigator.geo.LatLon;
import static org.junit.Assert.*;

public class RouteSectionTest {
    @Test
    public void slicePreservesOnlyIncludedHintsAndInterpolatedTimes() {
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0, 0.001),
                new LatLon(0, 0.002)), Arrays.asList(new VoiceHint(0, 2, 0, 100, -90),
                new VoiceHint(1, 5, 0, 100, 90)), Arrays.asList(0.0, 10.0, 30.0), 30, 222);
        double length = new PolylineIndex(route.track).totalLengthMeters();
        GeoJsonRoute section = RouteSection.between(route, length * 0.25, length * 0.75);
        assertEquals(3, section.track.size());
        assertEquals(0.0005, section.track.get(0).lon, 0.0000001);
        assertEquals(0.0015, section.track.get(2).lon, 0.0000001);
        assertEquals(Arrays.asList(0.0, 5.0, 15.0), section.timesSeconds);
        assertEquals(1, section.voiceHints.size());
        assertEquals(5, section.voiceHints.get(0).command);
        assertEquals(1, section.voiceHints.get(0).indexInTrack);
    }

    @Test
    public void emptyAndUntimedRoutesDoNotFabricateTiming() {
        GeoJsonRoute empty = new GeoJsonRoute(Collections.emptyList(), Collections.emptyList(), 0, 0);
        assertTrue(RouteSection.between(empty, 0, 1).track.isEmpty());
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0, 0.001)),
                Collections.emptyList(), 10, 111);
        assertTrue(RouteSection.between(route, 1, 50).timesSeconds.isEmpty());
    }
}
