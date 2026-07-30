package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import vibro.navigator.poi.Poi;

public class NavigationInputResolverTest {

    private static final Poi DESTINATION = new Poi("Destination", 48.3000d, 16.3000d);
    private static final Poi TOP_STOP = new Poi("Top stop", 48.2000d, 16.2000d);
    private static final Poi BOTTOM_STOP = new Poi("Bottom stop", 48.1000d, 16.1000d);

    @Test
    public void resolve_reversesBrouterStopsFromCurrentPositionSide() {
        NavigationInputResolver.Result result = resolve(ProfileSelection.brouter("car-eco"));

        assertEquals(BOTTOM_STOP.lat, result.request.stops.get(0).lat, 0.0);
        assertEquals(TOP_STOP.lat, result.request.stops.get(1).lat, 0.0);
    }

    @Test
    public void resolve_reversesStraightLineStopsFromCurrentPositionSide() {
        NavigationInputResolver.Result result = resolve(ProfileSelection.straightLine());

        assertEquals(BOTTOM_STOP.lat, result.request.stops.get(0).lat, 0.0);
        assertEquals(TOP_STOP.lat, result.request.stops.get(1).lat, 0.0);
    }

    @Test
    public void resolve_preservesCustomProfileSource() {
        NavigationInputResolver.Result result = resolve(ProfileSelection.customBrouter("custom-car", null));

        assertEquals("custom-car", result.request.profile);
        assertTrue(result.request.customProfile);
    }

    private NavigationInputResolver.Result resolve(ProfileSelection profileSelection) {
        List<Poi> stops = Arrays.asList(TOP_STOP, BOTTOM_STOP);
        return NavigationInputResolver.fromResolvedPois(DESTINATION, stops, profileSelection);
    }
}
