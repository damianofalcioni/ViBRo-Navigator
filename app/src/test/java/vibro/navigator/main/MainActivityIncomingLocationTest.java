package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.Collections;

import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.poi.ui.PoiReverseGeocodeController;

@RunWith(RobolectricTestRunner.class)
public class MainActivityIncomingLocationTest {
    private static final String COORDINATES = "48.2082,16.3738";
    private static final String DESTINATION = "My destination";
    private Activity activity;
    private ActivityController<Activity> activityController;
    private PoiHistoryStore history;
    private PoiInputController destination;
    private MainActivityStopController stops;

    @Before
    public void setUp() {
        activityController = Robolectric.buildActivity(Activity.class).setup();
        activity = activityController.get();
        activity.getSharedPreferences("vibenavigator_poi_history", Activity.MODE_PRIVATE)
                .edit().clear().commit();
        history = new PoiHistoryStore(activity);
        destination = new PoiInputController(activity, new EditText(activity), history,
                (query, limit) -> Collections.emptyList(), poi -> { });
        stops = new MainActivityStopController(activity, new LinearLayout(activity), history,
                (query, limit) -> Collections.emptyList(), controller -> { });
    }

    @After
    public void tearDown() {
        destination.dispose();
        stops.dispose();
    }

    @Test
    public void emptyDestinationReceivesCoordinatesBeforeEmptyStops() {
        destination.restoreText("  ");
        stops.addStopRow(null);

        shareCoordinates();

        assertCoordinates(destination);
        assertEquals(1, stops.size());
        assertEquals("", stop(0).getRawText());
    }

    @Test
    public void selectedDestinationIsPreservedAndFirstStopIsAdded() {
        Poi selected = new Poi(DESTINATION, 45, 12);
        destination.restorePoi(selected);

        shareCoordinates();

        assertSame(selected, destination.getSelectedPoi());
        assertEquals(DESTINATION, destination.getRawText());
        assertEquals(1, stops.size());
        assertCoordinates(stop(0));
        assertEquals(48.2082, history.list().get(0).lat, 0);
    }

    @Test
    public void fillsStopTwoWhenStopsOneAndFourAreFilled() {
        destination.restoreText(DESTINATION);
        stops.addStopRow("First stop");
        stops.addStopRow(null);
        stops.addStopRow(null);
        stops.addStopRow("Fourth stop");

        shareCoordinates();

        assertEquals(DESTINATION, destination.getRawText());
        assertEquals(4, stops.size());
        assertEquals("First stop", stop(0).getRawText());
        assertCoordinates(stop(1));
        assertEquals("", stop(2).getRawText());
        assertEquals("Fourth stop", stop(3).getRawText());
    }

    @Test
    public void whitespaceStopReceivesAddressQuery() {
        destination.restoreText(DESTINATION);
        stops.addStopRow(" \t ");
        stops.addStopRow("Later stop");

        receive(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Cafe%20Central")));

        assertEquals(DESTINATION, destination.getRawText());
        assertEquals(2, stops.size());
        assertEquals("Cafe Central", stop(0).getRawText());
        assertNull(stop(0).getSelectedPoi());
    }

    @Test
    public void successiveSharesAppendAfterAllFilledStops() {
        destination.restoreText(DESTINATION);
        stops.addStopRow("Existing stop");

        shareCoordinates();
        receive(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:47,15")));

        assertEquals(3, stops.size());
        assertEquals("Existing stop", stop(0).getRawText());
        assertCoordinates(stop(1));
        assertEquals(47, stop(2).getSelectedPoi().lat, 0);
    }

    @Test
    public void invalidShareDoesNotCreateStop() {
        destination.restoreText(DESTINATION);

        receive(new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")));

        assertEquals(DESTINATION, destination.getRawText());
        assertEquals(0, stops.size());
    }

    @Test
    public void resolvedShortLinkUsesFormAtCompletion() {
        MainActivityIntentHandler.LocationTarget target = () ->
                MainActivityStopRowOperations.incomingLocationTarget(destination, stops);
        destination.restoreText(DESTINATION);
        stops.addStopRow("Filled while waiting");
        stops.addStopRow(null);

        MainActivityIntentHandler.applyResolvedShortMapUrl(activity,
                new Intent(Intent.ACTION_SEND), COORDINATES, target, PoiReverseGeocodeController.disabled());

        assertEquals(DESTINATION, destination.getRawText());
        assertEquals(2, stops.size());
        assertCoordinates(stop(1));
    }

    @Test
    public void failedShortLinkDoesNotCreateStop() {
        destination.restoreText(DESTINATION);

        MainActivityIntentHandler.applyResolvedShortMapUrl(activity,
                new Intent(Intent.ACTION_SEND), null,
                () -> MainActivityStopRowOperations.incomingLocationTarget(destination, stops),
                PoiReverseGeocodeController.disabled());

        assertEquals(0, stops.size());
    }

    @Test
    public void shortLinkResultDoesNotModifyDestroyedActivity() {
        destination.restoreText(DESTINATION);
        activityController.pause().stop().destroy();

        MainActivityIntentHandler.applyResolvedShortMapUrl(activity,
                new Intent(Intent.ACTION_SEND), COORDINATES,
                () -> MainActivityStopRowOperations.incomingLocationTarget(destination, stops),
                PoiReverseGeocodeController.disabled());

        assertEquals(0, stops.size());
    }

    @Test
    public void alreadyAppliedIntentIsNotInsertedAgain() {
        destination.restoreText(DESTINATION);
        Intent intent = new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, COORDINATES);

        receive(intent);
        receive(intent);

        assertEquals(1, stops.size());
        assertTrue(intent.getBooleanExtra(MainActivityIncomingLocationState.APPLIED, false));
    }

    private void shareCoordinates() {
        receive(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, COORDINATES));
    }

    private void receive(Intent intent) {
        MainActivityIntentHandler.handleIncomingIntent(activity, intent, destination, stops,
                null, PoiReverseGeocodeController.disabled());
    }

    private PoiInputController stop(int index) {
        return stops.getStopControllers().get(index);
    }

    private static void assertCoordinates(PoiInputController controller) {
        assertEquals(48.2082, controller.getSelectedPoi().lat, 0);
        assertEquals(16.3738, controller.getSelectedPoi().lon, 0);
    }
}
