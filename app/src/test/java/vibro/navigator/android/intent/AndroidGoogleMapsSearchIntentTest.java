package vibro.navigator.android.intent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AndroidGoogleMapsSearchIntentTest {
    private static final String QUERY = "Cafe Central Vienna";
    private static final String MAPS_URL =
            "https://www.google.com/maps/search/?api=1&query=Cafe%20Central%20Vienna";

    @Test
    public void createAppIntent_targetsGoogleMapsPackage() {
        Intent intent = AndroidGoogleMapsSearchIntent.createAppIntent(QUERY);

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(MAPS_URL, intent.getDataString());
        assertEquals(AndroidGoogleMapsSearchIntent.GOOGLE_MAPS_PACKAGE, intent.getPackage());
    }

    @Test
    public void createWebIntent_buildsGoogleMapsSearchUrl() {
        Intent intent = AndroidGoogleMapsSearchIntent.createWebIntent(QUERY);

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(MAPS_URL, intent.getDataString());
        assertNull(intent.getPackage());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWebIntent_rejectsBlankQuery() {
        AndroidGoogleMapsSearchIntent.createWebIntent(" ");
    }
}
