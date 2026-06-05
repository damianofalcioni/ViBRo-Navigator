package vibro.navigator.intent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IntentLocationParserTest {
    private static final String ACTION_SEND = "android.intent.action.SEND";
    private static final String ACTION_VIEW = "android.intent.action.VIEW";
    private static final String MILAN_COORDINATES = "45.4642,9.1900";

    @Test
    public void parseToQuery_geoQueryCoordinates_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "geo:0,0?q=" + MILAN_COORDINATES + "(Milan)",
                null
        );

        assertEquals(MILAN_COORDINATES, parsed);
    }

    @Test
    public void parseToQuery_googleNavigationCoordinates_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "google.navigation:q=" + MILAN_COORDINATES + "&mode=w",
                null
        );

        assertEquals(MILAN_COORDINATES, parsed);
    }

    @Test
    public void parseToQuery_googleMapsUrl_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "https://www.google.com/maps/search/?api=1&query=" + MILAN_COORDINATES,
                null
        );

        assertEquals(MILAN_COORDINATES, parsed);
    }

    @Test
    public void parseToQuery_openStreetMapUrl_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "https://www.openstreetmap.org/?mlat=45.4642&mlon=9.1900#map=19/45.4642/9.1900",
                null
        );

        assertEquals(MILAN_COORDINATES, parsed);
    }

    @Test
    public void parseToQuery_openStreetMapFragmentOnlyUrl_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "https://www.openstreetmap.org/#map=19/45.4642/9.1900",
                null
        );

        assertEquals(MILAN_COORDINATES, parsed);
    }

    @Test
    public void parseToQuery_sharedMapLink_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_SEND,
                null,
                "https://maps.google.com/?q=" + MILAN_COORDINATES
        );

        assertEquals(MILAN_COORDINATES, parsed);
    }

    @Test
    public void parseToQuery_sharedAddress_returnsAddress() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_SEND,
                null,
                "Piazza del Duomo, Milan"
        );

        assertEquals("Piazza del Duomo, Milan", parsed);
    }

    @Test
    public void parseToQuery_invalidCoordinate_returnsNull() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "geo:0,0?q=123.456,9.1900",
                null
        );

        assertNull(parsed);
    }
}
