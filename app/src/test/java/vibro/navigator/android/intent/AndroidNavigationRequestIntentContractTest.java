package vibro.navigator.android.intent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavigationRequest;

public class AndroidNavigationRequestIntentContractTest {

    @Test
    public void toExtras_thenFromExtras_roundTripsRequestFields() {
        NavigationRequest original = new NavigationRequest(
                "trekking",
                "Vienna Center",
                new LatLon(48.2082d, 16.3738d),
                Arrays.asList(
                        new LatLon(48.2100d, 16.3600d),
                        new LatLon(48.2200d, 16.3900d)
                )
        );

        AndroidNavigationRequestIntentContract.Extras extras =
                AndroidNavigationRequestIntentContract.toExtras(original);
        NavigationRequest restored = AndroidNavigationRequestIntentContract.fromExtras(extras);

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
    public void fromExtras_discardsInvalidStopsAndRequiresProfile() {
        AndroidNavigationRequestIntentContract.Extras extras = new AndroidNavigationRequestIntentContract.Extras(
                null,
                "Test destination",
                48.2082d,
                16.3738d,
                new ArrayList<>(Arrays.asList("48.2,16.3", "bad", "12"))
        );

        NavigationRequest restored = AndroidNavigationRequestIntentContract.fromExtras(extras);

        assertFalse(restored.isComplete());
        assertEquals("Test destination", restored.destinationName);
        assertEquals(1, restored.stops.size());
        assertEquals(48.2d, restored.stops.get(0).lat, 0.0);
        assertEquals(16.3d, restored.stops.get(0).lon, 0.0);
    }
}
