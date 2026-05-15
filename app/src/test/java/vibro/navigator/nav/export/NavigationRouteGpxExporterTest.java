package vibro.navigator.nav.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;

@RunWith(RobolectricTestRunner.class)
public class NavigationRouteGpxExporterTest {
    private static final String GPX_NAMESPACE = "http://www.topografix.com/GPX/1/1";
    private static final String TAG_WAYPOINT = "wpt";
    private static final String TAG_NAME = "name";
    private static final String TAG_TYPE = "type";

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void export_includesTrackRouteAndInstructionWaypoints() throws Exception {
        GeoJsonRoute route = sampleRoute(Collections.singletonList(new VoiceHint(1, 2, 0, 25.0, -90)));

        Document document = parse(NavigationRouteGpxExporter.export(
                context,
                route,
                Collections.singletonList(new LatLon(48.05, 16.05))
        ));

        assertEquals("gpx", document.getDocumentElement().getLocalName());
        assertEquals(3, document.getElementsByTagNameNS(GPX_NAMESPACE, "trkpt").getLength());
        assertEquals(3, document.getElementsByTagNameNS(GPX_NAMESPACE, "rtept").getLength());
        assertEquals(3, document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).getLength());
        Element turnWaypoint = (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).item(0);
        Element arrivalWaypoint = (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).item(1);
        Element stopWaypoint = (Element) document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).item(2);

        assertEquals("48.100000", turnWaypoint.getAttribute("lat"));
        assertEquals("16.100000", turnWaypoint.getAttribute("lon"));
        assertEquals("Turn left", childText(turnWaypoint, TAG_NAME));
        assertTrue(childText(turnWaypoint, "desc").contains("25 m"));
        assertTrue(childText(turnWaypoint, "desc").contains("5 s"));
        assertEquals("vibro.navigator.turn", childText(turnWaypoint, TAG_TYPE));
        assertEquals("Destination reached", childText(arrivalWaypoint, TAG_NAME));
        assertEquals("48.050000", stopWaypoint.getAttribute("lat"));
        assertEquals("16.050000", stopWaypoint.getAttribute("lon"));
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

        Document document = parse(NavigationRouteGpxExporter.export(context, route, Collections.emptyList()));

        assertEquals(2, document.getElementsByTagNameNS(GPX_NAMESPACE, TAG_WAYPOINT).getLength());
        assertTrue(NavigationRouteGpxExporter.buildRouteName(new java.util.Date(0L))
                .matches("ViBRo-Navigator Export \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
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

    private static String childText(Element parent, String name) {
        return parent.getElementsByTagNameNS(GPX_NAMESPACE, name).item(0).getTextContent();
    }
}
