package vibro.navigator.android.intent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AndroidGoogleMapsSearchIntentTest {
    private static final String QUERY = "Cafe Central Vienna";
    private static final String MAPS_URL =
            "https://www.google.com/maps/search/?api=1&query=Cafe%20Central%20Vienna";

    @Test
    public void uriString_buildsGoogleMapsSearchUrl() {
        assertEquals(MAPS_URL, AndroidGoogleMapsSearchIntent.uriString(QUERY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void uriString_rejectsBlankQuery() {
        AndroidGoogleMapsSearchIntent.uriString(" ");
    }

    @Test
    public void uriString_encodesReservedCharacters() {
        assertEquals(
                "https://www.google.com/maps/search/?api=1&query=Cafe%20%26%20Bar",
                AndroidGoogleMapsSearchIntent.uriString("Cafe & Bar")
        );
    }
}
