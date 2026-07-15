package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.poi.ui.PoiInputController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class MainActivityStopControllerTest {

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
    public void restoreValues_preservesSelectedStopWithoutTriggeringSuggestions() {
        PoiSearchClient originalSearchClient = (query, limit) -> Collections.emptyList();
        MainActivityStopController original = new MainActivityStopController(
                activity,
                new LinearLayout(activity),
                new PoiHistoryStore(activity),
                originalSearchClient,
                stopInputController -> {
                }
        );
        original.addStopRow(null);
        Poi selected = new Poi("Stop A", 48.2082d, 16.3738d);
        PoiInputController originalController = original.getStopControllers().get(0);
        originalController.setPoi(selected);

        Bundle state = new Bundle();
        original.saveState(state);

        AtomicInteger restoredSearchCalls = new AtomicInteger();
        PoiSearchClient restoredSearchClient = (query, limit) -> {
            restoredSearchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        MainActivityStopController restored = new MainActivityStopController(
                activity,
                new LinearLayout(activity),
                new PoiHistoryStore(activity),
                restoredSearchClient,
                stopInputController -> {
                }
        );
        restored.restoreRows(state);
        restored.restoreValues(state);
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(1, restored.getStopControllers().size());
        PoiInputController restoredController = restored.getStopControllers().get(0);
        assertEquals(0, restoredSearchCalls.get());
        assertEquals("Stop A", restoredController.getRawText());
        assertNotNull(restoredController.getSelectedPoi());
        assertEquals(selected.name, restoredController.getSelectedPoi().name);
        assertEquals(selected.lat, restoredController.getSelectedPoi().lat, 0.0);
        assertEquals(selected.lon, restoredController.getSelectedPoi().lon, 0.0);
    }

    @Test
    public void restoreValues_preservesManualStopTextWithoutTriggeringSuggestions() {
        PoiSearchClient originalSearchClient = (query, limit) -> Collections.emptyList();
        MainActivityStopController original = new MainActivityStopController(
                activity,
                new LinearLayout(activity),
                new PoiHistoryStore(activity),
                originalSearchClient,
                stopInputController -> {
                }
        );
        original.addStopRow(null);
        PoiInputController originalController = original.getStopControllers().get(0);
        originalController.setText("Cafe Central");

        Bundle state = new Bundle();
        original.saveState(state);

        AtomicInteger restoredSearchCalls = new AtomicInteger();
        PoiSearchClient restoredSearchClient = (query, limit) -> {
            restoredSearchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        MainActivityStopController restored = new MainActivityStopController(
                activity,
                new LinearLayout(activity),
                new PoiHistoryStore(activity),
                restoredSearchClient,
                stopInputController -> {
                }
        );
        restored.restoreRows(state);
        restored.restoreValues(state);
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(1, restored.getStopControllers().size());
        PoiInputController restoredController = restored.getStopControllers().get(0);
        assertEquals(0, restoredSearchCalls.get());
        assertEquals("Cafe Central", restoredController.getRawText());
        assertNull(restoredController.getSelectedPoi());
    }

    @Test
    public void addStopRow_displaysNewestStopNearAddButton() {
        LinearLayout stopsContainer = new LinearLayout(activity);
        PoiSearchClient searchClient = (query, limit) -> Collections.emptyList();
        MainActivityStopController controller = new MainActivityStopController(
                activity,
                stopsContainer,
                new PoiHistoryStore(activity),
                searchClient,
                stopInputController -> {
                }
        );

        controller.addStopRow("First stop");
        controller.addStopRow("Second stop");

        assertEquals("First stop", controller.getStopControllers().get(0).getRawText());
        assertEquals("Second stop", controller.getStopControllers().get(1).getRawText());
        assertEquals("First stop", stopTextAt(stopsContainer.getChildAt(0)));
        assertEquals("Second stop", stopTextAt(stopsContainer.getChildAt(1)));
    }

    @Test
    public void addStopRow_allowsBindingActionsInsideCreatedRow() {
        LinearLayout stopsContainer = new LinearLayout(activity);
        AtomicReference<View> boundRow = new AtomicReference<>();
        AtomicReference<PoiInputController> boundController = new AtomicReference<>();
        AtomicInteger voiceClicks = new AtomicInteger();
        MainActivityStopController controller = new MainActivityStopController(
                activity,
                stopsContainer,
                new PoiHistoryStore(activity),
                (query, limit) -> Collections.emptyList(),
                new MainActivityStopController.MapPickListener() {
                    @Override
                    public void onPickStopFromMap(@NonNull PoiInputController stopInputController) {
                    }

                    @Override
                    public void onStopRowCreated(
                            @NonNull View row,
                            @NonNull PoiInputController stopInputController
                    ) {
                        boundRow.set(row);
                        boundController.set(stopInputController);
                        row.findViewById(R.id.stopVoiceButton)
                                .setOnClickListener(v -> voiceClicks.incrementAndGet());
                    }
                }
        );

        controller.addStopRow(null);
        stopsContainer.getChildAt(0).findViewById(R.id.stopVoiceButton).performClick();

        assertEquals(stopsContainer.getChildAt(0), boundRow.get());
        assertEquals(controller.getStopControllers().get(0), boundController.get());
        assertEquals(1, voiceClicks.get());
    }

    @Test
    public void replaceStops_clearsExistingRowsAndRestoresSelectedStopsWithoutSearch() {
        LinearLayout stopsContainer = new LinearLayout(activity);
        AtomicInteger searchCalls = new AtomicInteger();
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        MainActivityStopController controller = new MainActivityStopController(
                activity,
                stopsContainer,
                new PoiHistoryStore(activity),
                searchClient,
                stopInputController -> {
                }
        );
        controller.addStopRow("Old stop");
        Poi stopA = new Poi("Stop A", 48.2082d, 16.3738d);
        Poi stopB = new Poi("Stop B", 45.4642d, 9.19d);

        controller.replaceStops(Arrays.asList(stopA, stopB));
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals(2, stopsContainer.getChildCount());
        assertEquals(2, controller.getStopControllers().size());
        assertEquals(stopA.name, controller.getStopControllers().get(0).getSelectedPoi().name);
        assertEquals(stopB.name, controller.getStopControllers().get(1).getSelectedPoi().name);
    }

    @NonNull
    private static String stopTextAt(@NonNull View row) {
        EditText stopEdit = row.findViewById(R.id.stopEdit);
        return stopEdit.getText().toString();
    }
}

