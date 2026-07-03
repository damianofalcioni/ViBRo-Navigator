package vibro.navigator.poi.ui;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;

@RunWith(RobolectricTestRunner.class)
public class PoiReverseGeocodeControllerTest {
    private Context context;
    private ImmediateScheduler mainThreadScheduler;
    private RecordingDispatcher dispatcher;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibenavigator_poi_history", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        mainThreadScheduler = new ImmediateScheduler();
        dispatcher = new RecordingDispatcher();
    }

    @Test
    public void setPoiAndResolveAddress_updatesCoordinateLabelWithAddress() {
        PoiInputController inputController = createInputController();
        PoiReverseGeocodeController reverseController = new PoiReverseGeocodeController(
                (lat, lon) -> "Stephansplatz, Vienna",
                mainThreadScheduler,
                dispatcher
        );

        reverseController.setPoiAndResolveAddress(
                inputController,
                new Poi("48.208200, 16.373800", 48.2082d, 16.3738d)
        );
        dispatcher.runNext();

        assertEquals("Stephansplatz, Vienna", inputController.getRawText());
        assertEquals(48.2082d, inputController.getSelectedPoi().lat, 0.0d);
        assertEquals(16.3738d, inputController.getSelectedPoi().lon, 0.0d);
    }

    @Test
    public void setPoiAndResolveAddress_keepsNamedMapPoiLabel() {
        PoiInputController inputController = createInputController();
        PoiReverseGeocodeController reverseController = new PoiReverseGeocodeController(
                (lat, lon) -> "Postal address",
                mainThreadScheduler,
                dispatcher
        );

        reverseController.setPoiAndResolveAddress(
                inputController,
                new Poi("Cafe Central", 48.2100d, 16.3650d)
        );

        assertEquals(0, dispatcher.size());
        assertEquals("Cafe Central", inputController.getRawText());
    }

    @Test
    public void setPoiAndResolveAddress_ignoresStaleReverseResultAfterNewSelection() {
        PoiInputController inputController = createInputController();
        PoiReverseGeocodeController reverseController = new PoiReverseGeocodeController(
                (lat, lon) -> "Old address",
                mainThreadScheduler,
                dispatcher
        );

        reverseController.setPoiAndResolveAddress(
                inputController,
                new Poi("48.208200, 16.373800", 48.2082d, 16.3738d)
        );
        inputController.setPoi(new Poi("45.464200, 9.190000", 45.4642d, 9.19d));
        dispatcher.runNext();

        assertEquals("45.464200, 9.190000", inputController.getRawText());
        assertEquals(45.4642d, inputController.getSelectedPoi().lat, 0.0d);
        assertEquals(9.19d, inputController.getSelectedPoi().lon, 0.0d);
    }

    @Test
    public void setPoiAndResolveAddress_ignoresReverseResultAfterSameCoordinateGetsNamed() {
        PoiInputController inputController = createInputController();
        PoiReverseGeocodeController reverseController = new PoiReverseGeocodeController(
                (lat, lon) -> "Postal address",
                mainThreadScheduler,
                dispatcher
        );

        reverseController.setPoiAndResolveAddress(
                inputController,
                new Poi("48.208200, 16.373800", 48.2082d, 16.3738d)
        );
        inputController.setPoi(new Poi("Named marker", 48.2082d, 16.3738d));
        dispatcher.runNext();

        assertEquals("Named marker", inputController.getRawText());
        assertEquals(48.2082d, inputController.getSelectedPoi().lat, 0.0d);
        assertEquals(16.3738d, inputController.getSelectedPoi().lon, 0.0d);
    }

    @NonNull
    private PoiInputController createInputController() {
        return new PoiInputController(
                context,
                new EditText(context),
                new PoiHistoryStore(context),
                (query, limit) -> Collections.emptyList(),
                poi -> {
                }
        );
    }

    private static final class ImmediateScheduler implements TaskScheduler {
        @Override
        public void post(@NonNull Runnable runnable) {
            runnable.run();
        }
    }

    private static final class RecordingDispatcher implements PoiReverseGeocodeController.ReverseGeocodeDispatcher {
        @NonNull
        private final Queue<Runnable> runnables = new ArrayDeque<>();

        @Override
        public void submit(@NonNull Runnable runnable) {
            runnables.add(runnable);
        }

        private int size() {
            return runnables.size();
        }

        private void runNext() {
            runnables.remove().run();
        }
    }
}
