package vibro.navigator.poi.search;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

import vibro.navigator.poi.Poi;

public class GoogleGeocodeResponseParserTest {

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
}
