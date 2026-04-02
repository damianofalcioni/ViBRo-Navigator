package com.vibenavigator.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IntentLocationParserTest {

    @Test
    public void parseToQuery_geoQueryCoordinates_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                "android.intent.action.VIEW",
                "geo:0,0?q=45.4642,9.1900(Milan)",
                null
        );

        assertEquals("45.4642,9.1900", parsed);
    }

    @Test
    public void parseToQuery_googleNavigationCoordinates_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                "android.intent.action.VIEW",
                "google.navigation:q=45.4642,9.1900&mode=w",
                null
        );

        assertEquals("45.4642,9.1900", parsed);
    }

    @Test
    public void parseToQuery_googleMapsUrl_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                "android.intent.action.VIEW",
                "https://www.google.com/maps/search/?api=1&query=45.4642,9.1900",
                null
        );

        assertEquals("45.4642,9.1900", parsed);
    }

    @Test
    public void parseToQuery_openStreetMapUrl_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                "android.intent.action.VIEW",
                "https://www.openstreetmap.org/?mlat=45.4642&mlon=9.1900#map=19/45.4642/9.1900",
                null
        );

        assertEquals("45.4642,9.1900", parsed);
    }

    @Test
    public void parseToQuery_sharedMapLink_returnsCoordinatePair() {
        String parsed = IntentLocationParser.parseToQuery(
                "android.intent.action.SEND",
                null,
                "https://maps.google.com/?q=45.4642,9.1900"
        );

        assertEquals("45.4642,9.1900", parsed);
    }

    @Test
    public void parseToQuery_sharedAddress_returnsAddress() {
        String parsed = IntentLocationParser.parseToQuery(
                "android.intent.action.SEND",
                null,
                "Piazza del Duomo, Milan"
        );

        assertEquals("Piazza del Duomo, Milan", parsed);
    }

    @Test
    public void parseToQuery_invalidCoordinate_returnsNull() {
        String parsed = IntentLocationParser.parseToQuery(
                "android.intent.action.VIEW",
                "geo:0,0?q=123.456,9.1900",
                null
        );

        assertNull(parsed);
    }
}
