package vibro.navigator.nav.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;

public class NavigationRouteGpxExporterTest {
    private static final String GPX_NAMESPACE = "http://www.topografix.com/GPX/1/1";
    private static final String TAG_WAYPOINT = "wpt";
    private static final String TAG_TRACK_POINT = "trkpt";
    private static final String TAG_TRACK_SEGMENT = "trkseg";
    private static final String TAG_NAME = "name";
    private static final String TAG_TYPE = "type";
    private static final String ATTR_LAT = "lat";
    private static final String ATTR_LON = "lon";
    private static final String TYPE_GPS_FIX = "vibro.navigator.gps-fix";
    private static final String TYPE_TURN = "vibro.navigator.turn";
    private static final String PASSED_ROUTE_NAME = "Passed route";
    private static final String FIRST_GPS_FIX_TIME = "1970-01-01T00:00:01.000Z";

    @Test
    public void export_includesTrackRouteAndInstructionWaypoints() throws Exception {
        GeoJsonRoute route = sampleRoute(Collections.singletonList(new VoiceHint(1, 2, 0, 25.0, -90)));

        Document document = parse(NavigationRouteGpxExporter.export(
                TestNavigationTextResources.metric(),
                route,
                Collections.singletonList(new LatLon(48.05, 16.05))
        ));

        assertEquals("gpx", document.getDocumentElement().getLocalName());
        assertEquals(3, document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_TRACK_POINT).getLength());
        assertEquals(3, document.getElementsByTagNameNS(GPX_NAMESPACE, "rtept").getLength());
        assertEquals(3, document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).getLength());
        Element turnWaypoint = (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).item(0);
        Element arrivalWaypoint = (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).item(1);
        Element stopWaypoint = (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).item(2);

        assertEquals("48.100000", turnWaypoint.getAttribute(ATTR_LAT));
        assertEquals("16.100000", turnWaypoint.getAttribute(ATTR_LON));
        assertEquals("Turn left", childText(turnWaypoint, TAG_NAME));
        assertTrue(childText(turnWaypoint, "desc").contains("25 m"));
        assertTrue(childText(turnWaypoint, "desc").contains("5 s"));
        assertEquals(TYPE_TURN, childText(turnWaypoint, TAG_TYPE));
        assertEquals("Destination reached", childText(arrivalWaypoint, TAG_NAME));
        assertEquals("48.050000", stopWaypoint.getAttribute(ATTR_LAT));
        assertEquals("16.050000", stopWaypoint.getAttribute(ATTR_LON));
        assertEquals("Stop 1", childText(stopWaypoint, TAG_NAME));
        assertEquals("vibro.navigator.stop", childText(stopWaypoint, TAG_TYPE));
        assertTrue(childText(document.getDocumentElement(), TAG_NAME).startsWith("ViBRo-Navigator Export "));
    }

    @Test
    public void export_usesExistingArrivalHintInsteadOfDuplicatingDestinationWaypoint() throws Exception {
        GeoJsonRoute route = sampleRoute(Arrays.asList(
                new VoiceHint(1, 5, 0, 40.0, 90),
                new VoiceHint(2, 100, 0, 0.0, 0)
        ));

        Document document = parse(NavigationRouteGpxExporter.export(
                TestNavigationTextResources.metric(),
                route,
                Collections.emptyList()
        ));

        assertEquals(2, document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).getLength());
        assertTrue(NavigationRouteGpxExporter.buildRouteName(new java.util.Date(0L))
                .matches("ViBRo-Navigator Export \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    public void export_includesPassedSegmentsPassedInstructionsAndGpsFixWaypoints() throws Exception {
        GeoJsonRoute currentRoute = sampleRoute(Collections.singletonList(new VoiceHint(1, 5, 0, 25.0, 90)));
        GeoJsonRoute passedRoute = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(47.0, 15.0),
                        new LatLon(47.1, 15.1),
                        new LatLon(47.2, 15.2)
                ),
                Arrays.asList(
                        new VoiceHint(1, 2, 0, 40.0, -90),
                        new VoiceHint(2, 7, 0, 30.0, 90)
                ),
                Arrays.asList(0.0, 10.0, 30.0),
                30.0,
                300.0
        );
        NavigationRouteGpxExportHistory history = new NavigationRouteGpxExportHistory(
                Collections.singletonList(new NavigationRouteGpxExportHistory.PassedRoute(
                        passedRoute,
                        Arrays.asList(passedRoute.track.get(0), passedRoute.track.get(1)),
                        1
                )),
                Collections.singletonList(location(48.5, 16.5, 1_000L))
        );

        Document document = parse(NavigationRouteGpxExporter.export(
                TestNavigationTextResources.metric(),
                currentRoute,
                Collections.emptyList(),
                history
        ));

        assertEquals(5, document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_TRACK_POINT).getLength());
        assertEquals(2, document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_TRACK_SEGMENT).getLength());
        assertEquals(4, document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).getLength());
        assertEquals(3, countWaypointsByType(document, TYPE_TURN));
        assertEquals(1, countWaypointsByType(document, TYPE_GPS_FIX));
        assertEquals(PASSED_ROUTE_NAME, childText(
                (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, "trk").item(0),
                TAG_NAME
        ));

        Element gpsFix = waypointByType(document, TYPE_GPS_FIX);
        assertEquals("48.500000", gpsFix.getAttribute(ATTR_LAT));
        assertEquals("16.500000", gpsFix.getAttribute(ATTR_LON));
        assertEquals("GPS fix 1", childText(gpsFix, TAG_NAME));
        assertEquals(FIRST_GPS_FIX_TIME, childText(gpsFix, "time"));
        assertEquals(0, countWaypointsByName(document, "Sharp right"));
    }

    @Test
    public void exportStraightLine_includesOnlyStopsDestinationAndStraightRoute() throws Exception {
        LatLon stop = new LatLon(48.1, 16.1);
        LatLon destination = new LatLon(48.2, 16.2);
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(48.0, 16.0),
                        stop,
                        destination
                ),
                Collections.emptyList(),
                0.0,
                0.0
        );

        Document document = parse(NavigationRouteGpxExporter.exportStraightLine(
                TestNavigationTextResources.metric(),
                route,
                Collections.singletonList(stop),
                destination
        ));

        assertEquals(3, document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_TRACK_POINT).getLength());
        assertEquals(3, document.getElementsByTagNameNS(GPX_NAMESPACE, "rtept").getLength());
        assertEquals(2, document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).getLength());
        assertEquals(0, countWaypointsByType(document, TYPE_TURN));
        assertEquals(1, countWaypointsByType(document, "vibro.navigator.stop"));
        assertEquals(1, countWaypointsByType(document, "vibro.navigator.destination"));
    }

    @Test
    public void export_omitsInstructionCountdownWhenTrackTimesDoNotAlign() throws Exception {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(48.0, 16.0),
                        new LatLon(48.1, 16.1),
                        new LatLon(48.2, 16.2),
                        new LatLon(48.3, 16.3)
                ),
                Arrays.asList(
                        new VoiceHint(0, 2, 0, 25.0, -90),
                        new VoiceHint(2, 3, 0, 40.0, 90)
                ),
                Arrays.asList(0.0, 5.0, 20.0),
                20.0,
                300.0
        );

        Document document = parse(NavigationRouteGpxExporter.export(
                TestNavigationTextResources.metric(),
                route,
                Collections.emptyList()
        ));

        Element turnWaypoint = (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).item(0);
        assertTrue(childText(turnWaypoint, "desc").contains("--"));
    }

    @Test
    public void export_omitsInvalidInstructionHintsInsteadOfClampingToRouteEndpoints() throws Exception {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(48.0, 16.0),
                        new LatLon(48.1, 16.1),
                        new LatLon(48.2, 16.2)
                ),
                Arrays.asList(
                        new VoiceHint(-1, 2, 0, 25.0, -90),
                        new VoiceHint(3, 3, 0, 40.0, 90),
                        new VoiceHint(1, 5, 0, 10.0, 0)
                ),
                Arrays.asList(0.0, 5.0, 20.0),
                20.0,
                300.0
        );

        Document document = parse(NavigationRouteGpxExporter.export(
                TestNavigationTextResources.metric(),
                route,
                Collections.emptyList()
        ));

        assertEquals(2, document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).getLength());
        Element turnWaypoint = (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).item(0);
        assertEquals("48.100000", turnWaypoint.getAttribute(ATTR_LAT));
        assertEquals("16.100000", turnWaypoint.getAttribute(ATTR_LON));
    }

    private static GeoJsonRoute sampleRoute(List<VoiceHint> hints) {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(48.0, 16.0),
                        new LatLon(48.1, 16.1),
                        new LatLon(48.2, 16.2)
                ),
                hints,
                Arrays.asList(0.0, 5.0, 20.0),
                20.0,
                300.0
        );
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static NavigationLocation location(double lat, double lon, long timeMs) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(5f);
        return location;
    }

    private static String childText(Element parent, String name) {
        return parent.getElementsByTagNameNS(GPX_NAMESPACE, name).item(0).getTextContent();
    }

    private static Element waypointByType(Document document, String type) {
        for (int i = 0; i < document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).getLength(); i++) {
            Element waypoint = (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).item(i);
            if (type.equals(childText(waypoint, TAG_TYPE))) {
                return waypoint;
            }
        }
        throw new AssertionError("Waypoint type not found: " + type);
    }

    private static int countWaypointsByType(Document document, String type) {
        int matches = 0;
        for (int i = 0; i < document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).getLength(); i++) {
            Element waypoint = (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).item(i);
            if (type.equals(childText(waypoint, TAG_TYPE))) {
                matches++;
            }
        }
        return matches;
    }

    private static int countWaypointsByName(Document document, String name) {
        int matches = 0;
        for (int i = 0; i < document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).getLength(); i++) {
            Element waypoint = (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).item(i);
            if (name.equals(childText(waypoint, TAG_NAME))) {
                matches++;
            }
        }
        return matches;
    }
}
