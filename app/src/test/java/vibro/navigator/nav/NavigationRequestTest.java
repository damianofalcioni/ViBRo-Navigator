package vibro.navigator.nav;

import android.content.Intent;

import vibro.navigator.geo.LatLon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class NavigationRequestTest {

    @Test
    public void putInto_thenFromIntent_roundTripsRequestFields() {
        NavigationRequest original = new NavigationRequest(
                "trekking",
                "Vienna Center",
                new LatLon(48.2082d, 16.3738d),
                Arrays.asList(
                        new LatLon(48.2100d, 16.3600d),
                        new LatLon(48.2200d, 16.3900d)
                )
        );

        Intent intent = new Intent();
        original.putInto(intent);

        NavigationRequest restored = NavigationRequest.fromIntent(intent);

        assertTrue(restored.isComplete());
        assertEquals("trekking", restored.profile);
        assertEquals("Vienna Center", restored.destinationName);
        assertEquals(48.2082d, restored.destination.lat, 0.0);
        assertEquals(16.3738d, restored.destination.lon, 0.0);
        assertEquals(2, restored.stops.size());
        assertEquals(48.2100d, restored.stops.get(0).lat, 0.0);
        assertEquals(16.3600d, restored.stops.get(0).lon, 0.0);
        assertEquals(48.2200d, restored.stops.get(1).lat, 0.0);
        assertEquals(16.3900d, restored.stops.get(1).lon, 0.0);
    }

    @Test
    public void fromIntent_discardsInvalidStopsAndRequiresProfile() {
        Intent intent = new Intent();
        intent.putExtra(NavigationRequest.EXTRA_DEST_NAME, "Test destination");
        intent.putExtra(NavigationRequest.EXTRA_DEST_LAT, 48.2082d);
        intent.putExtra(NavigationRequest.EXTRA_DEST_LON, 16.3738d);
        intent.putStringArrayListExtra(
                NavigationRequest.EXTRA_STOPS,
                new ArrayList<>(Arrays.asList("48.2,16.3", "bad", "12"))
        );

        NavigationRequest restored = NavigationRequest.fromIntent(intent);

        assertFalse(restored.isComplete());
        assertEquals("Test destination", restored.destinationName);
        assertEquals(1, restored.stops.size());
        assertEquals(48.2d, restored.stops.get(0).lat, 0.0);
        assertEquals(16.3d, restored.stops.get(0).lon, 0.0);
    }
}
