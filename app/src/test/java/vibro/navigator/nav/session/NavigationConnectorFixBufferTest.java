package vibro.navigator.nav.session;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.assertEquals;

public class NavigationConnectorFixBufferTest {
    @Test
    public void recordedFixes15To18OmitTheBriefZShapeWithPlausibleAccuracy() {
        NavigationConnectorFixBuffer buffer = new NavigationConnectorFixBuffer();
        buffer.add(fix(40.307516, 17.668785, 1_000));
        buffer.add(fix(40.307683, 17.668692, 4_000));
        buffer.add(fix(40.307630, 17.668817, 5_000));
        assertEquals(3, buffer.points(true).size());
        buffer.add(fix(40.307863, 17.668602, 9_000));
        assertEquals(3, buffer.points(true).size());
        assertEquals(40.307683, buffer.points(true).get(1).lat, 0);
    }

    @Test
    public void genuineBacktrackingRetainsTheReversal() {
        NavigationConnectorFixBuffer buffer = new NavigationConnectorFixBuffer();
        buffer.add(fix(0, 0, 1_000));
        buffer.add(fix(0, 0.0002, 4_000));
        buffer.add(fix(0, 0.0001, 5_000));
        buffer.add(fix(0, 0, 8_000));
        assertEquals(4, buffer.points(true).size());
    }

    private static NavigationLocation fix(double lat, double lon, long time) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAccuracy(12);
        location.setTime(time);
        return location;
    }
}
