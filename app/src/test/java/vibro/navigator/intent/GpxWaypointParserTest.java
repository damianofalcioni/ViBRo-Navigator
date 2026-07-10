package vibro.navigator.intent;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class GpxWaypointParserTest {

    @Test
    public void parse_usesFinalWaypointAsDestinationAndEarlierWaypointsAsOrderedStops()
            throws IOException {
        GpxWaypointRoute route = parse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
                  <wpt lat="48.1000" lon="16.1000"><name>First stop</name></wpt>
                  <wpt lat="48.2000" lon="16.2000"><name>Second stop</name></wpt>
                  <wpt lat="48.3000" lon="16.3000"><name>Destination</name></wpt>
                </gpx>
                """);

        assertEquals("Destination", route.destination.name);
        assertEquals(48.3d, route.destination.lat, 0.0d);
        assertEquals(2, route.stops.size());
        assertEquals("First stop", route.stops.get(0).name);
        assertEquals("Second stop", route.stops.get(1).name);
    }

    @Test
    public void parse_usesCoordinatesAsTheVisibleFallbackForUnnamedWaypoint() throws IOException {
        GpxWaypointRoute route = parse("""
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <wpt lat="48.2082" lon="16.3738" />
                </gpx>
                """);

        assertEquals("48.208200, 16.373800", route.destination.displayLabel());
        assertEquals(0, route.stops.size());
    }

    @Test
    public void parse_ignoresInvalidWaypointCoordinatesButKeepsValidWaypoints() throws IOException {
        GpxWaypointRoute route = parse("""
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <wpt lat="48.1000" lon="16.1000"><name>Stop</name></wpt>
                  <wpt lat="not-a-coordinate" lon="16.2000"><name>Broken</name></wpt>
                  <wpt lat="48.3000" lon="16.3000"><name>Destination</name></wpt>
                </gpx>
                """);

        assertEquals("Destination", route.destination.name);
        assertEquals(1, route.stops.size());
        assertEquals("Stop", route.stops.get(0).name);
    }

    @Test
    public void parse_usesRoutePointsWhenWaypointsAreAbsent() throws IOException {
        GpxWaypointRoute route = parse("""
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <rte>
                    <rtept lat="48.1000" lon="16.1000"><name>Route stop</name></rtept>
                    <rtept lat="48.2000" lon="16.2000"><name>Route destination</name></rtept>
                  </rte>
                </gpx>
                """);

        assertEquals("Route destination", route.destination.name);
        assertEquals(1, route.stops.size());
        assertEquals("Route stop", route.stops.get(0).name);
    }

    @Test
    public void parse_usesFinalTrackPointAsDestinationWhenOnlyTrackGeometryExists()
            throws IOException {
        GpxWaypointRoute route = parse("""
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <metadata><name>Nuovo file 1</name></metadata>
                  <trk>
                    <name>Nuovo file 1</name>
                    <trkseg>
                      <trkpt lat="43.250142" lon="13.418704"><ele>130</ele></trkpt>
                      <trkpt lat="43.250109" lon="13.421583"><ele>129</ele></trkpt>
                      <trkpt lat="43.247268" lon="13.423606"><ele>132.52</ele></trkpt>
                    </trkseg>
                  </trk>
                </gpx>
                """);

        assertEquals(43.247268d, route.destination.lat, 0.0d);
        assertEquals(13.423606d, route.destination.lon, 0.0d);
        assertEquals(0, route.stops.size());
    }

    @Test
    public void parse_ignoresAppAnnotationWaypointsAndUsesTrackDestination()
            throws IOException {
        GpxWaypointRoute route = parse("""
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <wpt lat="48.1000" lon="16.1000">
                    <name>Turn left</name>
                    <type>vibro.navigator.turn</type>
                  </wpt>
                  <wpt lat="48.2000" lon="16.2000">
                    <name>GPS fix 1</name>
                    <type>vibro.navigator.gps-fix</type>
                  </wpt>
                  <trk>
                    <trkseg>
                      <trkpt lat="48.3000" lon="16.3000" />
                      <trkpt lat="48.4000" lon="16.4000" />
                    </trkseg>
                  </trk>
                </gpx>
                """);

        assertEquals(48.4d, route.destination.lat, 0.0d);
        assertEquals(16.4d, route.destination.lon, 0.0d);
        assertEquals(0, route.stops.size());
    }

    @Test
    public void parse_appendsGeometryDestinationWhenAppStopWaypointsHaveNoDestination()
            throws IOException {
        GpxWaypointRoute route = parse("""
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <wpt lat="48.1000" lon="16.1000">
                    <name>Stop 1</name>
                    <type>vibro.navigator.stop</type>
                  </wpt>
                  <rte>
                    <rtept lat="48.1000" lon="16.1000" />
                    <rtept lat="48.2000" lon="16.2000" />
                  </rte>
                </gpx>
                """);

        assertEquals(48.2d, route.destination.lat, 0.0d);
        assertEquals(16.2d, route.destination.lon, 0.0d);
        assertEquals(1, route.stops.size());
        assertEquals("Stop 1", route.stops.get(0).name);
    }

    @Test
    public void parse_rejectsDocumentsWithoutRouteOrTrackPoints() {
        assertThrows(IOException.class, () -> parse("""
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <metadata><name>Empty route</name></metadata>
                </gpx>
                """));
    }

    @Test
    public void parse_rejectsDocumentTypeDeclarations() {
        assertThrows(IOException.class, () -> parse("""
                <!DOCTYPE gpx [<!ENTITY waypoint "External">]>
                <gpx><wpt lat="48.2082" lon="16.3738"><name>&waypoint;</name></wpt></gpx>
                """));
    }

    @Test
    public void parse_rejectsDocumentTypeDeclarationsAcrossBufferBoundaries() {
        assertThrows(IOException.class, () -> new GpxWaypointParser().parse(new OneByteInputStream(
                """
                <!DOCTYPE gpx [<!ENTITY waypoint "External">]>
                <gpx><wpt lat="48.2082" lon="16.3738"><name>&waypoint;</name></wpt></gpx>
                """.getBytes(StandardCharsets.UTF_8)
        )));
    }

    private static GpxWaypointRoute parse(String gpx) throws IOException {
        return new GpxWaypointParser().parse(new ByteArrayInputStream(
                gpx.getBytes(StandardCharsets.UTF_8)
        ));
    }

    private static final class OneByteInputStream extends InputStream {
        private final byte[] bytes;
        private int offset;

        private OneByteInputStream(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public int read() {
            if (offset >= bytes.length) {
                return -1;
            }
            return bytes[offset++] & 0xff;
        }

        @Override
        public int read(byte[] buffer, int bufferOffset, int length) {
            if (offset >= bytes.length) {
                return -1;
            }
            buffer[bufferOffset] = bytes[offset++];
            return 1;
        }
    }
}
