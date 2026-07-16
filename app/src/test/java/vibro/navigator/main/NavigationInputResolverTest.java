package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.widget.EditText;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.poi.ui.PoiInputController;

@RunWith(RobolectricTestRunner.class)
public class NavigationInputResolverTest {

    private static final Poi DESTINATION = new Poi("Destination", 48.3000d, 16.3000d);
    private static final Poi TOP_STOP = new Poi("Top stop", 48.2000d, 16.2000d);
    private static final Poi BOTTOM_STOP = new Poi("Bottom stop", 48.1000d, 16.1000d);

    private Activity activity;
    private PoiHistoryStore historyStore;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        historyStore = new PoiHistoryStore(activity);
    }

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
        PoiInputController destinationController = createController();
        PoiInputController topStopController = createController();
        PoiInputController bottomStopController = createController();
        try {
            destinationController.setPoi(DESTINATION);
            topStopController.setPoi(TOP_STOP);
            bottomStopController.setPoi(BOTTOM_STOP);
            List<PoiInputController> stops = Arrays.asList(topStopController, bottomStopController);
            NavigationInputResolver.Result result = NavigationInputResolver.resolve(
                    activity,
                    destinationController,
                    stops,
                    profileSelection
            );
            assertNotNull(result);
            return result;
        } finally {
            destinationController.dispose();
            topStopController.dispose();
            bottomStopController.dispose();
        }
    }

    private PoiInputController createController() {
        PoiSearchClient emptySearchClient = (query, limit) -> Collections.emptyList();
        return new PoiInputController(
                activity,
                new EditText(activity),
                historyStore,
                emptySearchClient,
                poi -> {
                }
        );
    }
}
