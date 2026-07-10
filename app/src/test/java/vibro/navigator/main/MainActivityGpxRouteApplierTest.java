package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.os.Looper;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import vibro.navigator.intent.GpxWaypointParser;
import vibro.navigator.intent.GpxWaypointRoute;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.poi.ui.PoiInputController;

@RunWith(RobolectricTestRunner.class)
public class MainActivityGpxRouteApplierTest {
    private static final String DESTINATION_ADDRESS = "Destination address";
    private static final String STOP_ADDRESS = "Stop address";

    private Activity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences("vibenavigator_poi_history", Activity.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void apply_usesPoiSelectionApplierForUnnamedDestinationAndStops()
            throws IOException {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiInputController destinationController = createPoiController(new EditText(activity), searchCalls);
        MainActivityStopController stopController = createStopController(searchCalls);
        GpxWaypointRoute route = parse("""
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <wpt lat="48.1000" lon="16.1000" />
                  <wpt lat="48.2000" lon="16.2000" />
                </gpx>
                """);
        AtomicInteger routeModeSelections = new AtomicInteger();
        List<String> importedNames = new ArrayList<>();

        MainActivityGpxRouteApplier.apply(
                route,
                destinationController,
                stopController,
                routeModeSelections::incrementAndGet,
                (inputController, poi) -> {
                    importedNames.add(poi.name);
                    inputController.setPoi(addressedPoi(poi));
                }
        );
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(1, routeModeSelections.get());
        assertEquals(0, searchCalls.get());
        assertEquals(2, importedNames.size());
        assertEquals("", importedNames.get(0));
        assertEquals("", importedNames.get(1));
        assertEquals(DESTINATION_ADDRESS, destinationController.getRawText());
        assertEquals(STOP_ADDRESS, stopController.getStopControllers().get(0).getRawText());
    }

    @NonNull
    private MainActivityStopController createStopController(@NonNull AtomicInteger searchCalls) {
        return new MainActivityStopController(
                activity,
                new LinearLayout(activity),
                new PoiHistoryStore(activity),
                searchClient(searchCalls),
                stopInputController -> {
                }
        );
    }

    @NonNull
    private PoiInputController createPoiController(
            @NonNull EditText editText,
            @NonNull AtomicInteger searchCalls
    ) {
        return new PoiInputController(
                activity,
                editText,
                new PoiHistoryStore(activity),
                searchClient(searchCalls),
                poi -> {
                }
        );
    }

    @NonNull
    private static PoiSearchClient searchClient(@NonNull AtomicInteger searchCalls) {
        return (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
    }

    @NonNull
    private static Poi addressedPoi(@NonNull Poi poi) {
        String name = poi.lat < 48.15d ? STOP_ADDRESS : DESTINATION_ADDRESS;
        return new Poi(name, poi.lat, poi.lon);
    }

    @NonNull
    private static GpxWaypointRoute parse(@NonNull String gpx) throws IOException {
        return new GpxWaypointParser().parse(new ByteArrayInputStream(
                gpx.getBytes(StandardCharsets.UTF_8)
        ));
    }
}
