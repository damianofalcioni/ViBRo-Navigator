package vibro.navigator.nav.export;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class NavigationRouteGpxFixDiagnosticsTest {
    private static final String NAMESPACE = "urn:vibro:navigator:gpx:1";

    @Test
    public void preservesAvailableMeasurementsWithEscapedProviderInOwnNamespace() throws Exception {
        NavigationLocation fix = new NavigationLocation("gps & fused");
        fix.setAccuracy(12.5f);
        fix.setSpeed(2.5f);
        fix.setBearing(90);
        Document document = parse(fix);
        assertEquals("gps & fused", document.getElementsByTagNameNS(NAMESPACE, "provider").item(0).getTextContent());
        assertEquals("12.5", document.getElementsByTagNameNS(NAMESPACE, "accuracyMeters").item(0).getTextContent());
        assertEquals("2.5", document.getElementsByTagNameNS(NAMESPACE, "speedMps").item(0).getTextContent());
        assertEquals("90.0", document.getElementsByTagNameNS(NAMESPACE, "bearingDegrees").item(0).getTextContent());
        assertEquals(0, document.getElementsByTagNameNS(NAMESPACE, "bearingAccuracyDegrees").getLength());
    }

    @Test
    public void omitsUnknownAndInvalidMeasurements() throws Exception {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setAccuracy(Float.NaN);
        Document document = parse(fix);
        assertEquals(0, document.getElementsByTagNameNS(NAMESPACE, "accuracyMeters").getLength());
        assertEquals(0, document.getElementsByTagNameNS(NAMESPACE, "speedMps").getLength());
    }

    private static Document parse(NavigationLocation fix) throws Exception {
        StringBuilder xml = new StringBuilder();
        NavigationRouteGpxFixDiagnostics.append(xml, fix);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml.toString())));
    }
}
