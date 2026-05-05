package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.poi.ui.PoiInputController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
                (stopIndex, initialPoi) -> {
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
                (stopIndex, initialPoi) -> {
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
                (stopIndex, initialPoi) -> {
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
                (stopIndex, initialPoi) -> {
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
}

