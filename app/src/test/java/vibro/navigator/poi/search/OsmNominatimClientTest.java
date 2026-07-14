package vibro.navigator.poi.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

import vibro.navigator.poi.Poi;

public class OsmNominatimClientTest {
    private static final String MUSEUM_NAME = "Museum, Test City";
    private static final String OPENING_HOURS = "10:00-18:00";

    @Test
    public void parseReverseDisplayName_returnsDisplayName() throws Exception {
        String body = "{\"display_name\":\"Stephansplatz, Vienna, Austria\"}";

        assertEquals("Stephansplatz, Vienna, Austria", OsmNominatimClient.parseReverseDisplayName(body));
    }

    @Test
    public void parseReverseDisplayName_returnsNullWhenMissing() throws Exception {
        assertNull(OsmNominatimClient.parseReverseDisplayName("{}"));
    }

    @Test
    public void buildSearchUrl_requestsAddressDetailsExtraTagsAndEntrances() throws Exception {
        String url = OsmNominatimClient.buildSearchUrl("Cafe Central", 99);

        assertTrue(url.contains("&addressdetails=1"));
        assertTrue(url.contains("&extratags=1"));
        assertTrue(url.contains("&entrances=1"));
        assertTrue(url.endsWith("&limit=20"));
    }

    @Test
    public void parsePois_addsEntrancePoisAfterOriginalResult() throws Exception {
        String body = "[{"
                + "\"display_name\":\"" + MUSEUM_NAME + "\","
                + "\"lat\":\"48.2000000\","
                + "\"lon\":\"16.3000000\","
                + "\"address\":{\"road\":\"Main Street\"},"
                + "\"extratags\":{\"opening_hours\":\"" + OPENING_HOURS + "\"},"
                + "\"entrances\":[{"
                + "\"type\":\"main\","
                + "\"lat\":\"48.2001000\","
                + "\"lon\":\"16.3001000\","
                + "\"extratags\":{\"wheelchair\":\"yes\"}"
                + "}]"
                + "}]";

        List<Poi> pois = OsmNominatimClient.parsePois(body);

        assertEquals(2, pois.size());
        Poi original = pois.get(0);
        assertEquals(MUSEUM_NAME, original.displayLabel());
        assertNotNull(original.details());
        assertEquals(OPENING_HOURS, original.details().extraTags().get("opening_hours"));
        assertEquals("Main Street", original.details().addressDetails().get("road"));
        assertEquals(1, original.details().entrances().size());
        assertEquals("main", original.details().entrances().get(0).type());

        Poi entrance = pois.get(1);
        assertEquals(MUSEUM_NAME, entrance.displayLabel());
        assertEquals(48.2001d, entrance.lat, 0.0d);
        assertEquals(16.3001d, entrance.lon, 0.0d);
        assertNotNull(entrance.details());
        assertTrue(entrance.details().isEntrance());
        assertEquals("main", entrance.details().entranceType());
        assertEquals("yes", entrance.details().extraTags().get("wheelchair"));
        assertEquals("Main Street", entrance.details().addressDetails().get("road"));
    }

    @Test
    public void parsePois_keepsSingleMatchingEntranceOnlyInDetails() throws Exception {
        String body = "[{"
                + "\"display_name\":\"" + MUSEUM_NAME + "\","
                + "\"lat\":\"48.2000000\","
                + "\"lon\":\"16.3000000\","
                + "\"entrances\":[{"
                + "\"type\":\"main\","
                + "\"lat\":\"48.2000000\","
                + "\"lon\":\"16.3000000\""
                + "}]"
                + "}]";

        List<Poi> pois = OsmNominatimClient.parsePois(body);

        assertEquals(1, pois.size());
        assertNotNull(pois.get(0).details());
        assertEquals(1, pois.get(0).details().entrances().size());
    }
}
