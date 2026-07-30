package vibro.navigator.android.intent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AndroidGeoCoordinatesViewIntentTest {
    private static final double LAT = 48.2082d;
    private static final double LON = 16.3738d;
    private static final String GEO_URI_WITH_LABEL =
            "geo:48.208200,16.373800?q=48.208200,16.373800(Vienna%20Center)";
    private static final String GEO_URI_WITHOUT_LABEL = "geo:48.208200,16.373800?q=48.208200,16.373800";

    @Test
    public void uriString_buildsGeoUriWithCoordinateQuery() {
        assertEquals(GEO_URI_WITH_LABEL, AndroidGeoCoordinatesViewIntent.uriString(LAT, LON, "Vienna Center"));
    }

    @Test
    public void uriString_omitsBlankLabelFromCoordinateQuery() {
        assertEquals(GEO_URI_WITHOUT_LABEL, AndroidGeoCoordinatesViewIntent.uriString(LAT, LON, " "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void uriString_rejectsInvalidCoordinates() {
        AndroidGeoCoordinatesViewIntent.uriString(91.0d, 16.3738d, "Invalid");
    }

    @Test
    public void uriString_encodesReservedLabelCharacters() {
        assertEquals(
                "geo:48.208200,16.373800?q=48.208200,16.373800(Cafe%20%26%20Bar)",
                AndroidGeoCoordinatesViewIntent.uriString(LAT, LON, "Cafe & Bar")
        );
    }
}
