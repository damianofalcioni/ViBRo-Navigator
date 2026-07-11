package vibro.navigator.poi.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class PoiInputControllerTest {
    private static final String COFFEE_SPOT = "Coffee Spot";
    private static final String SAVED_DESTINATION = "Saved destination";

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibenavigator_poi_history", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void setPoi_doesNotTriggerSearchSuggestions() {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        Poi selected = new Poi(SAVED_DESTINATION, 48.2082d, 16.3738d);
        Poi[] listenerSelection = new Poi[1];
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                new PoiHistoryStore(context),
                searchClient,
                poi -> listenerSelection[0] = poi
        );

        controller.setPoi(selected);
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals(SAVED_DESTINATION, controller.getRawText());
        assertSame(selected, controller.getSelectedPoi());
        assertSame(selected, listenerSelection[0]);
    }

    @Test
    public void setPoi_clearsInputFocusAfterSelection() {
        Poi selected = new Poi(SAVED_DESTINATION, 48.2082d, 16.3738d);
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                new PoiHistoryStore(context),
                (query, limit) -> Collections.emptyList(),
                poi -> {
                }
        );
        controller.getEditText().requestFocus();

        controller.setPoi(selected);

        assertFalse(controller.getEditText().hasFocus());
    }

    @Test
    public void clickingSelectedSavedPoi_showsOnlyThatHistoryEntry() {
        PoiHistoryStore historyStore = new PoiHistoryStore(context);
        historyStore.addOrPromote(new Poi("Other destination", 45.4642d, 9.19d));
        Poi selected = new Poi(SAVED_DESTINATION, 48.2082d, 16.3738d);
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                historyStore,
                (query, limit) -> Collections.emptyList(),
                poi -> {
                }
        );

        controller.setPoi(selected);
        controller.getEditText().requestFocus();
        controller.getEditText().performClick();

        assertEquals(1, controller.getSuggestionCountForTesting());
        assertEquals(SAVED_DESTINATION, controller.getSuggestionLabelForTesting(0));
    }

    @Test
    public void deletingSelectedSavedPoi_clearsTextAndSelection() {
        PoiHistoryStore historyStore = new PoiHistoryStore(context);
        Poi selected = new Poi(SAVED_DESTINATION, 48.2082d, 16.3738d);
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                historyStore,
                (query, limit) -> Collections.emptyList(),
                poi -> {
                }
        );

        controller.setPoi(selected);
        controller.getEditText().requestFocus();
        controller.getEditText().performClick();
        controller.deleteSuggestionForTesting(0);

        assertEquals("", controller.getRawText());
        assertEquals(null, controller.getSelectedPoi());
        assertTrue(historyStore.list().isEmpty());
        assertEquals(0, controller.getSuggestionCountForTesting());
    }

    @Test
    public void deletingTypedHistoryMatch_keepsTypedText() {
        PoiHistoryStore historyStore = new PoiHistoryStore(context);
        historyStore.addOrPromote(new Poi(COFFEE_SPOT, 48.2082d, 16.3738d));
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                historyStore,
                (query, limit) -> Collections.emptyList(),
                poi -> {
                }
        );

        controller.getEditText().setText("coffee");
        controller.deleteSuggestionForTesting(0);

        assertEquals("coffee", controller.getRawText());
        assertEquals(null, controller.getSelectedPoi());
        assertTrue(historyStore.list().isEmpty());
    }

    @Test
    public void setPoi_parksFocusOutsideOtherPoiInputs() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        LinearLayout root = new LinearLayout(activity);
        root.setFocusableInTouchMode(true);
        EditText destinationEdit = new EditText(activity);
        EditText stopEdit = new EditText(activity);
        root.addView(destinationEdit);
        root.addView(stopEdit);
        activity.setContentView(root);
        PoiInputController destinationController = new PoiInputController(
                activity,
                destinationEdit,
                new PoiHistoryStore(activity),
                (query, limit) -> Collections.emptyList(),
                poi -> {
                }
        );
        PoiInputController stopController = new PoiInputController(
                activity,
                stopEdit,
                new PoiHistoryStore(activity),
                (query, limit) -> Collections.emptyList(),
                poi -> {
                }
        );
        stopEdit.requestFocus();

        stopController.setPoi(new Poi("Stop A", 48.2082d, 16.3738d));

        assertFalse(destinationEdit.hasFocus());
        assertFalse(stopEdit.hasFocus());
        assertTrue(stopEdit.getRootView().hasFocus());
        destinationController.dispose();
        stopController.dispose();
    }

    @Test
    public void setPoi_recoversWhenAnotherPoiInputFocusesAfterSelection() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        LinearLayout root = new LinearLayout(activity);
        EditText destinationEdit = new EditText(activity);
        EditText stopEdit = new EditText(activity);
        root.addView(destinationEdit);
        root.addView(stopEdit);
        activity.setContentView(root);
        PoiInputController destinationController = new PoiInputController(
                activity,
                destinationEdit,
                new PoiHistoryStore(activity),
                (query, limit) -> Collections.emptyList(),
                poi -> {
                }
        );
        PoiInputController stopController = new PoiInputController(
                activity,
                stopEdit,
                new PoiHistoryStore(activity),
                (query, limit) -> Collections.emptyList(),
                poi -> {
                }
        );
        stopEdit.requestFocus();

        stopController.setPoi(new Poi("Stop A", 48.2082d, 16.3738d));
        destinationEdit.requestFocus();
        shadowOf(Looper.getMainLooper()).idleFor(150, TimeUnit.MILLISECONDS);

        assertFalse(destinationEdit.hasFocus());
        assertFalse(stopEdit.hasFocus());
        destinationController.dispose();
        stopController.dispose();
    }

    @Test
    public void restorePoi_doesNotTriggerSearchSuggestionsAndRetainsSelection() {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        Poi selected = new Poi("Stored destination", 48.2082d, 16.3738d);
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                new PoiHistoryStore(context),
                searchClient,
                poi -> {
                }
        );

        controller.restorePoi(selected);
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals("Stored destination", controller.getRawText());
        assertSame(selected, controller.getSelectedPoi());
    }

    @Test
    public void replaceSelectedPoiNameIfSameCoordinates_keepsCoordinatesAndUpdatesHistory() {
        PoiHistoryStore historyStore = new PoiHistoryStore(context);
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                historyStore,
                (query, limit) -> Collections.emptyList(),
                poi -> {
                }
        );
        Poi selected = new Poi("48.208200, 16.373800", 48.2082d, 16.3738d);

        controller.setPoi(selected);
        boolean applied = controller.replaceSelectedPoiNameIfSameCoordinates(selected, "Stephansplatz, Vienna");

        assertTrue(applied);
        assertEquals("Stephansplatz, Vienna", controller.getRawText());
        assertEquals(48.2082d, controller.getSelectedPoi().lat, 0.0d);
        assertEquals(16.3738d, controller.getSelectedPoi().lon, 0.0d);
        assertEquals("Stephansplatz, Vienna", historyStore.list().get(0).name);
    }

    @Test
    public void replaceSelectedPoiNameIfSameCoordinates_rejectsStaleCoordinates() {
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                new PoiHistoryStore(context),
                (query, limit) -> Collections.emptyList(),
                poi -> {
                }
        );
        Poi original = new Poi("48.208200, 16.373800", 48.2082d, 16.3738d);
        Poi newer = new Poi("45.464200, 9.190000", 45.4642d, 9.19d);

        controller.setPoi(newer);
        boolean applied = controller.replaceSelectedPoiNameIfSameCoordinates(original, "Stale address");

        assertFalse(applied);
        assertEquals("45.464200, 9.190000", controller.getRawText());
        assertEquals(45.4642d, controller.getSelectedPoi().lat, 0.0d);
        assertEquals(9.19d, controller.getSelectedPoi().lon, 0.0d);
    }

    @Test
    public void restoreText_doesNotTriggerSearchSuggestions() {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                new PoiHistoryStore(context),
                searchClient,
                poi -> {
                }
        );

        controller.restoreText("Cafe Central");
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals("Cafe Central", controller.getRawText());
        assertEquals(null, controller.getSelectedPoi());
    }

    @Test
    public void typedQuery_keepsMatchingHistoryWhileOnlineSearchRuns() throws Exception {
        CountDownLatch searchLatch = new CountDownLatch(1);
        AtomicInteger searchCalls = new AtomicInteger();
        PoiHistoryStore historyStore = new PoiHistoryStore(context);
        historyStore.addOrPromote(new Poi(COFFEE_SPOT, 48.2082d, 16.3738d));
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            searchLatch.countDown();
            return Collections.singletonList(new Poi("Coffee Online", 48.2000d, 16.3600d));
        };
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                historyStore,
                searchClient,
                poi -> {
                }
        );

        controller.getEditText().setText("coffee");
        assertEquals(1, controller.getSuggestionCountForTesting());
        assertEquals(COFFEE_SPOT, controller.getSuggestionLabelForTesting(0));

        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertTrue(searchLatch.await(1, TimeUnit.SECONDS));
        assertEquals(1, searchCalls.get());
    }

    @Test
    public void typedSingleCharacterQuery_canReturnMatchingHistoryWithoutOnlineSearch() {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiHistoryStore historyStore = new PoiHistoryStore(context);
        historyStore.addOrPromote(new Poi(COFFEE_SPOT, 48.2082d, 16.3738d));
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                historyStore,
                searchClient,
                poi -> {
                }
        );

        controller.getEditText().setText("c");
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals(1, controller.getSuggestionCountForTesting());
        assertEquals(COFFEE_SPOT, controller.getSuggestionLabelForTesting(0));
    }

    @Test
    public void typedSingleCharacterQueryWithoutHistoryMatch_doesNotSearchOnline() {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiHistoryStore historyStore = new PoiHistoryStore(context);
        historyStore.addOrPromote(new Poi(COFFEE_SPOT, 48.2082d, 16.3738d));
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                historyStore,
                searchClient,
                poi -> {
                }
        );

        controller.getEditText().setText("m");
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals(0, controller.getSuggestionCountForTesting());
    }

    @Test
    public void typedQuery_fallsBackToOnlineSearchWhenHistoryHasNoMatch() throws Exception {
        CountDownLatch searchLatch = new CountDownLatch(1);
        AtomicInteger searchCalls = new AtomicInteger();
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            searchLatch.countDown();
            return Collections.emptyList();
        };
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                new PoiHistoryStore(context),
                searchClient,
                poi -> {
                }
        );

        controller.getEditText().setText("museum");
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertTrue(searchLatch.await(1, TimeUnit.SECONDS));
        assertEquals(1, searchCalls.get());
    }
}
