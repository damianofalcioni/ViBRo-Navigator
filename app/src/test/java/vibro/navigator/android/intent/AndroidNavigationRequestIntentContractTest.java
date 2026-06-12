package vibro.navigator.android.intent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;

public class AndroidNavigationRequestIntentContractTest {
    private static final String PROFILE = "trekking";
    private static final String DESTINATION_NAME = "Vienna Center";

    @Test
    public void toExtras_thenFromExtras_roundTripsRequestFields() {
        NavigationRequest original = new NavigationRequest(
                PROFILE,
                DESTINATION_NAME,
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
        assertEquals(NavigationRoutingMode.BROUTER, restored.routingMode);
        assertEquals(PROFILE, restored.profile);
        assertEquals(DESTINATION_NAME, restored.destinationName);
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
                new ArrayList<>(Arrays.asList("48.2,16.3", "bad", "12", "91,16.3", "48.3,16.4,extra"))
        );

        NavigationRequest restored = AndroidNavigationRequestIntentContract.fromExtras(extras);

        assertFalse(restored.isComplete());
        assertEquals("Test destination", restored.destinationName);
        assertEquals(1, restored.stops.size());
        assertEquals(48.2d, restored.stops.get(0).lat, 0.0);
        assertEquals(16.3d, restored.stops.get(0).lon, 0.0);
    }

    @Test
    public void fromExtras_discardsInvalidDestinationCoordinates() {
        AndroidNavigationRequestIntentContract.Extras extras = new AndroidNavigationRequestIntentContract.Extras(
                PROFILE,
                "Test destination",
                91.0d,
                16.3738d,
                new ArrayList<>()
        );

        NavigationRequest restored = AndroidNavigationRequestIntentContract.fromExtras(extras);

        assertFalse(restored.isComplete());
        assertNull(restored.destination);
    }

    @Test
    public void toExtras_thenFromExtras_preservesStraightLineModeWithoutProfile() {
        NavigationRequest original = new NavigationRequest(
                NavigationRoutingMode.STRAIGHT_LINE,
                null,
                DESTINATION_NAME,
                new LatLon(48.2082d, 16.3738d),
                Arrays.asList(new LatLon(48.2100d, 16.3600d))
        );

        AndroidNavigationRequestIntentContract.Extras extras =
                AndroidNavigationRequestIntentContract.toExtras(original);
        NavigationRequest restored = AndroidNavigationRequestIntentContract.fromExtras(extras);

        assertTrue(restored.isComplete());
        assertEquals(NavigationRoutingMode.STRAIGHT_LINE, restored.routingMode);
        assertNull(restored.profile);
        assertEquals("straight_line", extras.routingMode);
        assertEquals(1, restored.stops.size());
    }

    @Test
    public void toExtras_discardsInvalidStops() {
        NavigationRequest original = new NavigationRequest(
                PROFILE,
                DESTINATION_NAME,
                new LatLon(48.2082d, 16.3738d),
                Arrays.asList(
                        new LatLon(48.2100d, 16.3600d),
                        new LatLon(91.0d, 16.3900d)
                )
        );

        AndroidNavigationRequestIntentContract.Extras extras =
                AndroidNavigationRequestIntentContract.toExtras(original);

        assertEquals(1, extras.stops.size());
        assertEquals("48.21,16.36", extras.stops.get(0));
    }
}
