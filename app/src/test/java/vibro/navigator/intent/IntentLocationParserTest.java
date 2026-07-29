package vibro.navigator.intent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IntentLocationParserTest {
    private static final String ACTION_SEND = "android.intent.action.SEND";
    private static final String ACTION_VIEW = "android.intent.action.VIEW";
    private static final String MILAN_COORDINATES = "45.4642,9.1900";
    private static final String VIENNA_COORDINATES = "48.0000,16.0000";
    private static final String MARGARETENSTRASSE_POI_COORDINATES = "48.1958755,16.3645947";
    private static final String MARGARETENSTRASSE_VIEWPORT_COORDINATES = "48.1960405,16.3640609";
    private static final String REGIONAL_GOOGLE_POI_COORDINATES = "48.1967292,16.3628378";
    private static final String REGIONAL_GOOGLE_VIEWPORT_COORDINATES = "48.196623,16.3615614";

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
    public void parseToQuery_repeatedQueryKeyUsesFirstNonEmptyValue() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "geo:0,0?q=&q=" + MILAN_COORDINATES + "&q=" + VIENNA_COORDINATES,
                null
        );

        assertEquals(MILAN_COORDINATES, parsed);
    }

    @Test
    public void parseToQuery_repeatedMapQueryKeyDoesNotOverwriteFirstValue() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "https://www.google.com/maps/search/?api=1&query=" + MILAN_COORDINATES
                        + "&query=" + VIENNA_COORDINATES,
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
    public void parseToQuery_googleMapsPlaceUrlPrefersPoiCoordinatesOverViewport() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "https://www.google.com/maps/place/Margaretenstra%C3%9Fe+25,+1040+Wien/"
                        + "@" + MARGARETENSTRASSE_VIEWPORT_COORDINATES + ",18z/data=!4m6!3m5"
                        + "!1s0x476d078150b0f74b:0x5cb88e606b2bdaa2!8m2"
                        + "!3d48.1958755!4d16.3645947!16s%2Fg%2F11c29xktv0?force=pwa",
                null
        );

        assertEquals(MARGARETENSTRASSE_POI_COORDINATES, parsed);
    }

    @Test
    public void parseToQuery_regionalGoogleMapsPlaceUrlPrefersPoiCoordinatesOverViewport() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "https://www.google.it/maps/place/48%C2%B011'48.2%22N+16%C2%B021'46.2%22E/"
                        + "@" + REGIONAL_GOOGLE_VIEWPORT_COORDINATES + ",17.86z/data=!4m4!3m3"
                        + "!8m2!3d48.1967292!4d16.3628378?entry=tts",
                null
        );

        assertEquals(REGIONAL_GOOGLE_POI_COORDINATES, parsed);
    }

    @Test
    public void parseToQuery_googleMapsPlaceUrlWithoutPoiDataFallsBackToViewportCoordinates() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "https://www.google.com/maps/place/Margaretenstra%C3%9Fe+25,+1040+Wien/"
                        + "@" + MARGARETENSTRASSE_VIEWPORT_COORDINATES + ",18z",
                null
        );

        assertEquals(MARGARETENSTRASSE_VIEWPORT_COORDINATES, parsed);
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
    public void parseToQuery_googleMapsUrlWithExplicitPositiveSigns_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "https://www.google.com/maps/search/?api=1&query=%2B45.4642,%2B9.1900",
                null
        );

        assertEquals("+45.4642,+9.1900", parsed);
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
    public void extractShortMapUrl_sharedGoogleMapsShortLink_returnsNormalizedUrl() {
        String shortUrl = IntentLocationParser.extractShortMapUrl(
                ACTION_SEND,
                null,
                "Meet me here: maps.app.goo.gl/abc123."
        );

        assertEquals("https://maps.app.goo.gl/abc123", shortUrl);
    }

    @Test
    public void parseToQuery_sharedGoogleMapsShortLinkWithoutExpansion_returnsNull() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_SEND,
                null,
                "https://maps.app.goo.gl/abc123"
        );

        assertNull(parsed);
    }

    @Test
    public void parseToQuery_sharedMapLinkWithTrailingPeriod_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_SEND,
                null,
                "Meet me here: https://maps.google.com/?q=" + MILAN_COORDINATES + "."
        );

        assertEquals(MILAN_COORDINATES, parsed);
    }

    @Test
    public void parseToQuery_sharedTextSkipsNonMapUrlBeforeMapLink() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_SEND,
                null,
                "Details: https://example.com/event Destination: "
                        + "https://maps.google.com/?q=Piazza%20del%20Duomo%2C%20Milan"
        );

        assertEquals("Piazza del Duomo, Milan", parsed);
    }

    @Test
    public void parseToQuery_nonMapGoogleUrl_returnsNull() {
        String parsed = IntentLocationParser.parseToQuery(
                ACTION_VIEW,
                "https://www.google.com/search?q=Piazza%20del%20Duomo",
                null
        );

        assertNull(parsed);
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
