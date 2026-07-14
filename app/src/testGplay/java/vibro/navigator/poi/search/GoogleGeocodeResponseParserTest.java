package vibro.navigator.poi.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

import vibro.navigator.poi.Poi;

public class GoogleGeocodeResponseParserTest {
    private static final String CAFE_CENTRAL = "Cafe Central";
    private static final String LOCALITY = "locality";
    private static final String ROUTE = "route";
    private static final String STREET_NUMBER = "street_number";
    private static final String VIENNA = "Vienna";

    @Test
    public void parseResults_filtersInvalidEntriesAndHonorsLimit() throws Exception {
        String body = "{"
                + "\"results\":["
                + "{\"formatted_address\":\"Vienna\",\"geometry\":{\"location\":{\"lat\":48.2082,\"lng\":16.3738}}},"
                + "{\"formatted_address\":\"\",\"geometry\":{\"location\":{\"lat\":48.0,\"lng\":16.0}}},"
                + "{\"formatted_address\":\"Missing Geometry\"},"
                + "{\"formatted_address\":\"Graz\",\"geometry\":{\"location\":{\"lat\":47.0707,\"lng\":15.4395}}}"
                + "]"
                + "}";

        List<Poi> results = GoogleGeocodeResponseParser.parseResults(body, 1);

        assertEquals(1, results.size());
        assertEquals("Vienna", results.get(0).name);
        assertEquals(48.2082d, results.get(0).lat, 0.0d);
        assertEquals(16.3738d, results.get(0).lon, 0.0d);
    }

    @Test
    public void parseResults_returnsEmptyListWhenResultsMissing() throws Exception {
        List<Poi> results = GoogleGeocodeResponseParser.parseResults("{\"status\":\"ZERO_RESULTS\"}", 5);

        assertEquals(0, results.size());
    }

    @Test
    public void parseResults_filtersOutOfRangeCoordinates() throws Exception {
        String body = "{"
                + "\"results\":["
                + "{\"formatted_address\":\"Invalid\",\"geometry\":{\"location\":{\"lat\":91.0,\"lng\":16.3738}}},"
                + "{\"formatted_address\":\"Vienna\",\"geometry\":{\"location\":{\"lat\":48.2082,\"lng\":16.3738}}}"
                + "]"
                + "}";

        List<Poi> results = GoogleGeocodeResponseParser.parseResults(body, 5);

        assertEquals(1, results.size());
        assertEquals("Vienna", results.get(0).name);
    }

    @Test
    public void parseResults_usesConciseLabelFromAddressComponents() throws Exception {
        String body = "{"
                + "\"results\":[{"
                + "\"formatted_address\":\"Cafe Central, Herrengasse 14, Innere Stadt, Vienna, Austria\","
                + "\"geometry\":{\"location\":{\"lat\":48.2100,\"lng\":16.3650}},"
                + "\"address_components\":["
                + component(CAFE_CENTRAL, "point_of_interest", "establishment") + ","
                + component("14", STREET_NUMBER) + ","
                + component("Herrengasse", ROUTE) + ","
                + component(VIENNA, LOCALITY) + ","
                + component(VIENNA, "administrative_area_level_1") + ","
                + component("1010", "postal_code") + ","
                + component("Austria", "country")
                + "]"
                + "}]"
                + "}";

        List<Poi> results = GoogleGeocodeResponseParser.parseResults(body, 5);

        assertEquals(1, results.size());
        assertEquals("Cafe Central, Herrengasse 14, Vienna", results.get(0).name);
        assertNotNull(results.get(0).details());
        assertEquals(VIENNA, results.get(0).details().addressDetails().get("city"));
    }

    @Test
    public void parseFirstFormattedAddress_returnsFirstAddress() throws Exception {
        String body = "{"
                + "\"results\":["
                + "{\"formatted_address\":\"Stephansplatz, Vienna\"},"
                + "{\"formatted_address\":\"Vienna, Austria\"}"
                + "]"
                + "}";

        assertEquals("Stephansplatz, Vienna", GoogleGeocodeResponseParser.parseFirstFormattedAddress(body));
    }

    @Test
    public void parseFirstFormattedAddress_usesConciseLabelFromAddressComponents() throws Exception {
        String body = "{"
                + "\"results\":[{"
                + "\"formatted_address\":\"1600 Amphitheatre Parkway, Mountain View, CA 94043, USA\","
                + "\"address_components\":["
                + component("1600", STREET_NUMBER) + ","
                + component("Amphitheatre Parkway", ROUTE) + ","
                + component("Mountain View", LOCALITY) + ","
                + component("California", "administrative_area_level_1") + ","
                + component("94043", "postal_code") + ","
                + component("United States", "country")
                + "]"
                + "}]"
                + "}";

        assertEquals(
                "1600 Amphitheatre Parkway, Mountain View",
                GoogleGeocodeResponseParser.parseFirstFormattedAddress(body)
        );
    }

    @Test
    public void parseFirstFormattedAddress_returnsNullWhenResultsMissing() throws Exception {
        assertEquals(null, GoogleGeocodeResponseParser.parseFirstFormattedAddress("{\"status\":\"ZERO_RESULTS\"}"));
    }

    @Test
    public void statusHelpersRecognizeValidationStatuses() throws Exception {
        assertTrue(GoogleGeocodeResponseParser.isOkStatus("{\"status\":\"OK\"}"));
        assertTrue(GoogleGeocodeResponseParser.isRequestDeniedStatus("{\"status\":\"REQUEST_DENIED\"}"));
        assertFalse(GoogleGeocodeResponseParser.isOkStatus("{\"status\":\"REQUEST_DENIED\"}"));
    }

    private static String component(String name, String... types) {
        StringBuilder out = new StringBuilder();
        out.append("{\"long_name\":\"").append(name).append("\",\"types\":[");
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append('"').append(types[i]).append('"');
        }
        return out.append("]}").toString();
    }
}
